package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Toplanma alanı kapsama raporu.
 * GET /api/admin/assembly-areas/coverage endpoint'i tarafından döndürülür.
 */
@Getter
@Builder
public class AssemblyCoverageResponse {

    /** İstanbul geneli özet */
    private int totalNeighborhoods;
    private int coveredNeighborhoods;
    private int uncoveredNeighborhoods;
    private double coveragePct;

    /** İlçe bazında detay */
    private List<DistrictCoverage> districts;

    @Getter
    @Builder
    public static class DistrictCoverage {
        private UUID districtId;
        private String districtName;
        private int totalNeighborhoods;
        private int coveredNeighborhoods;
        private double coveragePct;
        /** Toplanma alanı henüz tanımlanmamış mahalle adları */
        private List<String> uncoveredNeighborhoodNames;
    }
}
