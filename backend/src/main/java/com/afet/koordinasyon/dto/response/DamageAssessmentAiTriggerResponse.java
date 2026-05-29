package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DamageAssessmentAiTriggerResponse {
    private UUID assessmentId;
    private String aiAnalysisStatus;
}
