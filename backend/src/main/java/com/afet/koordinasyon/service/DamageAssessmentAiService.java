package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.DamageAssessment;
import com.afet.koordinasyon.domain.enums.AiAnalysisStatus;
import com.afet.koordinasyon.domain.enums.PhotoType;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.service.ai.AiAssessmentResult;
import com.afet.koordinasyon.service.ai.AiDamageAssessmentProvider;
import com.afet.koordinasyon.service.ai.PhotoData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DamageAssessmentAiService {

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MINUTES = {1, 5, 15};

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.provider:claude}")
    private String aiProvider;

    @Value("${ai.claude-api-key:}")
    private String claudeApiKey;

    @Value("${ai.openai-api-key:}")
    private String openaiApiKey;

    @Value("${ai.model:claude-3-5-sonnet-latest}")
    private String aiModel;

    @Value("${ai.max-photos:3}")
    private int maxPhotos;

    @Value("${app.storage.local-path:.local-storage}")
    private String localStoragePath;

    private final DamageAssessmentRepository damageAssessmentRepository;
    private final AiDamageAssessmentProvider assessmentProvider;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        boolean claudeKeyPresent = claudeApiKey != null && !claudeApiKey.isBlank();
        boolean openaiKeyPresent = openaiApiKey != null && !openaiApiKey.isBlank();
        log.info("AI config: enabled={}, provider={}, model={}, claudeKeyPresent={}, openaiKeyPresent={}",
                aiEnabled, aiProvider, aiModel, claudeKeyPresent, openaiKeyPresent);

        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Background analysis: runs in aiTaskExecutor pool, dispatched by DamageAiQueueService.
     * Each DB step runs in its own REQUIRES_NEW transaction to ensure intermediate states
     * (PROCESSING) are visible immediately to API callers without holding a long TX.
     */
    @Async("aiTaskExecutor")
    public void generateAiAssessment(UUID damageAssessmentId) {
        if (!aiEnabled) {
            log.debug("AI analysis skipped: ai.enabled=false for {}", damageAssessmentId);
            return;
        }

        String resolvedKey = resolveApiKey();
        if (resolvedKey == null || resolvedKey.isBlank()) {
            log.warn("AI analysis skipped: no API key for provider='{}' (assessment={}). " +
                    "Set CLAUDE_API_KEY or OPENAI_API_KEY environment variable.", aiProvider, damageAssessmentId);
            return;
        }

        if (!"claude".equalsIgnoreCase(aiProvider) && !"openai".equalsIgnoreCase(aiProvider)) {
            log.error("AI analysis skipped: unsupported provider '{}' for {}.", aiProvider, damageAssessmentId);
            return;
        }

        // Step 1: Load photos and compute hash. Skip if data unchanged and already COMPLETED.
        String[] hashHolder = new String[1];
        List<PhotoData> photos = transactionTemplate.execute(status -> {
            DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElse(null);
            if (a == null) {
                log.warn("AI analysis: assessment {} not found", damageAssessmentId);
                return null;
            }

            List<PhotoData> collected = collectPhotos(a);
            String hash = computeHash(a);
            hashHolder[0] = hash;

            // Cache hit: data unchanged, analysis already done
            if (hash.equals(a.getDamageHash()) && a.getAiAnalysisStatus() == AiAnalysisStatus.COMPLETED) {
                log.info("AI analysis: {} unchanged (hash match) — skipping re-analysis", damageAssessmentId);
                return null;
            }

            return collected;
        });

        if (photos == null) return; // cache hit or not found

        if (photos.isEmpty()) {
            log.warn("AI analysis: no readable photos found for assessment {}", damageAssessmentId);
            transactionTemplate.executeWithoutResult(status -> {
                DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElseThrow();
                scheduleRetryOrFail(a, "Analiz için erişilebilir fotoğraf bulunamadı.");
                damageAssessmentRepository.save(a);
            });
            return;
        }

        // Step 2: Mark PROCESSING (commit immediately for visibility).
        boolean started = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElse(null);
            if (a == null) return false;
            if (a.getAiAnalysisStatus() == AiAnalysisStatus.PROCESSING) {
                log.info("AI analysis: {} already PROCESSING — skipping duplicate", damageAssessmentId);
                return false;
            }
            a.setAiAnalysisStatus(AiAnalysisStatus.PROCESSING);
            a.setDamageHash(hashHolder[0]);
            damageAssessmentRepository.save(a);
            return true;
        }));

        if (!started) return;

        log.info("AI analysis: starting for {} ({} photo(s), provider={}, model={})",
                damageAssessmentId, photos.size(), aiProvider, aiModel);

        try {
            AiAssessmentResult result = assessmentProvider.analyze(photos);

            transactionTemplate.executeWithoutResult(status -> {
                DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElseThrow();
                a.setAiComment(result.comment());
                a.setAiConfidence(result.confidence());
                a.setAiModel(result.model());
                a.setAiAnalyzedAt(OffsetDateTime.now());
                a.setAiAnalysisStatus(AiAnalysisStatus.COMPLETED);
                a.setAiPriority(result.priority());
                a.setAiRecommendations(result.recommendations());
                a.setAiRiskScore(result.riskScore());
                a.setAiRetryCount(0);
                a.setAiNextRetryAt(null);
                damageAssessmentRepository.save(a);
            });

            log.info("AI analysis completed for {}: confidence={}, riskScore={}", damageAssessmentId,
                    result.confidence(), result.riskScore());

        } catch (Exception e) {
            log.error("AI analysis failed for {} — {}: {}", damageAssessmentId, e.getClass().getSimpleName(), e.getMessage(), e);
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElseThrow();
                    scheduleRetryOrFail(a, "Analiz sırasında bir hata oluştu.");
                    damageAssessmentRepository.save(a);
                });
            } catch (Exception saveEx) {
                log.error("Failed to persist AI failure status for {}: {}", damageAssessmentId, saveEx.getMessage());
            }
        }
    }

    private void scheduleRetryOrFail(DamageAssessment a, String errorComment) {
        int retryCount = a.getAiRetryCount() == null ? 0 : a.getAiRetryCount();
        if (retryCount >= MAX_RETRIES) {
            a.setAiAnalysisStatus(AiAnalysisStatus.FAILED);
            a.setAiComment(errorComment + " (maksimum yeniden deneme sayısına ulaşıldı)");
            a.setAiNextRetryAt(null);
            log.warn("AI analysis: {} permanently FAILED after {} retries", a.getId(), retryCount);
        } else {
            long delayMinutes = RETRY_DELAYS_MINUTES[retryCount];
            a.setAiRetryCount(retryCount + 1);
            a.setAiAnalysisStatus(AiAnalysisStatus.PENDING);
            a.setAiNextRetryAt(OffsetDateTime.now().plusMinutes(delayMinutes));
            log.info("AI analysis: {} scheduled for retry #{} in {} min(s)", a.getId(), retryCount + 1, delayMinutes);
        }
    }

    private String computeHash(DamageAssessment a) {
        try {
            String photoKeys = a.getPhotos().stream()
                    .filter(p -> p.getPhotoType() == PhotoType.REPORTER_PHOTO
                            || p.getPhotoType() == PhotoType.ASSIGNEE_FIELD_PHOTO)
                    .map(p -> p.getStorageKey())
                    .sorted()
                    .reduce("", (s1, s2) -> s1 + "|" + s2);

            String data = a.getAddress() + "|"
                    + a.getDamageLevel().name() + "|"
                    + a.isCollapseRisk() + "|"
                    + a.isCasualtiesSuspected() + "|"
                    + a.isEmergencyEvacuationNeeded() + "|"
                    + a.isGasLeakRisk() + "|"
                    + photoKeys;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "hash-error-" + System.currentTimeMillis();
        }
    }

    private String resolveApiKey() {
        if ("openai".equalsIgnoreCase(aiProvider)) {
            return openaiApiKey;
        }
        return claudeApiKey;
    }

    private List<PhotoData> collectPhotos(DamageAssessment assessment) {
        List<PhotoData> result = new ArrayList<>();

        var reporterPhotos = assessment.getPhotos().stream()
                .filter(p -> p.getPhotoType() == PhotoType.REPORTER_PHOTO)
                .toList();

        for (var photo : reporterPhotos) {
            if (result.size() >= maxPhotos) break;
            PhotoData data = toPhotoData(photo.getStorageKey(), photo.getMimeType());
            if (data != null) result.add(data);
        }

        if (result.size() < maxPhotos) {
            var fieldPhotos = assessment.getPhotos().stream()
                    .filter(p -> p.getPhotoType() == PhotoType.ASSIGNEE_FIELD_PHOTO)
                    .toList();
            for (var photo : fieldPhotos) {
                if (result.size() >= maxPhotos) break;
                PhotoData data = toPhotoData(photo.getStorageKey(), photo.getMimeType());
                if (data != null) result.add(data);
            }
        }

        return result;
    }

    private PhotoData toPhotoData(String storageKey, String mimeType) {
        try {
            Path path = Paths.get(localStoragePath, storageKey);
            if (!Files.exists(path)) {
                log.warn("AI analysis: photo file not found at {}", path);
                return null;
            }
            byte[] bytes = Files.readAllBytes(path);
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return new PhotoData(base64, normalizeMime(mimeType));
        } catch (Exception e) {
            log.warn("AI analysis: failed to encode photo {}: {}", storageKey, e.getMessage());
            return null;
        }
    }

    private String normalizeMime(String mimeType) {
        if (mimeType == null) return "image/jpeg";
        return switch (mimeType.toLowerCase()) {
            case "image/jpg" -> "image/jpeg";
            case "image/jpeg", "image/png", "image/webp", "image/gif" -> mimeType.toLowerCase();
            default -> "image/jpeg";
        };
    }
}
