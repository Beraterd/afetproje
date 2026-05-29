package com.afet.koordinasyon.service.email;

import java.util.UUID;

public record DamageReportFieldTeamAssignedEmailEvent(UUID damageAssessmentId, UUID assigneeId) {}
