package com.afet.koordinasyon.dto.response;

import java.time.OffsetDateTime;

/**
 * Vardiya devir teslim özeti — tamamen deterministik sayılardan oluşur (AI yok).
 * "Aktif Operasyon / Kaynak / Son Gelişmeler / Bekleyen İşler" bölümleri.
 */
public record HandoffSummaryResponse(
        int hours,
        OffsetDateTime generatedAt,
        ActiveOperations activeOperations,
        ResourceSummary resource,
        RecentActivity recentActivity,
        PendingWork pendingWork
) {
    public record ActiveOperations(
            long openTeamNeeds,
            long inProgressEvents,
            long heavyDamagePendingVerification
    ) {}

    public record ResourceSummary(
            long criticalStockItems,
            long outOfStockItems,
            long openResourceRequests,
            long highPriorityResourceRequests
    ) {}

    public record RecentActivity(
            long eventsCompleted,
            long damageVerified,
            long teamsAssigned
    ) {}

    public record PendingWork(
            long pendingDocumentApprovals,
            long pendingDamageApprovals,
            long criticalStockItems,
            long openTeamNeeds
    ) {}
}
