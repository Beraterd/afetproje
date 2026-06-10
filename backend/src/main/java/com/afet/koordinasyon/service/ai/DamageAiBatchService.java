package com.afet.koordinasyon.service.ai;

import com.afet.koordinasyon.domain.entity.DamageAssessment;
import com.afet.koordinasyon.domain.enums.AiAnalysisStatus;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handles startup batch scan and periodic retry of failed/pending AI analyses.
 * On startup: enqueues all records with no AI result yet.
 * Every 5 minutes: enqueues records that are PENDING and whose retry window has elapsed.
 * Every 15 minutes: resets stale PROCESSING records (crash recovery).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DamageAiBatchService {

    private final DamageAssessmentRepository repository;
    private final DamageAiQueueService queueService;

    /** Called once after application context is fully started. */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("AI batch: startup scan — enqueuing records without completed analysis");
        enqueueAllMissing();
    }

    /**
     * Enqueue all records that need analysis (null status or PENDING without retry time).
     * Called on startup and from the /ai/enqueue-missing endpoint.
     */
    @Transactional(readOnly = true)
    public int enqueueAllMissing() {
        List<UUID> ids = repository.findIdsNeedingAiAnalysis(AiAnalysisStatus.PENDING);
        for (UUID id : ids) {
            repository.findById(id).ifPresent(a ->
                    queueService.enqueue(a.getId(), a.getDamageLevel()));
        }
        if (!ids.isEmpty()) {
            log.info("AI batch: enqueued {} record(s) for analysis", ids.size());
        }
        return ids.size();
    }

    /** Every 5 minutes: enqueue PENDING records whose retry window has elapsed. */
    @Scheduled(fixedDelay = 300_000)
    @Transactional(readOnly = true)
    public void retryEligible() {
        List<UUID> ids = repository.findIdsReadyForRetry(AiAnalysisStatus.PENDING, OffsetDateTime.now());
        for (UUID id : ids) {
            repository.findById(id).ifPresent(a ->
                    queueService.enqueue(a.getId(), a.getDamageLevel()));
        }
        if (!ids.isEmpty()) {
            log.info("AI batch retry: enqueued {} record(s)", ids.size());
        }
    }

    /** Every 15 minutes: reset PROCESSING records stuck for > 10 minutes (crash recovery). */
    @Scheduled(fixedDelay = 900_000)
    public void recoverStaleProcessing() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(10);
        List<UUID> stale = repository.findStaleProcessing(AiAnalysisStatus.PROCESSING, threshold);
        for (UUID id : stale) {
            repository.findById(id).ifPresent(a -> {
                a.setAiAnalysisStatus(AiAnalysisStatus.PENDING);
                repository.save(a);
                queueService.enqueue(a.getId(), a.getDamageLevel());
                log.warn("AI batch: reset stale PROCESSING record {} to PENDING", id);
            });
        }
    }
}
