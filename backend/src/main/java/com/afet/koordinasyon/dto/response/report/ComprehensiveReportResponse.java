package com.afet.koordinasyon.dto.response.report;

import lombok.Builder;

import java.util.List;

/**
 * Günlük / Haftalık / Aylık kapsamlı rapor: tüm operasyonel modüller + yönetici özeti.
 */
@Builder
public record ComprehensiveReportResponse(
        ReportMeta meta,
        ReportModules.TeamModule team,
        ReportModules.DamageModule damage,
        ReportModules.ResourceModule resource,
        ReportModules.StockModule stock,
        ReportModules.DocumentModule document,
        ReportModules.CoordinatorModule coordinator,
        ReportModules.VolunteerModule volunteer,
        List<String> kpis,
        String managerSummary
) {}
