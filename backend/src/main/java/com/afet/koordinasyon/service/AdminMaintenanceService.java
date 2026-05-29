package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.DamageAssessmentPhoto;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.dto.request.PurgeOperationalDataRequest;
import com.afet.koordinasyon.dto.response.PurgeOperationalDataResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMaintenanceService {

    private static final List<String> DEFAULT_PROTECTED_EMAILS = List.of(
            "admin.gmail.com",
            "admin@afetkoordinasyon.istanbul"
    );

    @Value("${app.storage.local-path:.local-storage}")
    private String localStoragePath;

    private final UserRepository userRepository;
    private final EventVolunteerRepository eventVolunteerRepository;
    private final EventRepository eventRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final DamageAssessmentPhotoRepository damageAssessmentPhotoRepository;
    private final ResourceRequestRepository resourceRequestRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final DocumentRepository documentRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final SimulationNotificationLogRepository simulationNotificationLogRepository;
    private final EmergencyStatusMessageLogRepository emergencyStatusMessageLogRepository;
    private final EmergencyStatusMessageRepository emergencyStatusMessageRepository;
    private final EarthquakeSimulationRepository earthquakeSimulationRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final DistrictCoordinationCenterRepository districtCoordinationCenterRepository;
    private final NeighborhoodCoordinationCenterRepository neighborhoodCoordinationCenterRepository;

    /**
     * Tüm operasyonel veriyi (veya seçilenleri) tek transaction içinde temizler.
     * Protected e-posta listesindeki adminler asla silinmez.
     */
    @Transactional
    public PurgeOperationalDataResponse purgeOperationalData(PurgeOperationalDataRequest req, UUID actorId) {
        if (!"PURGE".equals(req.getConfirmationText())) {
            throw new BusinessRuleException("Onay metni hatalı. Tam olarak 'PURGE' yazmalısınız.");
        }

        List<String> protectedEmails = resolveProtectedEmails(req.getProtectedEmails(), actorId);

        int deletedEventVolunteers = 0;
        int deletedEvents = 0;
        int deletedDamagePhotos = 0;
        int deletedDamageAssessments = 0;
        int deletedResourceRequests = 0;
        int deletedUsers = 0;
        List<String> warnings = new ArrayList<>();

        // ── 1. Ekip durumu (events + volunteers) ─────────────────────────────
        if (req.isPurgeEvents()) {
            deletedEventVolunteers = eventVolunteerRepository.deleteAllVolunteers();
            deletedEvents = eventRepository.deleteAllEvents();
            log.info("[Purge] Silinen gönüllü: {}, etkinlik: {}", deletedEventVolunteers, deletedEvents);
        }

        // ── 2. Hasar tespiti ─────────────────────────────────────────────────
        if (req.isPurgeDamageAssessments()) {
            List<DamageAssessmentPhoto> photos = damageAssessmentPhotoRepository.findAll();
            deletedDamagePhotos = photos.size();
            for (DamageAssessmentPhoto photo : photos) {
                try {
                    Files.deleteIfExists(Paths.get(localStoragePath, photo.getStorageKey()));
                } catch (IOException e) {
                    String w = "Fiziksel fotoğraf silinemedi: " + photo.getStorageKey();
                    warnings.add(w);
                    log.warn("[Purge] {}", w);
                }
            }
            damageAssessmentPhotoRepository.deleteAllPhotos();
            deletedDamageAssessments = damageAssessmentRepository.deleteAllAssessments();
            log.info("[Purge] Silinen fotoğraf: {}, hasar tespiti: {}", deletedDamagePhotos, deletedDamageAssessments);
        }

        // ── 3. Kaynak talepleri ───────────────────────────────────────────────
        if (req.isPurgeResourceRequests()) {
            deletedResourceRequests = resourceRequestRepository.deleteAllRequests();
            log.info("[Purge] Silinen kaynak talebi: {}", deletedResourceRequests);
        }

        // ── 4. Kullanıcılar ───────────────────────────────────────────────────
        if (req.isPurgeUsers()) {
            List<User> toDelete = userRepository.findByEmailNotIn(protectedEmails);
            List<UUID> toDeleteIds = toDelete.stream().map(User::getId).toList();

            if (!toDeleteIds.isEmpty()) {
                User protectedAdmin = findProtectedAdmin(protectedEmails);

                // 4a. Nullable FK → NULL
                districtRepository.clearCoordinatorIn(toDeleteIds);
                districtRepository.clearCoordinatorLocationUpdatedByIn(toDeleteIds);
                neighborhoodRepository.clearCoordinatorIn(toDeleteIds);
                neighborhoodRepository.clearCoordinatorLocationUpdatedByIn(toDeleteIds);
                districtCoordinationCenterRepository.clearUpdatedByIn(toDeleteIds);
                neighborhoodCoordinationCenterRepository.clearUpdatedByIn(toDeleteIds);

                // 4b. NOT NULL FK → protected admin'e yeniden ata (zaten purge edilmediyse)
                if (!req.isPurgeDamageAssessments()) {
                    damageAssessmentRepository.clearVerifiedByIn(toDeleteIds);
                    damageAssessmentRepository.clearApprovedByIn(toDeleteIds);
                    damageAssessmentRepository.reassignReportedBy(protectedAdmin, toDeleteIds);
                }
                if (!req.isPurgeResourceRequests()) {
                    resourceRequestRepository.clearAssignedToIn(toDeleteIds);
                    resourceRequestRepository.clearClosedByIn(toDeleteIds);
                    resourceRequestRepository.reassignCreatedBy(protectedAdmin, toDeleteIds);
                }
                if (!req.isPurgeEvents()) {
                    eventVolunteerRepository.deleteByUserIdIn(toDeleteIds);
                    eventRepository.reassignCreatedBy(protectedAdmin, toDeleteIds);
                }
                earthquakeSimulationRepository.reassignCreatedBy(protectedAdmin, toDeleteIds);
                emergencyStatusMessageRepository.reassignSenderIn(protectedAdmin, toDeleteIds);

                // 4c. Bağımlı kayıtları sil
                emergencyStatusMessageLogRepository.deleteByRecipientIdIn(toDeleteIds);
                simulationNotificationLogRepository.deleteByUserIdIn(toDeleteIds);
                emergencyContactRepository.deleteByOwnerOrContactIn(toDeleteIds);
                teamMembershipRepository.deleteByUserIdIn(toDeleteIds);
                documentRepository.clearReviewedByIn(toDeleteIds);
                documentRepository.deleteByUserIdIn(toDeleteIds);
                refreshTokenRepository.deleteByUserIdIn(toDeleteIds);
                emailVerificationTokenRepository.deleteByUserIdIn(toDeleteIds);

                deletedUsers = toDeleteIds.size();
                userRepository.deleteAll(toDelete);
                log.info("[Purge] Silinen kullanıcı: {}", deletedUsers);
            }
        }

        // ── 5. Risk skor sıfırlama ────────────────────────────────────────────
        // Events veya resource requests temizlendiyse tüm risk skorlarını sıfırla
        if (req.isPurgeEvents() || req.isPurgeResourceRequests()) {
            districtRepository.resetAllRiskScores();
            neighborhoodRepository.resetAllRiskScores();
            log.info("[Purge] Risk skorları sıfırlandı");
        }

        return PurgeOperationalDataResponse.builder()
                .deletedEventVolunteers(deletedEventVolunteers)
                .deletedEvents(deletedEvents)
                .deletedDamagePhotos(deletedDamagePhotos)
                .deletedDamageAssessments(deletedDamageAssessments)
                .deletedResourceRequests(deletedResourceRequests)
                .deletedUsers(deletedUsers)
                .warnings(warnings)
                .build();
    }

    /**
     * Tek bir kullanıcıyı hard-delete ile siler.
     * Protected adminler ve kendi hesabını silme engellenir (422).
     *
     * @param protectedEmailsOverride null → uygulama varsayılanı kullanılır
     */
    @Transactional
    public void deleteUserHard(UUID userId, UUID actorId, List<String> protectedEmailsOverride) {
        List<String> protectedEmails = resolveProtectedEmails(protectedEmailsOverride, actorId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (protectedEmails.contains(user.getEmail())) {
            throw new BusinessRuleException("Bu admin kullanıcı korunuyor ve silinemez.");
        }
        if (userId.equals(actorId)) {
            throw new BusinessRuleException("Kendi hesabınızı silemezsiniz.");
        }

        User protectedAdmin = findProtectedAdmin(protectedEmails);
        List<UUID> userIdList = List.of(userId);

        // Nullable FK → NULL
        districtRepository.clearCoordinatorIn(userIdList);
        districtRepository.clearCoordinatorLocationUpdatedByIn(userIdList);
        neighborhoodRepository.clearCoordinatorIn(userIdList);
        neighborhoodRepository.clearCoordinatorLocationUpdatedByIn(userIdList);
        districtCoordinationCenterRepository.clearUpdatedByIn(userIdList);
        neighborhoodCoordinationCenterRepository.clearUpdatedByIn(userIdList);

        // NOT NULL FK → protected admin'e yeniden ata
        eventRepository.reassignCreatedBy(protectedAdmin, userIdList);
        damageAssessmentRepository.reassignReportedBy(protectedAdmin, userIdList);
        damageAssessmentRepository.clearVerifiedByIn(userIdList);
        damageAssessmentRepository.clearApprovedByIn(userIdList);
        resourceRequestRepository.reassignCreatedBy(protectedAdmin, userIdList);
        resourceRequestRepository.clearAssignedToIn(userIdList);
        resourceRequestRepository.clearClosedByIn(userIdList);
        earthquakeSimulationRepository.reassignCreatedBy(protectedAdmin, userIdList);
        emergencyStatusMessageRepository.reassignSenderIn(protectedAdmin, userIdList);

        // Bağımlı kayıtları sil
        emergencyStatusMessageLogRepository.deleteByRecipientIdIn(userIdList);
        simulationNotificationLogRepository.deleteByUserIdIn(userIdList);
        emergencyContactRepository.deleteByOwnerOrContactIn(userIdList);
        eventVolunteerRepository.deleteByUserIdIn(userIdList);
        teamMembershipRepository.deleteByUserIdIn(userIdList);
        documentRepository.clearReviewedByIn(userIdList);
        documentRepository.deleteByUserIdIn(userIdList);
        refreshTokenRepository.deleteByUserIdIn(userIdList);
        emailVerificationTokenRepository.deleteByUserIdIn(userIdList);

        userRepository.delete(user);
        log.info("[DeleteUser] Kullanıcı silindi: {} ({})", user.getEmail(), userId);
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private List<String> resolveProtectedEmails(List<String> provided, UUID actorId) {
        List<String> base = (provided != null && !provided.isEmpty()) ? provided : DEFAULT_PROTECTED_EMAILS;
        // Çağrı yapan admin her halükarda korunur
        List<String> result = new ArrayList<>(base);
        userRepository.findById(actorId).ifPresent(actor -> {
            if (!result.contains(actor.getEmail())) {
                result.add(actor.getEmail());
            }
        });
        return result;
    }

    private User findProtectedAdmin(List<String> protectedEmails) {
        return protectedEmails.stream()
                .map(email -> userRepository.findByEmail(email).orElse(null))
                .filter(u -> u != null)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Korunan admin kullanıcı veritabanında bulunamadı. Önce bir admin kullanıcı oluşturun."));
    }
}
