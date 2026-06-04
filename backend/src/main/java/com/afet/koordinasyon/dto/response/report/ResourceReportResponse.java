package com.afet.koordinasyon.dto.response.report;

import lombok.Builder;

/** Kaynak Talep Raporu — yalnızca kaynak talepleri. */
@Builder
public record ResourceReportResponse(
        ReportMeta meta,
        ReportModules.ResourceModule resource,
        String managerSummary
) {}
