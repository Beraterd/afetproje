package com.afet.koordinasyon.dto.request;

import com.afet.koordinasyon.domain.enums.ResourceStockMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * Stok miktarı güncellemesi. newQuantity zorunludur; movementType verilmezse
 * yeni-eski farkına göre INCREASE/DECREASE/CORRECTION otomatik belirlenir.
 */
@Data
public class UpdateStockQuantityRequest {

    @NotNull
    @PositiveOrZero
    private Integer newQuantity;

    private ResourceStockMovementType movementType;

    private String reason;
}
