package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateSimulationRequest {
    @NotNull
    private UUID districtId;
    @NotNull
    @DecimalMin("0.1")
    @DecimalMax("10.0")
    private BigDecimal magnitude;
    private String notes;
}
