package com.afet.koordinasyon.service.ai;

import java.util.UUID;

/**
 * Immutable job entry for the priority-based AI analysis queue.
 * Lower priority value = processed first (HEAVY/COLLAPSED → 0, MODERATE → 1, LIGHT/UNASSESSED → 2).
 */
public record DamageAiJob(UUID damageId, int priority, long enqueueTimeMs)
        implements Comparable<DamageAiJob> {

    @Override
    public int compareTo(DamageAiJob other) {
        int p = Integer.compare(this.priority, other.priority);
        return p != 0 ? p : Long.compare(this.enqueueTimeMs, other.enqueueTimeMs);
    }
}
