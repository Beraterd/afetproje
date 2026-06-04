package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RecommendedMemberResponse {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private int score;
    private int orderId;
    private int completedSimilarTasks;
    private List<String> reasons;
    private String proximityLevel;
    private String availability;
    private String previousSimilarTask;
}
