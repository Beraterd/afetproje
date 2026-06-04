package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ResourceRequestResponse {
    private UUID id;
    private UUID districtId;
    private String districtName;
    private UUID neighborhoodId;
    private String neighborhoodName;
    private String requestScope;
    private String resourceType;
    private String resourceTypeLabel;
    /** Ürün adı: özel girildiyse o, yoksa kategori adı. */
    private String productName;
    private Integer quantity;
    private String unit;
    private String priority;
    private String priorityLabel;
    private String status;
    private String statusLabel;
    private String description;
    private String createdBy;
    private String closedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
}
