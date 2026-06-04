package com.afet.koordinasyon.dto.request;

import com.afet.koordinasyon.domain.enums.RequestPriority;
import com.afet.koordinasyon.domain.enums.ResourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateResourceRequestRequest {

    @NotNull
    private UUID districtId;

    @NotNull(message = "Mahalle seçimi zorunludur")
    private UUID neighborhoodId;

    @NotNull
    private ResourceType resourceType;

    /** Ürün adı — bazı kategorilerde zorunlu, diğerlerinde kategori adından türetilir (servis doğrular). */
    private String name;

    @NotNull(message = "Miktar zorunludur")
    @Positive
    private Integer quantity;

    /** Birim (adet, koli, litre...). */
    private String unit;

    /** Öncelik UI'dan kaldırıldı; gönderilmezse servis MEDIUM uygular. */
    private RequestPriority priority;

    private String description;
}
