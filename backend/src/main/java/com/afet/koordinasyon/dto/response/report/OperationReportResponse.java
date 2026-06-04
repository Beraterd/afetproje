package com.afet.koordinasyon.dto.response.report;

import lombok.Builder;

/** Operasyon / Ekip Raporu — yalnızca ekip ve operasyonel performans. */
@Builder
public record OperationReportResponse(
        ReportMeta meta,
        ReportModules.TeamModule team,
        String managerSummary
) {}
