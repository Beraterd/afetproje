package com.afet.koordinasyon.dto.response.report;

import lombok.Builder;

import java.util.List;

/**
 * Rapor modülleri için paylaşılan iç içe record'lar. Kapsamlı (günlük/haftalık/aylık) ve özel
 * raporlar bu modülleri yeniden kullanır.
 *
 * <p>Veri kaynağı bulunmayan alanlar {@code null} (Long/Double/Integer) bırakılır; frontend bunları
 * "Veri bulunamadı" olarak gösterir. Kesin sayımlar {@code long} olarak 0 döner.
 */
public final class ReportModules {

    private ReportModules() {}

    /** Genel amaçlı kategori/etiket sayımı (kategori dağılımı, ekip kırılımı vb.). */
    public record CategoryCount(String code, String label, long count) {}

    /** Mahalle bazlı tekil metrik (dağılım/yoğunluk tabloları için). */
    public record NeighborhoodMetric(String neighborhoodId, String neighborhoodName, String districtName, long value) {
        /** Görüntüleme etiketi: "Mahalle - İlçe" veya sadece mahalle adı. */
        public String displayLabel() {
            return (districtName != null && !districtName.isBlank())
                    ? neighborhoodName + " - " + districtName
                    : neighborhoodName;
        }
    }

    /** Ekip / Operasyon modülü. */
    @Builder
    public record TeamModule(
            long totalTeams, long activeTeams, long availableTeams,
            long totalTasks, long completedTasks, long inProgressTasks, long openTasks,
            long requiredPeople, long shortage,
            Long avgResponseMinutes, Long avgCompletionMinutes,
            List<CategoryCount> teamBreakdown) {}

    /** Hasar Tespiti modülü. */
    @Builder
    public record DamageModule(
            long total, long newInPeriod,
            long pendingReview, long fieldVerified, long coordinatorApproved,
            long assignedTeam, long teamNeeded, long unassigned,
            long lightRisk, long moderateRisk, long heavyRisk, long criticalRisk,
            Long avgReviewMinutes,
            List<NeighborhoodMetric> topNeighborhoods) {}

    /** Kaynak Talepleri modülü. */
    @Builder
    public record ResourceModule(
            long total, long fulfilled, long pending, long rejected, long partial,
            double fulfillmentRate, Long avgResolutionMinutes,
            long criticalPending,
            List<CategoryCount> topCategories,
            List<NeighborhoodMetric> topNeighborhoods) {}

    /** Stok modülü. */
    @Builder
    public record StockModule(
            long totalItems, long totalQuantity, long availableQuantity,
            long criticalItems, long outOfStockItems,
            long periodInflow, long periodOutflow,
            Integer estimatedDaysRemaining,
            List<CategoryCount> categoryDistribution,
            List<NeighborhoodMetric> neighborhoodDistribution) {}

    /** Belge Onayları modülü. */
    @Builder
    public record DocumentModule(
            long uploaded, long approved, long pending, long rejected,
            List<CategoryCount> typeDistribution) {}

    /** Koordinatör Atamaları modülü. */
    @Builder
    public record CoordinatorModule(
            long assignmentsInPeriod, long districtCoordinators, long neighborhoodCoordinators,
            Long pendingProcesses) {}

    /** Gönüllü modülü. */
    @Builder
    public record VolunteerModule(
            long total, long newInPeriod, long approved, long pending,
            long active, long passive, long assigned,
            Long rejected, Long trained, Long capacityGap,
            List<NeighborhoodMetric> neighborhoodDistribution) {}
}
