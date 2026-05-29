package com.afet.koordinasyon.dto.request;

import com.afet.koordinasyon.domain.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyDamageAssessmentRequest {
    @NotNull
    private VerificationStatus verificationStatus;
    private String note;
}
