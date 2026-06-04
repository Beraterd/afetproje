package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * Stok meta verisi güncellemesi. Miktar bu uçtan DEĞİŞTİRİLMEZ;
 * miktar için PATCH /quantity (hareket kaydı oluşturur) kullanılır.
 */
@Data
public class UpdateResourceStockRequest {

    private String name;

    private String unit;

    private Double dailyUsageEstimate;

    @PositiveOrZero
    private Integer criticalThreshold;

    private String warehouseName;

    private String notes;
}
