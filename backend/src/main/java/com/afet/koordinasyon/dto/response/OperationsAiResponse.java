package com.afet.koordinasyon.dto.response;

import java.time.Instant;

public record OperationsAiResponse(
        String answer,
        Instant generatedAt
) {}
