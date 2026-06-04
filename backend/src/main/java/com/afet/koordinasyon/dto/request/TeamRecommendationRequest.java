package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TeamRecommendationRequest(
        @NotBlank String teamType,
        @NotNull UUID districtId,
        UUID neighborhoodId,
        @Min(1) int requiredTeamSize,
        String priority
) {}
