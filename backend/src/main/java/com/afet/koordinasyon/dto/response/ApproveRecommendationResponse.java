package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApproveRecommendationResponse {
    private TeamRecommendationResponse recommendation;
    private List<EventAssignmentResponse> assignments;
    private int mailSentCount;
    private int mailFailedCount;
    private String mailNote;
}
