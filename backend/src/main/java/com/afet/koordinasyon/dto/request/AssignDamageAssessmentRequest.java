package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignDamageAssessmentRequest {

    @NotNull
    private UUID userId;
}
