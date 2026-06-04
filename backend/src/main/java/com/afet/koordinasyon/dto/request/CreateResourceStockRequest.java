package com.afet.koordinasyon.dto.request;

import com.afet.koordinasyon.domain.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateResourceStockRequest {

    /** Ürün adı — bazı kategorilerde zorunlu, bazılarında kategori label'ından türetilir (servis doğrular). */
    private String name;

    @NotNull
    private ResourceType category;

    @NotNull
    @PositiveOrZero
    private Integer quantity;

    @NotBlank
    private String unit;

    private Double dailyUsageEstimate;

    @PositiveOrZero
    private Integer criticalThreshold;

    @NotNull
    private UUID districtId;

    @NotNull(message = "Mahalle seçimi zorunludur")
    private UUID neighborhoodId;

    private String notes;
}
