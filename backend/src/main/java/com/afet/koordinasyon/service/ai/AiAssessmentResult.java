package com.afet.koordinasyon.service.ai;

import com.afet.koordinasyon.domain.enums.AiConfidence;

public record AiAssessmentResult(
        String comment,
        AiConfidence confidence,
        String model,
        Integer priority,
        String recommendations,
        Integer riskScore
) {}
