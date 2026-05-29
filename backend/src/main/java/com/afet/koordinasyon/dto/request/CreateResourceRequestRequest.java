package com.afet.koordinasyon.dto.request;

import com.afet.koordinasyon.domain.enums.ResourceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateResourceRequestRequest {

    @NotNull
    private UUID districtId;

    private UUID neighborhoodId;

    @NotNull
    private ResourceType resourceType;

    private Integer quantity;

    private String description;
}
