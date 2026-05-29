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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DamageAssessmentAiService {

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
     * Each DB operation runs in its own REQUIRES_NEW transaction so that PROCESSING
     * is committed and visible to pollers before the AI call starts (which can take 4-10 s).
     * Without this split, the single @Transactional would hold the TX open for the whole
     * AI call and the DB would jump from NOT_STARTED straight to COMPLETED — making the
     * frontend's first poll always see a stale NOT_STARTED and appear to miss the result.
     */
    @Async
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
            log.error("AI analysis skipped: unsupported provider '{}' for {}. " +
                    "Supported values: claude, openai", aiProvider, damageAssessmentId);
            return;
        }

        // Step 1: Set PROCESSING and commit immediately so pollers can observe it.
        boolean started = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElse(null);
            if (a == null) {
                log.warn("AI analysis: assessment {} not found in DB", damageAssessmentId);
                return false;
            }
            if (a.getAiAnalysisStatus() == AiAnalysisStatus.PROCESSING) {
                log.info("AI analysis: {} is already PROCESSING — skipping duplicate job", damageAssessmentId);
                return false;
            }
            a.setAiAnalysisStatus(AiAnalysisStatus.PROCESSING);
            damageAssessmentRepository.save(a);
            return true;
        }));

        if (!started) return;

        // Step 2: Load photos within a fresh transaction (lazy collection needs an active TX).
        List<PhotoData> photos = transactionTemplate.execute(status -> {
            DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElse(null);
            if (a == null) return new ArrayList<>();
            return collectPhotos(a);
        });

        if (photos == null || photos.isEmpty()) {
            log.warn("AI analysis: no readable photos found for assessment {}", damageAssessmentId);
            transactionTemplate.executeWithoutResult(status -> {
                DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElseThrow();
                a.setAiAnalysisStatus(AiAnalysisStatus.FAILED);
                a.setAiComment("Analiz için erişilebilir fotoğraf bulunamadı.");
                damageAssessmentRepository.save(a);
            });
            return;
        }

        log.info("AI analysis: starting for {} ({} photo(s), provider={}, model={})",
                damageAssessmentId, photos.size(), aiProvider, aiModel);

        try {
            AiAssessmentResult result = assessmentProvider.analyze(photos);

            // Step 3: Commit COMPLETED in its own transaction.
            transactionTemplate.executeWithoutResult(status -> {
                DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElseThrow();
                a.setAiComment(result.comment());
                a.setAiConfidence(result.confidence());
                a.setAiModel(result.model());
                a.setAiAnalyzedAt(OffsetDateTime.now());
                a.setAiAnalysisStatus(AiAnalysisStatus.COMPLETED);
                damageAssessmentRepository.save(a);
            });

            log.info("AI analysis completed for {}: confidence={}", damageAssessmentId, result.confidence());

        } catch (Exception e) {
            log.error("AI analysis failed for {} — {}: {}", damageAssessmentId, e.getClass().getSimpleName(), e.getMessage(), e);
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    DamageAssessment a = damageAssessmentRepository.findById(damageAssessmentId).orElseThrow();
                    a.setAiAnalysisStatus(AiAnalysisStatus.FAILED);
                    a.setAiComment("Analiz sırasında bir hata oluştu.");
                    damageAssessmentRepository.save(a);
                });
            } catch (Exception saveEx) {
                log.error("Failed to persist AI failure status for {}: {}", damageAssessmentId, saveEx.getMessage());
            }
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
