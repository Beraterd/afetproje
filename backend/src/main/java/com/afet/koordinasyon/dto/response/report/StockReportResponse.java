package com.afet.koordinasyon.dto.response.report;

import lombok.Builder;

import java.util.List;

/** Stok Raporu — yalnızca stok ve stok hareketleri. */
@Builder
public record StockReportResponse(
        ReportMeta meta,
        ReportModules.StockModule stock,
        List<String> supplyRecommendations,
        String managerSummary
) {}
