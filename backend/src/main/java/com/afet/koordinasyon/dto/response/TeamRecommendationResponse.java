package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TeamRecommendationResponse {
    private UUID id;
    private UUID eventId;
    private String teamType;
    private String teamTypeLabel;
    private String districtName;
    private String neighborhoodName;
    private int requiredTeamSize;
    private String priority;
    private String status;
    private String aiExplanation;
    private List<RecommendedMemberResponse> recommendedPersonnel;
    private String requestedByName;
    private String approvedByName;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
}
