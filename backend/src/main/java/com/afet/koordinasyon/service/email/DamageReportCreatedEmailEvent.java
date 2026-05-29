package com.afet.koordinasyon.service.email;

import java.util.UUID;

public record DamageReportCreatedEmailEvent(UUID damageAssessmentId, UUID reporterId) {}
