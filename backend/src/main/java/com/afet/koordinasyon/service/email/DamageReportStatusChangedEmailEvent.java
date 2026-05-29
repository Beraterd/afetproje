package com.afet.koordinasyon.service.email;

import com.afet.koordinasyon.domain.enums.VerificationStatus;

import java.util.UUID;

public record DamageReportStatusChangedEmailEvent(UUID damageAssessmentId, VerificationStatus newStatus) {}
