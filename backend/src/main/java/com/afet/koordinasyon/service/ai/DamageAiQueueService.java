package com.afet.koordinasyon.service.ai;

import com.afet.koordinasyon.domain.enums.DamageLevel;
import com.afet.koordinasyon.service.DamageAssessmentAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Priority-ordered queue for background AI damage assessment jobs.
 * Heavy/Collapsed buildings are processed first (priority 0),
 * then Moderate (1), then Light/Unassessed (2).
 *
 * DamageAssessmentAiService.generateAiAssessment() is @Async("aiTaskExecutor"),
 * so drain() dispatches jobs quickly without blocking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DamageAiQueueService {

    private final DamageAssessmentAiService aiService;

    private final PriorityBlockingQueue<DamageAiJob> queue = new PriorityBlockingQueue<>();
    private final Set<UUID> inQueue = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Returns the queue depth for monitoring. */
    public int queueSize() {
        return queue.size();
    }

    /**
     * Add a job to the priority queue. Duplicate IDs are silently ignored
     * (the in-flight set prevents spawning two concurrent analyses for the same record).
     */
    public void enqueue(UUID damageId, DamageLevel level) {
        if (!inQueue.add(damageId)) {
            log.debug("AI queue: {} already queued — skipping duplicate", damageId);
            return;
        }
        int priority = jobPriority(level);
        queue.offer(new DamageAiJob(damageId, priority, System.currentTimeMillis()));
        log.debug("AI queue: enqueued {} (priority={}, queueSize={})", damageId, priority, queue.size());
    }

    /**
     * Drain up to 30 jobs per tick. Each dispatches to aiTaskExecutor asynchronously.
     * Fixed delay keeps ticks from stacking if processing is slow.
     */
    @Scheduled(fixedDelay = 500)
    public void drainQueue() {
        int dispatched = 0;
        DamageAiJob job;
        while (dispatched < 30 && (job = queue.poll()) != null) {
            inQueue.remove(job.damageId());
            aiService.generateAiAssessment(job.damageId());
            dispatched++;
        }
        if (dispatched > 0) {
            log.debug("AI queue drain: dispatched {} job(s), remaining={}", dispatched, queue.size());
        }
    }

    private static int jobPriority(DamageLevel level) {
        if (level == null) return 2;
        return switch (level) {
            case HEAVY, COLLAPSED -> 0;
            case MODERATE -> 1;
            default -> 2;
        };
    }
}
