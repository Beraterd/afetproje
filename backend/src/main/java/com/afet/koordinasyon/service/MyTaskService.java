package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.DamageAssessment;
import com.afet.koordinasyon.domain.entity.DamageAssessmentAssignment;
import com.afet.koordinasyon.domain.entity.DamageAssessmentPhoto;
import com.afet.koordinasyon.domain.enums.AssignmentStatus;
import com.afet.koordinasyon.domain.enums.PhotoType;
import com.afet.koordinasyon.domain.enums.VerificationStatus;
import com.afet.koordinasyon.dto.response.DamageAssessmentTaskResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DamageAssessmentAssignmentRepository;
import com.afet.koordinasyon.repository.DamageAssessmentPhotoRepository;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MyTaskService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    @Value("${app.storage.local-path:.local-storage}")
    private String localStoragePath;

    @Value("${app.base-url}")
    private String baseUrl;

    private final DamageAssessmentAssignmentRepository assignmentRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final DamageAssessmentPhotoRepository photoRepository;
    private final DamageAssessmentAiService damageAssessmentAiService;
    private final com.afet.koordinasyon.service.ai.DamageAiQueueService damageAiQueueService;

    @Transactional(readOnly = true)
    public List<DamageAssessmentTaskResponse> getMyTasks(UserPrincipal principal) {
        return assignmentRepository.findMyTasks(principal.getId())
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DamageAssessmentTaskResponse getTaskDetail(UUID assignmentId, UserPrincipal principal) {
        DamageAssessmentAssignment assignment = findMyAssignment(assignmentId, principal.getId());
        return toTaskResponse(assignment);
    }

    @Transactional
    public void uploadFieldPhotos(UUID assignmentId, List<MultipartFile> photos, UserPrincipal principal) {
        if (photos == null || photos.isEmpty()) {
            throw new BusinessRuleException("En az 1 fotoğraf yüklenmesi zorunludur");
        }
        DamageAssessmentAssignment assignment = findMyAssignment(assignmentId, principal.getId());
        DamageAssessment assessment = assignment.getDamageAssessment();

        for (MultipartFile photo : photos) {
            validatePhoto(photo);
        }
        for (MultipartFile photo : photos) {
            saveFieldPhoto(photo, assessment, assignment.getUser());
        }

        final UUID assessmentId = assessment.getId();
        final com.afet.koordinasyon.domain.enums.DamageLevel assessmentLevel = assessment.getDamageLevel();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                damageAiQueueService.enqueue(assessmentId, assessmentLevel);
            }
        });
    }

    @Transactional
    public DamageAssessmentTaskResponse markFieldVerified(UUID assignmentId, UserPrincipal principal) {
        DamageAssessmentAssignment assignment = findMyAssignment(assignmentId, principal.getId());

        if (assignment.getAssignmentStatus() == AssignmentStatus.COMPLETED) {
            throw new BusinessRuleException("Tamamlanmış görev güncellenemez");
        }

        assignment.setAssignmentStatus(AssignmentStatus.FIELD_VERIFIED);
        assignmentRepository.save(assignment);

        DamageAssessment assessment = assignment.getDamageAssessment();
        if (assessment.getVerificationStatus() == VerificationStatus.INCELEME_GEREKIYOR
                || assessment.getVerificationStatus() == VerificationStatus.ASSIGNED) {
            assessment.setVerificationStatus(VerificationStatus.SAHADA_DOGRULANDI);
            damageAssessmentRepository.save(assessment);
        }

        return toTaskResponse(assignment);
    }

    @Transactional
    public DamageAssessmentTaskResponse completeTask(UUID assignmentId, UserPrincipal principal) {
        DamageAssessmentAssignment assignment = findMyAssignment(assignmentId, principal.getId());

        if (assignment.getAssignmentStatus() == AssignmentStatus.COMPLETED) {
            throw new BusinessRuleException("Görev zaten tamamlandı");
        }

        assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
        assignmentRepository.save(assignment);

        return toTaskResponse(assignment);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private DamageAssessmentAssignment findMyAssignment(UUID assignmentId, UUID userId) {
        DamageAssessmentAssignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        if (!assignment.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Bu göreve erişim yetkiniz yok");
        }
        if (!assignment.isActive()) {
            throw new BusinessRuleException("Bu görev artık aktif değil");
        }
        return assignment;
    }

    private void validatePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new BusinessRuleException("Fotoğraf dosyası boş olamaz");
        }
        String mime = photo.getContentType();
        if (mime == null || !ALLOWED_MIME_TYPES.contains(mime.toLowerCase())) {
            throw new BusinessRuleException(
                    "Geçersiz dosya formatı. Yalnızca JPEG, PNG ve WEBP kabul edilmektedir. Alınan format: "
                            + (mime != null ? mime : "bilinmiyor"));
        }
        if (photo.getSize() > 20L * 1024 * 1024) {
            throw new BusinessRuleException("Fotoğraf boyutu 20MB'ı geçemez");
        }
    }

    private void saveFieldPhoto(MultipartFile photo, DamageAssessment assessment,
                                 com.afet.koordinasyon.domain.entity.User uploadedBy) {
        String originalName = photo.getOriginalFilename() != null ? photo.getOriginalFilename() : "photo.jpg";
        String storageKey = "damage/" + assessment.getId() + "/field/" + UUID.randomUUID() + "_" + originalName;
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
                .photoType(PhotoType.ASSIGNEE_FIELD_PHOTO)
                .uploadedBy(uploadedBy)
                .build();

        photoRepository.save(photoEntity);
    }

    private DamageAssessmentTaskResponse toTaskResponse(DamageAssessmentAssignment assignment) {
        DamageAssessment a = assignment.getDamageAssessment();

        List<String> reporterPhotoUrls = a.getPhotos().stream()
                .filter(p -> p.getPhotoType() == PhotoType.REPORTER_PHOTO)
                .map(p -> baseUrl + "/api/damage-assessments/photos/" + p.getDownloadToken())
                .toList();

        List<String> fieldPhotoUrls = a.getPhotos().stream()
                .filter(p -> p.getPhotoType() == PhotoType.ASSIGNEE_FIELD_PHOTO)
                .map(p -> baseUrl + "/api/damage-assessments/photos/" + p.getDownloadToken())
                .toList();

        var by = assignment.getAssignedBy();

        return DamageAssessmentTaskResponse.builder()
                .assignmentId(assignment.getId())
                .damageAssessmentId(a.getId())
                .address(a.getAddress())
                .districtName(a.getDistrict().getName())
                .neighborhoodName(a.getNeighborhood().getName())
                .damageLevel(a.getDamageLevel().name())
                .damageLevelLabel(a.getDamageLevel().getLabel())
                .latitude(a.getLatitude() != null ? a.getLatitude().doubleValue() : null)
                .longitude(a.getLongitude() != null ? a.getLongitude().doubleValue() : null)
                .note(a.getNote())
                .collapseRisk(a.isCollapseRisk())
                .emergencyEvacuationNeeded(a.isEmergencyEvacuationNeeded())
                .casualtiesSuspected(a.isCasualtiesSuspected())
                .gasLeakRisk(a.isGasLeakRisk())
                .assignmentStatus(assignment.getAssignmentStatus().name())
                .assignmentStatusLabel(assignment.getAssignmentStatus().getLabel())
                .assignedByName(by.getFirstName() + " " + by.getLastName())
                .assignedAt(assignment.getAssignedAt())
                .active(assignment.isActive())
                .reporterPhotoUrls(reporterPhotoUrls)
                .fieldPhotoUrls(fieldPhotoUrls)
                .aiComment(a.getAiComment())
                .aiConfidence(a.getAiConfidence() != null ? a.getAiConfidence().name() : null)
                .aiConfidenceLabel(a.getAiConfidence() != null ? a.getAiConfidence().getLabel() : null)
                .aiAnalysisStatus(a.getAiAnalysisStatus() != null ? a.getAiAnalysisStatus().name() : null)
                .build();
    }
}
