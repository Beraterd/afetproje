package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ApproveRecommendationRequest(
        @NotEmpty List<UUID> selectedUserIds
) {}
