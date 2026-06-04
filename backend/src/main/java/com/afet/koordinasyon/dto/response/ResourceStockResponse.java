package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ResourceStockResponse {
    private UUID id;
    private String name;
    private String category;
    private String categoryLabel;
    private int quantity;
    private String unit;
    private Double dailyUsageEstimate;
    private int criticalThreshold;
    /** Kaç günlük yeterli olduğu; hesaplanamıyorsa null. */
    private Double daysRemaining;
    private String status;
    private String statusLabel;
    private UUID districtId;
    private String districtName;
    private UUID neighborhoodId;
    private String neighborhoodName;
    private String warehouseName;
    private String notes;
    private boolean active;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
