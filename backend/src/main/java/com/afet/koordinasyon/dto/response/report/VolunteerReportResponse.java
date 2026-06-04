package com.afet.koordinasyon.dto.response.report;

import lombok.Builder;

/** Gönüllü Raporu — yalnızca gönüllüler. */
@Builder
public record VolunteerReportResponse(
        ReportMeta meta,
        ReportModules.VolunteerModule volunteer,
        String managerSummary
) {}
