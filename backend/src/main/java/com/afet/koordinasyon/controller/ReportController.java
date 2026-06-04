package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.domain.enums.ReportType;
import com.afet.koordinasyon.dto.response.DailyReportResponse;
import com.afet.koordinasyon.dto.response.report.*;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.AdvancedReportExportService;
import com.afet.koordinasyon.service.AdvancedReportService;
import com.afet.koordinasyon.service.AuditLogService;
import com.afet.koordinasyon.service.ReportExportService;
import com.afet.koordinasyon.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Yönetim raporları (rol bazlı kapsam) — analiz, PDF/Excel export")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;
    private final AuditLogService auditLogService;
    private final AdvancedReportService advancedReportService;
    private final AdvancedReportExportService advancedReportExportService;

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final String REPORT_ROLES = "hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')";

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Profesyonel yönetim raporu (rol bazlı: sistem/ilçe/mahalle)")
    public ResponseEntity<DailyReportResponse> daily(
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reportService.generateDaily(districtId, neighborhoodId, principal));
    }

    @GetMapping("/daily/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Yönetim raporunu kurumsal PDF olarak indir")
    public ResponseEntity<byte[]> dailyPdf(
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        DailyReportResponse report = reportService.generateDaily(districtId, neighborhoodId, principal);
        byte[] pdf = reportExportService.toPdf(report);
        auditExport(principal, "PDF", districtId, neighborhoodId);
        return fileResponse(pdf, MediaType.APPLICATION_PDF, fileName("pdf"));
    }

    @GetMapping("/daily/excel")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Yönetim raporunu çok-sayfalı Excel olarak indir")
    public ResponseEntity<byte[]> dailyExcel(
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        DailyReportResponse report = reportService.generateDaily(districtId, neighborhoodId, principal);
        byte[] xlsx = reportExportService.toExcel(report);
        auditExport(principal, "EXCEL", districtId, neighborhoodId);
        return fileResponse(xlsx,
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                fileName("xlsx"));
    }

    // ── Yeni rapor türleri (rol bazlı ilçe/mahalle kapsamı backend'de zorlanır) ──

    @GetMapping("/comprehensive")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Kapsamlı rapor (type=DAILY|WEEKLY|MONTHLY) — tüm modüller + yönetici özeti")
    public ResponseEntity<ComprehensiveReportResponse> comprehensive(
            @RequestParam ReportType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                advancedReportService.generateComprehensive(type, date, districtId, neighborhoodId, principal));
    }

    @GetMapping("/operation")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Operasyon / Ekip Raporu")
    public ResponseEntity<OperationReportResponse> operation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(advancedReportService.generateOperation(date, districtId, neighborhoodId, principal));
    }

    @GetMapping("/resource")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Kaynak Talep Raporu")
    public ResponseEntity<ResourceReportResponse> resource(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(advancedReportService.generateResource(date, districtId, neighborhoodId, principal));
    }

    @GetMapping("/stock")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Stok Raporu")
    public ResponseEntity<StockReportResponse> stock(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(advancedReportService.generateStock(date, districtId, neighborhoodId, principal));
    }

    @GetMapping("/damage")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Hasar Raporu")
    public ResponseEntity<DamageReportResponse> damage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(advancedReportService.generateDamage(date, districtId, neighborhoodId, principal));
    }

    @GetMapping("/volunteer")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Gönüllü Raporu")
    public ResponseEntity<VolunteerReportResponse> volunteer(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(advancedReportService.generateVolunteer(date, districtId, neighborhoodId, principal));
    }

    // ── Yeni rapor türleri için PDF / Excel export ──────────────────────────────

    @GetMapping("/comprehensive/pdf")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Kapsamlı raporu PDF indir")
    public ResponseEntity<byte[]> comprehensivePdf(@RequestParam ReportType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateComprehensive(type, date, districtId, neighborhoodId, principal);
        byte[] pdf = advancedReportExportService.comprehensivePdf(report);
        auditExport(principal, "PDF", districtId, neighborhoodId);
        return fileResponse(pdf, MediaType.APPLICATION_PDF, typeFileName(type.name(), "pdf"));
    }

    @GetMapping("/comprehensive/excel")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Kapsamlı raporu Excel indir")
    public ResponseEntity<byte[]> comprehensiveExcel(@RequestParam ReportType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateComprehensive(type, date, districtId, neighborhoodId, principal);
        byte[] xlsx = advancedReportExportService.comprehensiveExcel(report);
        auditExport(principal, "EXCEL", districtId, neighborhoodId);
        return fileResponse(xlsx, XLSX, typeFileName(type.name(), "xlsx"));
    }

    @GetMapping("/operation/pdf")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Operasyon raporunu PDF indir")
    public ResponseEntity<byte[]> operationPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateOperation(date, districtId, neighborhoodId, principal);
        auditExport(principal, "PDF", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.operationPdf(report), MediaType.APPLICATION_PDF, typeFileName("OPERATION", "pdf"));
    }

    @GetMapping("/operation/excel")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Operasyon raporunu Excel indir")
    public ResponseEntity<byte[]> operationExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateOperation(date, districtId, neighborhoodId, principal);
        auditExport(principal, "EXCEL", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.operationExcel(report), XLSX, typeFileName("OPERATION", "xlsx"));
    }

    @GetMapping("/resource/pdf")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Kaynak raporunu PDF indir")
    public ResponseEntity<byte[]> resourcePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateResource(date, districtId, neighborhoodId, principal);
        auditExport(principal, "PDF", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.resourcePdf(report), MediaType.APPLICATION_PDF, typeFileName("RESOURCE", "pdf"));
    }

    @GetMapping("/resource/excel")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Kaynak raporunu Excel indir")
    public ResponseEntity<byte[]> resourceExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateResource(date, districtId, neighborhoodId, principal);
        auditExport(principal, "EXCEL", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.resourceExcel(report), XLSX, typeFileName("RESOURCE", "xlsx"));
    }

    @GetMapping("/stock/pdf")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Stok raporunu PDF indir")
    public ResponseEntity<byte[]> stockPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateStock(date, districtId, neighborhoodId, principal);
        auditExport(principal, "PDF", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.stockPdf(report), MediaType.APPLICATION_PDF, typeFileName("STOCK", "pdf"));
    }

    @GetMapping("/stock/excel")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Stok raporunu Excel indir")
    public ResponseEntity<byte[]> stockExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateStock(date, districtId, neighborhoodId, principal);
        auditExport(principal, "EXCEL", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.stockExcel(report), XLSX, typeFileName("STOCK", "xlsx"));
    }

    @GetMapping("/damage/pdf")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Hasar raporunu PDF indir")
    public ResponseEntity<byte[]> damagePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateDamage(date, districtId, neighborhoodId, principal);
        auditExport(principal, "PDF", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.damagePdf(report), MediaType.APPLICATION_PDF, typeFileName("DAMAGE", "pdf"));
    }

    @GetMapping("/damage/excel")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Hasar raporunu Excel indir")
    public ResponseEntity<byte[]> damageExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateDamage(date, districtId, neighborhoodId, principal);
        auditExport(principal, "EXCEL", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.damageExcel(report), XLSX, typeFileName("DAMAGE", "xlsx"));
    }

    @GetMapping("/volunteer/pdf")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Gönüllü raporunu PDF indir")
    public ResponseEntity<byte[]> volunteerPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateVolunteer(date, districtId, neighborhoodId, principal);
        auditExport(principal, "PDF", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.volunteerPdf(report), MediaType.APPLICATION_PDF, typeFileName("VOLUNTEER", "pdf"));
    }

    @GetMapping("/volunteer/excel")
    @PreAuthorize(REPORT_ROLES)
    @Operation(summary = "Gönüllü raporunu Excel indir")
    public ResponseEntity<byte[]> volunteerExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID districtId, @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var report = advancedReportService.generateVolunteer(date, districtId, neighborhoodId, principal);
        auditExport(principal, "EXCEL", districtId, neighborhoodId);
        return fileResponse(advancedReportExportService.volunteerExcel(report), XLSX, typeFileName("VOLUNTEER", "xlsx"));
    }

    private String typeFileName(String type, String ext) {
        return "afet-rapor-" + type.toLowerCase() + "-" + LocalDate.now() + "." + ext;
    }

    private void auditExport(UserPrincipal principal, String format, UUID districtId, UUID neighborhoodId) {
        try {
            auditLogService.logUserAction(principal, AuditActionType.REPORT_EXPORTED, "REPORT", null,
                    "Yönetim raporu " + format + " olarak dışa aktarıldı",
                    Map.of("format", format,
                            "districtId", String.valueOf(districtId),
                            "neighborhoodId", String.valueOf(neighborhoodId)));
        } catch (Exception e) {
            log.warn("Rapor export audit log kaydı başarısız: {}", e.getMessage());
        }
    }

    private String fileName(String ext) {
        return "afet-yonetim-raporu-" + LocalDate.now() + "." + ext;
    }

    private ResponseEntity<byte[]> fileResponse(byte[] body, MediaType type, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(type);
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(body.length);
        return new ResponseEntity<>(body, headers, 200);
    }
}
