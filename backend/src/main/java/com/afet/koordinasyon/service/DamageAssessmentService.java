package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.DamageAssessment;
import com.afet.koordinasyon.domain.entity.DamageAssessmentAssignment;
import com.afet.koordinasyon.domain.entity.DamageAssessmentPhoto;
import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.EventVolunteer;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.AiAnalysisStatus;
import com.afet.koordinasyon.domain.enums.DamageLevel;
import com.afet.koordinasyon.domain.enums.EventStatus;
import com.afet.koordinasyon.domain.enums.PhotoType;
import com.afet.koordinasyon.domain.enums.EventVolunteerStatus;
import com.afet.koordinasyon.domain.enums.LocationSource;
import com.afet.koordinasyon.domain.enums.TeamName;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.domain.enums.VerificationStatus;
import com.afet.koordinasyon.dto.request.AssignDamageAssessmentRequest;
import com.afet.koordinasyon.dto.request.CreateDamageAssessmentRequest;
import com.afet.koordinasyon.dto.request.VerifyDamageAssessmentRequest;
import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.email.DamageReportCreatedEmailEvent;
import com.afet.koordinasyon.service.email.DamageReportStatusChangedEmailEvent;
import com.afet.koordinasyon.service.email.DamageReportFieldTeamAssignedEmailEvent;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DamageAssessmentService {

    private static final Set<String> ALLOWED_PHOTO_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    @Value("${app.storage.local-path:.local-storage}")
    private String localStoragePath;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final DamageAssessmentRepository damageAssessmentRepository;
    private final DamageAssessmentPhotoRepository damageAssessmentPhotoRepository;
    private final DamageAssessmentAssignmentRepository assignmentRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final UserRepository userRepository;
    private final EventVolunteerRepository eventVolunteerRepository;
    private final DamageAssessmentAiService damageAssessmentAiService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PagedResponse<DamageAssessmentResponse> listAssessments(
            int page, int size, UUID districtId, UUID neighborhoodId,
            String damageLevel, String verificationStatus, UserPrincipal principal) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (principal.getRole() == UserRole.DISTRICT_COORDINATOR && districtId == null) {
            // Primary: FK lookup; fallback: user's own district_id
            districtId = districtRepository.findByCoordinatorId(principal.getId())
                    .map(District::getId)
                    .orElse(principal.getDistrictId());
        }
        if (principal.getRole() == UserRole.NEIGHBORHOOD_COORDINATOR && neighborhoodId == null) {
            // Primary: FK lookup; fallback: user's own neighborhood_id
            neighborhoodId = neighborhoodRepository.findByCoordinatorId(principal.getId())
                    .map(Neighborhood::getId)
                    .orElse(principal.getNeighborhoodId());
        }

        Page<DamageAssessment> result;
        if (neighborhoodId != null) {
            result = damageAssessmentRepository.findByNeighborhoodId(neighborhoodId, pageable);
        } else if (districtId != null) {
            result = damageAssessmentRepository.findByDistrictId(districtId, pageable);
        } else {
            result = damageAssessmentRepository.findAll(pageable);
        }

        return PagedResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public DamageAssessmentResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<DamageAssessmentResponse> listMyAssessments(int page, int size, UserPrincipal principal) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DamageAssessment> result = damageAssessmentRepository
                .findByReportedByIdOrderByCreatedAtDesc(principal.getId(), pageable);
        return PagedResponse.from(result.map(this::toResponse));
    }

    @Transactional
    public DamageAssessmentResponse create(CreateDamageAssessmentRequest req,
                                            List<MultipartFile> photos,
                                            UserPrincipal principal) {
        // Offline-queue idempotency: return existing record if clientGeneratedId already processed
        if (req.getClientGeneratedId() != null && !req.getClientGeneratedId().isBlank()) {
            var existing = damageAssessmentRepository.findByClientGeneratedId(req.getClientGeneratedId());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        // Fotoğraf zorunluluğu
        if (photos == null || photos.isEmpty()) {
            throw new BusinessRuleException("En az 1 fotoğraf yüklenmesi zorunludur");
        }

        // Fotoğraf formatı kontrolü
        for (MultipartFile photo : photos) {
            validatePhotoMimeType(photo);
        }

        Neighborhood neighborhood = neighborhoodRepository.findById(req.getNeighborhoodId())
                .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", req.getNeighborhoodId()));
        District district = neighborhood.getDistrict();

        // Yetki kontrolü — FK-based first, user.district_id fallback
        UserRole role = principal.getRole();
        if (role == UserRole.DISTRICT_COORDINATOR) {
            UUID assignedDistrictId = districtRepository.findByCoordinatorId(principal.getId())
                    .map(District::getId)
                    .orElse(principal.getDistrictId());
            if (!district.getId().equals(assignedDistrictId)) {
                throw new BusinessRuleException("Sadece kendi ilçeniz için hasar tespiti girebilirsiniz");
            }
        } else if (role == UserRole.NEIGHBORHOOD_COORDINATOR) {
            UUID assignedNeighborhoodId = neighborhoodRepository.findByCoordinatorId(principal.getId())
                    .map(Neighborhood::getId)
                    .orElse(principal.getNeighborhoodId());
            if (!neighborhood.getId().equals(assignedNeighborhoodId)) {
                throw new BusinessRuleException("Sadece kendi mahalleniz için hasar tespiti girebilirsiniz");
            }
        }

        User reporter = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        DamageAssessment assessment = DamageAssessment.builder()
                .district(district)
                .neighborhood(neighborhood)
                .streetName(req.getStreetName())
                .buildingNo(req.getBuildingNo())
                .address(req.getAddress())
                .latitude(BigDecimal.valueOf(req.getLatitude()))
                .longitude(BigDecimal.valueOf(req.getLongitude()))
                .locationSource(req.getLocationSource() != null ? req.getLocationSource() : LocationSource.MAP_SELECTED)
                .locationVerified(Boolean.TRUE.equals(req.getLocationVerified()))
                .buildingType(req.getBuildingType())
                .floorCount(req.getFloorCount())
                .occupancyType(req.getOccupancyType())
                .damageLevel(req.getDamageLevel() != null ? req.getDamageLevel() : DamageLevel.UNASSESSED)
                .collapseRisk(Boolean.TRUE.equals(req.getCollapseRisk()))
                .emergencyEvacuationNeeded(Boolean.TRUE.equals(req.getEmergencyEvacuationNeeded()))
                .casualtiesSuspected(Boolean.TRUE.equals(req.getCasualtiesSuspected()))
                .blockedRoad(Boolean.TRUE.equals(req.getBlockedRoad()))
                .gasLeakRisk(Boolean.TRUE.equals(req.getGasLeakRisk()))
                .note(req.getNote())
                .clientGeneratedId(req.getClientGeneratedId())
                .reportedBy(reporter)
                .build();

        DamageAssessment saved = damageAssessmentRepository.save(assessment);

        // Fotoğrafları kaydet ve listeye ekle (lazy loading sorununu önlemek için)
        for (MultipartFile photo : photos) {
            DamageAssessmentPhoto photoEntity = savePhoto(photo, saved, PhotoType.REPORTER_PHOTO, reporter);
            saved.getPhotos().add(photoEntity);
        }

        // AI analizi transaction commit sonrası async olarak başlat
        final UUID savedId = saved.getId();
        final UUID reporterIdFinal = reporter.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                damageAssessmentAiService.generateAiAssessment(savedId);
            }
        });

        eventPublisher.publishEvent(new DamageReportCreatedEmailEvent(savedId, reporterIdFinal));

        return toResponse(saved);
    }

    @Transactional
    public DamageAssessmentAiTriggerResponse triggerAiAnalysis(UUID id, UserPrincipal principal) {
        DamageAssessment assessment = findById(id);

        // Prevent duplicate parallel jobs: if a job is already running, return current state.
        if (assessment.getAiAnalysisStatus() == AiAnalysisStatus.PROCESSING) {
            log.info("AI trigger: assessment {} already PROCESSING — returning without starting new job", id);
            return DamageAssessmentAiTriggerResponse.builder()
                    .assessmentId(id)
                    .aiAnalysisStatus(AiAnalysisStatus.PROCESSING.name())
                    .build();
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                damageAssessmentAiService.generateAiAssessment(id);
            }
        });

        return DamageAssessmentAiTriggerResponse.builder()
                .assessmentId(id)
                .aiAnalysisStatus(AiAnalysisStatus.PROCESSING.name())
                .build();
    }

    @Transactional
    public DamageAssessmentResponse verify(UUID id, VerifyDamageAssessmentRequest req, UserPrincipal principal) {
        UserRole role = principal.getRole();
        if (role == UserRole.VOLUNTEER) {
            throw new BusinessRuleException("Gönüllüler doğrulama yapamaz");
        }

        DamageAssessment assessment = findById(id);

        if (role == UserRole.DISTRICT_COORDINATOR) {
            UUID assignedDistrictId = districtRepository.findByCoordinatorId(principal.getId())
                    .map(District::getId)
                    .orElse(principal.getDistrictId());
            if (!assessment.getDistrict().getId().equals(assignedDistrictId)) {
                throw new BusinessRuleException("Sadece kendi ilçenizdeki kayıtları doğrulayabilirsiniz");
            }
        }
        if (role == UserRole.NEIGHBORHOOD_COORDINATOR) {
            UUID assignedNeighborhoodId = neighborhoodRepository.findByCoordinatorId(principal.getId())
                    .map(Neighborhood::getId)
                    .orElse(principal.getNeighborhoodId());
            if (!assessment.getNeighborhood().getId().equals(assignedNeighborhoodId)) {
                throw new BusinessRuleException("Sadece kendi mahallenizin kayıtlarını doğrulayabilirsiniz");
            }
        }

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        assessment.setVerificationStatus(req.getVerificationStatus());
        if (req.getNote() != null) assessment.setNote(req.getNote());

        VerificationStatus newStatus = req.getVerificationStatus();
        if (newStatus == VerificationStatus.SAHADA_DOGRULANDI) {
            assessment.setVerifiedBy(actor);
            assessment.setVerifiedAt(OffsetDateTime.now());
        } else if (newStatus == VerificationStatus.KOORDINATOR_ONAYLADI) {
            // Koordinatör onayı — sadece ADMIN veya DISTRICT_COORDINATOR yapabilir
            if (role == UserRole.NEIGHBORHOOD_COORDINATOR) {
                throw new BusinessRuleException("Koordinatör onayı yalnızca ilçe koordinatörü veya admin tarafından verilebilir");
            }
            assessment.setApprovedBy(actor);
            assessment.setApprovedAt(OffsetDateTime.now());
        }
        // ASSIGNED status: no extra fields to set — handled via assignment flow

        DamageAssessment updated = damageAssessmentRepository.save(assessment);
        VerificationStatus publishedStatus = req.getVerificationStatus();
        eventPublisher.publishEvent(new DamageReportStatusChangedEmailEvent(id, publishedStatus));
        return toResponse(updated);
    }

    // ── Assignment CRUD ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DamageAssessmentAssignmentResponse> listAssignments(UUID damageAssessmentId, UserPrincipal principal) {
        findById(damageAssessmentId); // ensures exists
        checkAssignmentAccess(damageAssessmentId, principal);
        return assignmentRepository.findByDamageAssessmentIdAndActiveTrue(damageAssessmentId)
                .stream().map(this::toAssignmentResponse).toList();
    }

    @Transactional
    public DamageAssessmentAssignmentResponse assign(UUID damageAssessmentId,
                                                     AssignDamageAssessmentRequest req,
                                                     UserPrincipal principal) {
        DamageAssessment assessment = findById(damageAssessmentId);
        checkAssignmentAccess(damageAssessmentId, principal);

        if (assignmentRepository.existsByDamageAssessmentIdAndUserIdAndActiveTrue(damageAssessmentId, req.getUserId())) {
            throw new BusinessRuleException("Bu kullanıcı zaten bu hasar tespitine atanmış");
        }

        User assignee = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.getUserId()));
        User assignedBy = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        DamageAssessmentAssignment assignment = DamageAssessmentAssignment.builder()
                .damageAssessment(assessment)
                .user(assignee)
                .assignedBy(assignedBy)
                .assignedAt(OffsetDateTime.now())
                .active(true)
                .build();

        DamageAssessmentAssignment saved = assignmentRepository.save(assignment);

        // Auto-transition status to ASSIGNED if currently INCELEME_GEREKIYOR
        boolean statusTransitioned = false;
        if (assessment.getVerificationStatus() == VerificationStatus.INCELEME_GEREKIYOR) {
            assessment.setVerificationStatus(VerificationStatus.ASSIGNED);
            damageAssessmentRepository.save(assessment);
            statusTransitioned = true;
        }

        // Notify the assignee (field worker) about their new task
        eventPublisher.publishEvent(new DamageReportFieldTeamAssignedEmailEvent(damageAssessmentId, assignee.getId()));

        // Notify the reporter that a field team has been dispatched (§3.3)
        if (statusTransitioned) {
            eventPublisher.publishEvent(new DamageReportStatusChangedEmailEvent(damageAssessmentId, VerificationStatus.ASSIGNED));
        }

        return toAssignmentResponse(saved);
    }

    @Transactional
    public void removeAssignment(UUID damageAssessmentId, UUID assignmentId, UserPrincipal principal) {
        checkAssignmentAccess(damageAssessmentId, principal);

        DamageAssessmentAssignment assignment = assignmentRepository
                .findByIdAndDamageAssessmentIdAndActiveTrue(assignmentId, damageAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        assignment.setActive(false);
        assignment.setRemovedAt(OffsetDateTime.now());
        assignmentRepository.save(assignment);

        // Revert status to INCELEME_GEREKIYOR if no more active assignments
        DamageAssessment assessment = findById(damageAssessmentId);
        boolean hasMoreAssignments = !assignmentRepository
                .findByDamageAssessmentIdAndActiveTrue(damageAssessmentId).isEmpty();
        if (!hasMoreAssignments && assessment.getVerificationStatus() == VerificationStatus.ASSIGNED) {
            assessment.setVerificationStatus(VerificationStatus.INCELEME_GEREKIYOR);
            damageAssessmentRepository.save(assessment);
        }
    }

    @Transactional(readOnly = true)
    public List<EligibleAssigneeResponse> getEligibleAssignees(UUID damageAssessmentId, UserPrincipal principal) {
        checkAssignmentAccess(damageAssessmentId, principal);
        DamageAssessment assessment = findById(damageAssessmentId);

        UUID districtId = assessment.getDistrict().getId();

        List<EventVolunteer> candidates = eventVolunteerRepository.findEligibleAssignees(
                TeamName.HASAR_TESPIT_EKIBI,
                List.of(EventStatus.IN_PROGRESS, EventStatus.OPEN),
                districtId,
                EventVolunteerStatus.ASSIGNED,
                damageAssessmentId);

        LinkedHashMap<UUID, EligibleAssigneeResponse> seen = new LinkedHashMap<>();
        for (EventVolunteer ev : candidates) {
            UUID userId = ev.getUser().getId();
            if (!seen.containsKey(userId)) {
                seen.put(userId, EligibleAssigneeResponse.builder()
                        .userId(userId)
                        .firstName(ev.getUser().getFirstName())
                        .lastName(ev.getUser().getLastName())
                        .email(ev.getUser().getEmail())
                        .phone(ev.getUser().getPhone())
                        .eventId(ev.getEvent().getId())
                        .eventTitle(ev.getEvent().getTitle())
                        .build());
            }
        }
        return new ArrayList<>(seen.values());
    }

    private void checkAssignmentAccess(UUID damageAssessmentId, UserPrincipal principal) {
        UserRole role = principal.getRole();
        if (role == UserRole.VOLUNTEER) {
            throw new BusinessRuleException("Gönüllüler görevli atayamaz");
        }
        if (role == UserRole.ADMIN) return;

        DamageAssessment assessment = findById(damageAssessmentId);

        if (role == UserRole.DISTRICT_COORDINATOR) {
            UUID assignedDistrictId = districtRepository.findByCoordinatorId(principal.getId())
                    .map(District::getId)
                    .orElse(principal.getDistrictId());
            if (!assessment.getDistrict().getId().equals(assignedDistrictId)) {
                throw new BusinessRuleException("Sadece kendi ilçenizdeki kayıtlara görevli atayabilirsiniz");
            }
        } else if (role == UserRole.NEIGHBORHOOD_COORDINATOR) {
            UUID assignedNeighborhoodId = neighborhoodRepository.findByCoordinatorId(principal.getId())
                    .map(Neighborhood::getId)
                    .orElse(principal.getNeighborhoodId());
            if (!assessment.getNeighborhood().getId().equals(assignedNeighborhoodId)) {
                throw new BusinessRuleException("Sadece kendi mahallenizin kayıtlarına görevli atayabilirsiniz");
            }
        }
    }

    // ── Photo serving ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public void servePhoto(UUID downloadToken, HttpServletResponse response) {
        DamageAssessmentPhoto photo = damageAssessmentPhotoRepository.findByDownloadToken(downloadToken)
                .orElseThrow(() -> new ResourceNotFoundException("Fotoğraf", "downloadToken", downloadToken));

        Path filePath = Paths.get(localStoragePath, photo.getStorageKey());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Dosya", "path", photo.getStorageKey());
        }

        response.setContentType(photo.getMimeType());
        response.setHeader("Content-Disposition", "inline; filename=\"" + photo.getFileName() + "\"");
        if (photo.getFileSizeBytes() != null) {
            response.setContentLengthLong(photo.getFileSizeBytes());
        }

        try {
            Files.copy(filePath, response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessRuleException("Fotoğraf okunurken hata oluştu");
        }
    }

    @Transactional(readOnly = true)
    public List<MapDamageSummaryResponse> getDamageSummary() {
        List<Object[]> rows = damageAssessmentRepository.findNeighborhoodDamageSummary();
        return rows.stream().map(r -> MapDamageSummaryResponse.builder()
                .neighborhoodId((UUID) r[0])
                .totalCount(((Number) r[1]).intValue())
                .lightCount(((Number) r[2]).intValue())
                .moderateCount(((Number) r[3]).intValue())
                .heavyCount(((Number) r[4]).intValue())
                .collapsedCount(((Number) r[5]).intValue())
                .build()).toList();
    }

    @Transactional(readOnly = true)
    public List<DamagePointResponse> getDamagePoints(UUID districtId, UUID neighborhoodId) {
        // §1 — Haritada YALNIZCA sahada doğrulanıp koordinatör tarafından onaylanan
        // (KOORDINATOR_ONAYLADI) ve koordinatı bulunan kayıtlar pin olarak gösterilir.
        // İnceleme bekleyen / görevli atanmış / yalnızca saha doğrulanmış / onaysız
        // kayıtlar sistemde durur fakat haritada görünmez. Filtre backend'de uygulanır
        // (asıl güvenilir filtre); stream üzerindeki ek kontrol savunma amaçlıdır.
        final VerificationStatus approved = VerificationStatus.KOORDINATOR_ONAYLADI;
        List<DamageAssessment> assessments;
        if (neighborhoodId != null) {
            assessments = damageAssessmentRepository.findApprovedByNeighborhoodIdWithCoordinates(neighborhoodId, approved);
        } else if (districtId != null) {
            assessments = damageAssessmentRepository.findApprovedByDistrictIdWithCoordinates(districtId, approved);
        } else {
            assessments = damageAssessmentRepository.findApprovedWithCoordinates(approved);
        }
        return assessments.stream()
                .filter(a -> a.getVerificationStatus() == approved
                        && a.getLatitude() != null && a.getLongitude() != null)
                .map(a -> DamagePointResponse.builder()
                .id(a.getId())
                .latitude(a.getLatitude().doubleValue())
                .longitude(a.getLongitude().doubleValue())
                .damageLevel(a.getDamageLevel().name())
                .damageLevelLabel(a.getDamageLevel().getLabel())
                .address(a.getAddress())
                .verificationStatus(a.getVerificationStatus().name())
                .verificationStatusLabel(a.getVerificationStatus().getLabel())
                .note(a.getNote())
                .districtId(a.getDistrict().getId())
                .districtName(a.getDistrict().getName())
                .neighborhoodId(a.getNeighborhood().getId())
                .neighborhoodName(a.getNeighborhood().getName())
                .gasLeakRisk(a.isGasLeakRisk())
                .collapseRisk(a.isCollapseRisk())
                .emergencyEvacuationNeeded(a.isEmergencyEvacuationNeeded())
                .casualtiesSuspected(a.isCasualtiesSuspected())
                .createdAt(a.getCreatedAt())
                .build()).toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validatePhotoMimeType(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new BusinessRuleException("Fotoğraf dosyası boş olamaz");
        }
        String mime = photo.getContentType();
        if (mime == null || !ALLOWED_PHOTO_MIME_TYPES.contains(mime.toLowerCase())) {
            throw new BusinessRuleException(
                    "Geçersiz dosya formatı. Yalnızca JPEG, PNG ve WEBP formatındaki fotoğraflar kabul edilmektedir. " +
                    "Alınan format: " + (mime != null ? mime : "bilinmiyor"));
        }
        if (photo.getSize() > 20L * 1024 * 1024) {
            throw new BusinessRuleException("Fotoğraf boyutu 20MB'ı geçemez");
        }
    }

    private DamageAssessmentPhoto savePhoto(MultipartFile photo, DamageAssessment assessment,
                                             PhotoType photoType, User uploadedBy) {
        String originalName = photo.getOriginalFilename() != null ? photo.getOriginalFilename() : "photo.jpg";
        String storageKey = "damage/" + assessment.getId().toString() + "/" + UUID.randomUUID() + "_" + originalName;
        Path target = Paths.get(localStoragePath, storageKey);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, photo.getBytes());
        } catch (IOException e) {
            throw new BusinessRuleException("Fotoğraf kaydedilirken hata oluştu: " + e.getMessage());
        }

        DamageAssessmentPhoto photoEntity = DamageAssessmentPhoto.builder()
                .damageAssessment(assessment)
                .storageKey(storageKey)
                .fileName(originalName)
                .mimeType(photo.getContentType() != null ? photo.getContentType() : "image/jpeg")
                .fileSizeBytes(photo.getSize())
                .photoType(photoType)
                .uploadedBy(uploadedBy)
                .build();

        return damageAssessmentPhotoRepository.save(photoEntity);
    }

    private DamageAssessment findById(UUID id) {
        return damageAssessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DamageAssessment", "id", id));
    }

    private DamageAssessmentAssignmentResponse toAssignmentResponse(DamageAssessmentAssignment a) {
        User u = a.getUser();
        User by = a.getAssignedBy();
        return DamageAssessmentAssignmentResponse.builder()
                .id(a.getId())
                .userId(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .assignedById(by.getId())
                .assignedByFirstName(by.getFirstName())
                .assignedByLastName(by.getLastName())
                .assignedAt(a.getAssignedAt())
                .build();
    }

    private DamageAssessmentResponse toResponse(DamageAssessment a) {
        List<String> reporterPhotoUrls = a.getPhotos().stream()
                .filter(p -> p.getPhotoType() == PhotoType.REPORTER_PHOTO)
                .map(p -> baseUrl + "/api/damage-assessments/photos/" + p.getDownloadToken())
                .collect(Collectors.toList());
        List<String> fieldPhotoUrls = a.getPhotos().stream()
                .filter(p -> p.getPhotoType() == PhotoType.ASSIGNEE_FIELD_PHOTO)
                .map(p -> baseUrl + "/api/damage-assessments/photos/" + p.getDownloadToken())
                .collect(Collectors.toList());
        List<String> photoUrls = new ArrayList<>(reporterPhotoUrls);
        photoUrls.addAll(fieldPhotoUrls);

        List<DamageAssessmentAssignmentResponse> assignments = assignmentRepository
                .findByDamageAssessmentIdAndActiveTrue(a.getId())
                .stream().map(this::toAssignmentResponse).toList();

        return DamageAssessmentResponse.builder()
                .id(a.getId())
                .districtId(a.getDistrict().getId())
                .districtName(a.getDistrict().getName())
                .neighborhoodId(a.getNeighborhood().getId())
                .neighborhoodName(a.getNeighborhood().getName())
                .streetName(a.getStreetName())
                .buildingNo(a.getBuildingNo())
                .address(a.getAddress())
                .latitude(a.getLatitude() != null ? a.getLatitude().doubleValue() : null)
                .longitude(a.getLongitude() != null ? a.getLongitude().doubleValue() : null)
                .locationSource(a.getLocationSource() != null ? a.getLocationSource().name() : null)
                .locationVerified(a.isLocationVerified())
                .buildingType(a.getBuildingType())
                .floorCount(a.getFloorCount())
                .occupancyType(a.getOccupancyType())
                .damageLevel(a.getDamageLevel().name())
                .damageLevelLabel(a.getDamageLevel().getLabel())
                .collapseRisk(a.isCollapseRisk())
                .emergencyEvacuationNeeded(a.isEmergencyEvacuationNeeded())
                .casualtiesSuspected(a.isCasualtiesSuspected())
                .blockedRoad(a.isBlockedRoad())
                .gasLeakRisk(a.isGasLeakRisk())
                .note(a.getNote())
                .verificationStatus(a.getVerificationStatus().name())
                .verificationStatusLabel(a.getVerificationStatus().getLabel())
                .reportedBy(a.getReportedBy() != null
                        ? a.getReportedBy().getFirstName() + " " + a.getReportedBy().getLastName() : null)
                .verifiedBy(a.getVerifiedBy() != null
                        ? a.getVerifiedBy().getFirstName() + " " + a.getVerifiedBy().getLastName() : null)
                .verifiedAt(a.getVerifiedAt())
                .approvedBy(a.getApprovedBy() != null
                        ? a.getApprovedBy().getFirstName() + " " + a.getApprovedBy().getLastName() : null)
                .approvedAt(a.getApprovedAt())
                .photoUrls(photoUrls)
                .reporterPhotoUrls(reporterPhotoUrls)
                .fieldPhotoUrls(fieldPhotoUrls)
                .assignments(assignments)
                .aiComment(a.getAiComment())
                .aiConfidence(a.getAiConfidence() != null ? a.getAiConfidence().name() : null)
                .aiConfidenceLabel(a.getAiConfidence() != null ? a.getAiConfidence().getLabel() : null)
                .aiModel(a.getAiModel())
                .aiAnalyzedAt(a.getAiAnalyzedAt())
                .aiAnalysisStatus(a.getAiAnalysisStatus() != null ? a.getAiAnalysisStatus().name() : null)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
