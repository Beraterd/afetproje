package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ResourceStockMovementResponse {
    private UUID id;
    private String movementType;
    private String movementTypeLabel;
    private int quantityChange;
    private int previousQuantity;
    private int newQuantity;
    private String reason;
    private String createdBy;
    private OffsetDateTime createdAt;
}
