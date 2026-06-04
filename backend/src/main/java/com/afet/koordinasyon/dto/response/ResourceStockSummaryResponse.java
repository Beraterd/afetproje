package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceStockSummaryResponse {
    /** Toplam (aktif) stok kalemi sayısı. */
    private int totalItems;
    /** Kritik seviyedeki stok sayısı. */
    private int criticalCount;
    /** Tükenmiş stok sayısı. */
    private int outOfStockCount;
    /** Yeterlilik hesaplanabilen stokların ortalama gün sayısı; yoksa null. */
    private Double averageDaysRemaining;
}
