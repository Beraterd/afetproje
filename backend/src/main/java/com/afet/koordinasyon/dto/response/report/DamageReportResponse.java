package com.afet.koordinasyon.dto.response.report;

import lombok.Builder;

/** Hasar Raporu — yalnızca hasar tespitleri. */
@Builder
public record DamageReportResponse(
        ReportMeta meta,
        ReportModules.DamageModule damage,
        String managerSummary
) {}
