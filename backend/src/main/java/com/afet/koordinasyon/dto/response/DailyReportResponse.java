package com.afet.koordinasyon.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * Afet Yönetim Günlük Faaliyet Raporu — rol bazlı kapsamla (sistem/ilçe/mahalle) üretilir.
 * Tüm istatistikler mevcut verilerden hesaplanır.
 */
@Builder
public record DailyReportResponse(
        Meta meta,
        ResourceStats resources,
        StockStats stock,
        TaskStats tasks,
        RequestStats requests,
        DamageStats damage,
        AssemblyStats assemblyAreas,
        VolunteerStats volunteers,
        CommunicationStats communication,
        List<String> kpis,
        // ── §9 Profesyonel karar destek bölümleri ─────────────────────────────
        ExecutiveSummary executiveSummary,
        RiskAnalysis riskAnalysis,
        OperationPerformance operationPerformance,
        List<NeighborhoodComparison> neighborhoodComparisons,
        TrendAnalysis trendAnalysis,
        String aiCommentary
) {
    @Builder
    public record Meta(
            String title, String date, String time,
            String districtName, String neighborhoodName,
            String generatedBy, String role, String scopeLabel) {}

    /** Kaynak Yönetimi. */
    @Builder
    public record ResourceStats(long total, long fulfilled, long pending, long rejected, double fulfillmentRate) {}

    /** Stok Durumu + kategori dağılımı + son 24 saat tüketim. */
    @Builder
    public record StockStats(long totalItems, long criticalItems, long outOfStockItems,
                             long last24hConsumed, List<CategoryCount> categoryDistribution) {}

    public record CategoryCount(String category, String label, long quantity) {}

    /** Görev ve Ekip Yönetimi. */
    @Builder
    public record TaskStats(long total, long completed, long inProgress, long open, long activeTeams) {}

    /** Talep Yönetimi. */
    @Builder
    public record RequestStats(long total, long open, long closed, long urgent, long critical) {}

    /** Hasar ve Durum Analizi. */
    @Builder
    public record DamageStats(long reported, long verified, long pending,
                              long light, long moderate, long heavy, long collapsed) {}

    /** Toplanma Alanları. */
    @Builder
    public record AssemblyStats(long total, long active, long totalCapacity) {}

    /** Gönüllü Yönetimi. */
    @Builder
    public record VolunteerStats(long total, long active, long assigned) {}

    /** Vatandaş İletişim Verileri (sistem bildirimleri). */
    @Builder
    public record CommunicationStats(long sent, long read, double readRate, long last24h) {}

    // ── §9 Genel Durum Özeti ──────────────────────────────────────────────────
    /** Sistemin mevcut durumunu özetleyen, otomatik üretilen madde listesi. */
    @Builder
    public record ExecutiveSummary(List<String> highlights) {}

    // ── §9 Risk Analizi ───────────────────────────────────────────────────────
    /**
     * Otomatik risk puanı (0–100) ve seviyesi (DÜŞÜK/ORTA/YÜKSEK/KRİTİK) + katkı veren faktörler.
     */
    @Builder
    public record RiskAnalysis(int score, String level,
                               List<RiskFactor> factors, List<String> warnings) {}

    /** Tek bir risk faktörü ve puana katkısı. */
    public record RiskFactor(String label, int points, String detail) {}

    // ── §9 Operasyon Performansı ──────────────────────────────────────────────
    @Builder
    public record OperationPerformance(
            double taskCompletionRate,        // görev tamamlama oranı (%)
            double requestFulfillmentRate,    // talep karşılama oranı (%)
            double distributionPerformance,   // kaynak dağıtım performansı (%)
            Long avgResponseMinutes,          // ortalama müdahale süresi (dk) — null = veri yok
            Long avgResolutionMinutes) {}     // ortalama talep çözüm süresi (dk) — null = veri yok

    // ── §9 Mahalle Karşılaştırmaları ──────────────────────────────────────────
    @Builder
    public record NeighborhoodComparison(
            String neighborhoodId, String neighborhoodName,
            long requestCount, long resourceUsage, long taskCount,
            long damageCount, int riskScore) {}

    // ── §9 Trend Analizi (bugün / son 7 gün / son 30 gün) ─────────────────────
    @Builder
    public record TrendAnalysis(TrendMetric requests, TrendMetric tasks,
                                TrendMetric resourceConsumption, TrendMetric stockChange) {}

    /**
     * Tek bir metriğin pencere değerleri ve yüzdesel değişimi.
     * changeRate = son 7 gün ile önceki 7 günün karşılaştırmasıdır (% ; null = referans yok).
     */
    public record TrendMetric(long today, long last7Days, long last30Days, Double changeRate) {}
}
