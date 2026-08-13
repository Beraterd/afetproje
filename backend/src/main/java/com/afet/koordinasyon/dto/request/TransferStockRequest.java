package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class TransferStockRequest {

    @NotNull
    private UUID sourceStockId;

    @NotNull
    private UUID targetStockId;

    @NotNull
    @Positive
    private Integer quantity;

    private String reason;
}
