package com.afet.koordinasyon.service;

import com.afet.koordinasyon.dto.response.report.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Faz 2 — 8 rapor türü (kapsamlı + özel) için kurumsal PDF (OpenPDF) ve çok-sheet Excel (POI) export.
 * Mevcut {@link ReportExportService} (eski /daily) korunur; bu servis yeni ReportMeta + ReportModules
 * yapısını render eder. Türkçe karakter için Cp1254 kodlama, her sayfada başlık + sayfa numarası.
 * Grafikler PDF'te oransal yatay çubuk tablolarıyla temsil edilir (ek bağımlılık yok).
 */
@Service
@Slf4j
public class AdvancedReportExportService {

    private static final Color BRAND = new Color(30, 58, 95);
    private static final Color BRAND_LIGHT = new Color(224, 231, 243);
    private static final Color ACCENT = new Color(190, 30, 45);
    private static final Color GRID = new Color(210, 215, 222);
    private static final Color BAR = new Color(37, 99, 235);
    private static final String ENC = "Cp1254";
    private static final String NO_DATA = "Veri bulunamadı";

    private record NameValue(String label, String value) {}
    private record NameNum(String label, double value) {}

    // ════════════════════════ PDF — public giriş ════════════════════════

    public byte[] comprehensivePdf(ComprehensiveReportResponse r) {
        return buildPdf(r.meta(), doc -> {
            kpiCards(doc, comprehensiveKpis(r));
            teamSection(doc, r.team());
            damageSection(doc, r.damage());
            resourceSection(doc, r.resource());
            stockSection(doc, r.stock());
            documentSection(doc, r.document());
            coordinatorSection(doc, r.coordinator());
            volunteerSection(doc, r.volunteer());
            managerSummary(doc, r.managerSummary());
        });
    }

    public byte[] operationPdf(OperationReportResponse r) {
        return buildPdf(r.meta(), doc -> {
            kpiCards(doc, teamKpis(r.team()));
            teamSection(doc, r.team());
            managerSummary(doc, r.managerSummary());
        });
    }

    public byte[] resourcePdf(ResourceReportResponse r) {
        return buildPdf(r.meta(), doc -> {
            kpiCards(doc, resourceKpis(r.resource()));
            resourceSection(doc, r.resource());
            managerSummary(doc, r.managerSummary());
        });
    }

    public byte[] stockPdf(StockReportResponse r) {
        return buildPdf(r.meta(), doc -> {
            kpiCards(doc, stockKpis(r.stock()));
            stockSection(doc, r.stock());
            if (r.supplyRecommendations() != null && !r.supplyRecommendations().isEmpty()) {
                sectionTitle(doc, "Acil Tedarik Önerileri");
                for (String s : r.supplyRecommendations()) bullet(doc, s);
            }
            managerSummary(doc, r.managerSummary());
        });
    }

    public byte[] damagePdf(DamageReportResponse r) {
        return buildPdf(r.meta(), doc -> {
            kpiCards(doc, damageKpis(r.damage()));
            damageSection(doc, r.damage());
            managerSummary(doc, r.managerSummary());
        });
    }

    public byte[] volunteerPdf(VolunteerReportResponse r) {
        return buildPdf(r.meta(), doc -> {
            kpiCards(doc, volunteerKpis(r.volunteer()));
            volunteerSection(doc, r.volunteer());
            managerSummary(doc, r.managerSummary());
        });
    }

    // ════════════════════════ Excel — public giriş ════════════════════════

    public byte[] comprehensiveExcel(ComprehensiveReportResponse r) {
        return buildExcel(wb -> {
            generalSheet(wb, r.meta(), comprehensiveKpis(r), r.managerSummary());
            teamSheet(wb, r.team());
            damageSheet(wb, r.damage());
            resourceSheet(wb, r.resource());
            stockSheet(wb, r.stock());
            documentSheet(wb, r.document());
            coordinatorSheet(wb, r.coordinator());
            volunteerSheet(wb, r.volunteer());
        });
    }

    public byte[] operationExcel(OperationReportResponse r) {
        return buildExcel(wb -> { generalSheet(wb, r.meta(), teamKpis(r.team()), r.managerSummary()); teamSheet(wb, r.team()); });
    }

    public byte[] resourceExcel(ResourceReportResponse r) {
        return buildExcel(wb -> { generalSheet(wb, r.meta(), resourceKpis(r.resource()), r.managerSummary()); resourceSheet(wb, r.resource()); });
    }

    public byte[] stockExcel(StockReportResponse r) {
        return buildExcel(wb -> { generalSheet(wb, r.meta(), stockKpis(r.stock()), r.managerSummary()); stockSheet(wb, r.stock()); });
    }

    public byte[] damageExcel(DamageReportResponse r) {
        return buildExcel(wb -> { generalSheet(wb, r.meta(), damageKpis(r.damage()), r.managerSummary()); damageSheet(wb, r.damage()); });
    }

    public byte[] volunteerExcel(VolunteerReportResponse r) {
        return buildExcel(wb -> { generalSheet(wb, r.meta(), volunteerKpis(r.volunteer()), r.managerSummary()); volunteerSheet(wb, r.volunteer()); });
    }

    // ════════════════════════ KPI üreticiler ════════════════════════

    private List<NameValue> comprehensiveKpis(ComprehensiveReportResponse r) {
        List<NameValue> k = new ArrayList<>();
        k.add(new NameValue("Toplam Görev", num(r.team().totalTasks())));
        k.add(new NameValue("Aktif Ekip", num(r.team().activeTeams())));
        k.add(new NameValue("Toplam Hasar", num(r.damage().total())));
        k.add(new NameValue("Onaylı Hasar", num(r.damage().coordinatorApproved())));
        k.add(new NameValue("Bekleyen Talep", num(r.resource().pending())));
        k.add(new NameValue("Kritik Stok", num(r.stock().criticalItems() + r.stock().outOfStockItems())));
        k.add(new NameValue("Toplam Gönüllü", num(r.volunteer().total())));
        k.add(new NameValue("Bekleyen Belge", num(r.document().pending())));
        return k;
    }

    private List<NameValue> teamKpis(ReportModules.TeamModule t) {
        return List.of(
                new NameValue("Toplam Ekip", num(t.totalTeams())),
                new NameValue("Aktif Ekip", num(t.activeTeams())),
                new NameValue("Müsait Ekip", num(t.availableTeams())),
                new NameValue("Toplam Görev", num(t.totalTasks())),
                new NameValue("Tamamlanan", num(t.completedTasks())),
                new NameValue("Ek Personel İhtiyacı", num(t.shortage())));
    }

    private List<NameValue> resourceKpis(ReportModules.ResourceModule r) {
        return List.of(
                new NameValue("Toplam Talep", num(r.total())),
                new NameValue("Karşılanan", num(r.fulfilled())),
                new NameValue("Bekleyen", num(r.pending())),
                new NameValue("Karşılama Oranı", pct(r.fulfillmentRate())),
                new NameValue("Kritik Bekleyen", num(r.criticalPending())));
    }

    private List<NameValue> stockKpis(ReportModules.StockModule s) {
        return List.of(
                new NameValue("Stok Kalemi", num(s.totalItems())),
                new NameValue("Toplam Miktar", num(s.totalQuantity())),
                new NameValue("Kritik Kalem", num(s.criticalItems())),
                new NameValue("Tükenen Kalem", num(s.outOfStockItems())),
                new NameValue("Tahmini Gün", s.estimatedDaysRemaining() != null ? s.estimatedDaysRemaining() + " gün" : NO_DATA));
    }

    private List<NameValue> damageKpis(ReportModules.DamageModule d) {
        return List.of(
                new NameValue("Toplam Hasar", num(d.total())),
                new NameValue("Yeni (dönem)", num(d.newInPeriod())),
                new NameValue("Saha Doğrulanan", num(d.fieldVerified())),
                new NameValue("Koordinatör Onaylı", num(d.coordinatorApproved())),
                new NameValue("Ekip İhtiyacı", num(d.teamNeeded())),
                new NameValue("Kritik (Yıkık)", num(d.criticalRisk())));
    }

    private List<NameValue> volunteerKpis(ReportModules.VolunteerModule v) {
        return List.of(
                new NameValue("Toplam Gönüllü", num(v.total())),
                new NameValue("Yeni (dönem)", num(v.newInPeriod())),
                new NameValue("Aktif", num(v.active())),
                new NameValue("Pasif", num(v.passive())),
                new NameValue("Göreve Atanmış", num(v.assigned())));
    }

    // ════════════════════════ PDF — bölümler ════════════════════════

    private void teamSection(Document doc, ReportModules.TeamModule t) {
        sectionTitle(doc, "Ekip / Operasyon Durumu");
        PdfPTable kv = kvTable();
        kv(kv, "Toplam Ekip", num(t.totalTeams()));
        kv(kv, "Aktif Ekip", num(t.activeTeams()));
        kv(kv, "Müsait Ekip", num(t.availableTeams()));
        kv(kv, "Toplam Görev", num(t.totalTasks()));
        kv(kv, "Tamamlanan Görev", num(t.completedTasks()));
        kv(kv, "Devam Eden Görev", num(t.inProgressTasks()));
        kv(kv, "Açık Görev", num(t.openTasks()));
        kv(kv, "Gereken Personel", num(t.requiredPeople()));
        kv(kv, "Ek Personel İhtiyacı", num(t.shortage()));
        kv(kv, "Ort. Görev Tamamlama", minutes(t.avgCompletionMinutes()));
        kv(kv, "Ort. Müdahale Süresi", minutes(t.avgResponseMinutes()));
        doc.add(kv);
        barChart(doc, "Ekip Görev Durumu",
                List.of(new NameNum("Tamamlanan", t.completedTasks()),
                        new NameNum("Devam Eden", t.inProgressTasks()),
                        new NameNum("Açık", t.openTasks())));
        if (t.teamBreakdown() != null && !t.teamBreakdown().isEmpty()) {
            barChart(doc, "Ekip Tipine Göre Görev",
                    t.teamBreakdown().stream().map(c -> new NameNum(c.label(), c.count())).toList());
        }
    }

    private void damageSection(Document doc, ReportModules.DamageModule d) {
        sectionTitle(doc, "Hasar Tespiti");
        PdfPTable kv = kvTable();
        kv(kv, "Toplam Hasar", num(d.total()));
        kv(kv, "Dönemde Yeni", num(d.newInPeriod()));
        kv(kv, "İnceleme Bekleyen", num(d.pendingReview()));
        kv(kv, "Sahada Doğrulanan", num(d.fieldVerified()));
        kv(kv, "Koordinatör Onaylı", num(d.coordinatorApproved()));
        kv(kv, "Ekip Gönderilen", num(d.assignedTeam()));
        kv(kv, "Ekip İhtiyacı", num(d.teamNeeded()));
        kv(kv, "Ort. İnceleme Süresi", minutes(d.avgReviewMinutes()));
        doc.add(kv);
        barChart(doc, "Risk Seviyesine Göre Dağılım",
                List.of(new NameNum("Düşük", d.lightRisk()), new NameNum("Orta", d.moderateRisk()),
                        new NameNum("Yüksek", d.heavyRisk()), new NameNum("Kritik", d.criticalRisk())));
        barChart(doc, "Duruma Göre Dağılım",
                List.of(new NameNum("İnceleme", d.pendingReview()), new NameNum("Saha Doğrulandı", d.fieldVerified()),
                        new NameNum("Koordinatör Onaylı", d.coordinatorApproved())));
        neighborhoodChart(doc, "Mahalle Bazlı Hasar Yoğunluğu", d.topNeighborhoods());
    }

    private void resourceSection(Document doc, ReportModules.ResourceModule r) {
        sectionTitle(doc, "Kaynak Talepleri");
        PdfPTable kv = kvTable();
        kv(kv, "Toplam Talep", num(r.total()));
        kv(kv, "Karşılanan", num(r.fulfilled()));
        kv(kv, "Bekleyen", num(r.pending()));
        kv(kv, "Kısmen Karşılanan", num(r.partial()));
        kv(kv, "Reddedilen", num(r.rejected()));
        kv(kv, "Karşılama Oranı", pct(r.fulfillmentRate()));
        kv(kv, "Kritik Bekleyen", num(r.criticalPending()));
        kv(kv, "Ort. Çözüm Süresi", minutes(r.avgResolutionMinutes()));
        doc.add(kv);
        barChart(doc, "Talep Durum Dağılımı",
                List.of(new NameNum("Karşılanan", r.fulfilled()), new NameNum("Bekleyen", r.pending()),
                        new NameNum("Kısmi", r.partial()), new NameNum("Reddedilen", r.rejected())));
        if (r.topCategories() != null && !r.topCategories().isEmpty()) {
            barChart(doc, "Kategori Bazlı Talep",
                    r.topCategories().stream().map(c -> new NameNum(c.label(), c.count())).toList());
        }
        neighborhoodChart(doc, "İlçe/Mahalle Bazlı Talep Yoğunluğu", r.topNeighborhoods());
    }

    private void stockSection(Document doc, ReportModules.StockModule s) {
        sectionTitle(doc, "Stok Durumu");
        PdfPTable kv = kvTable();
        kv(kv, "Toplam Stok Kalemi", num(s.totalItems()));
        kv(kv, "Toplam Miktar", num(s.totalQuantity()));
        kv(kv, "Kullanılabilir Miktar", num(s.availableQuantity()));
        kv(kv, "Kritik Kalem", num(s.criticalItems()));
        kv(kv, "Tükenen Kalem", num(s.outOfStockItems()));
        kv(kv, "Dönem Girişi", num(s.periodInflow()));
        kv(kv, "Dönem Çıkışı", num(s.periodOutflow()));
        kv(kv, "Tahmini Kalan Gün", s.estimatedDaysRemaining() != null ? s.estimatedDaysRemaining() + " gün" : NO_DATA);
        doc.add(kv);
        if (s.categoryDistribution() != null && !s.categoryDistribution().isEmpty()) {
            barChart(doc, "Kategori Bazlı Stok Dağılımı",
                    s.categoryDistribution().stream().map(c -> new NameNum(c.label(), c.count())).toList());
        }
        barChart(doc, "Stok Tüketim (Dönem)",
                List.of(new NameNum("Giriş", s.periodInflow()), new NameNum("Çıkış", s.periodOutflow())));
        neighborhoodChart(doc, "İlçe/Mahalle Bazlı Stok", s.neighborhoodDistribution());
    }

    private void documentSection(Document doc, ReportModules.DocumentModule d) {
        sectionTitle(doc, "Belge Onayları");
        PdfPTable kv = kvTable();
        kv(kv, "Yüklenen", num(d.uploaded()));
        kv(kv, "Onaylanan", num(d.approved()));
        kv(kv, "Reddedilen", num(d.rejected()));
        kv(kv, "Bekleyen", num(d.pending()));
        doc.add(kv);
        if (d.typeDistribution() != null && !d.typeDistribution().isEmpty()) {
            barChart(doc, "Belge Türü Dağılımı",
                    d.typeDistribution().stream().map(c -> new NameNum(c.label(), c.count())).toList());
        }
    }

    private void coordinatorSection(Document doc, ReportModules.CoordinatorModule c) {
        sectionTitle(doc, "Koordinatör Atamaları");
        PdfPTable kv = kvTable();
        kv(kv, "Dönemde Yapılan Atama", num(c.assignmentsInPeriod()));
        kv(kv, "Atanmış İlçe Koordinatörü", num(c.districtCoordinators()));
        kv(kv, "Atanmış Mahalle Koordinatörü", num(c.neighborhoodCoordinators()));
        kv(kv, "Bekleyen Süreç", c.pendingProcesses() != null ? String.valueOf(c.pendingProcesses()) : NO_DATA);
        doc.add(kv);
    }

    private void volunteerSection(Document doc, ReportModules.VolunteerModule v) {
        sectionTitle(doc, "Gönüllü Kayıtları");
        PdfPTable kv = kvTable();
        kv(kv, "Toplam Gönüllü", num(v.total()));
        kv(kv, "Dönemde Yeni Kayıt", num(v.newInPeriod()));
        kv(kv, "Onaylı", num(v.approved()));
        kv(kv, "Bekleyen", num(v.pending()));
        kv(kv, "Aktif", num(v.active()));
        kv(kv, "Pasif", num(v.passive()));
        kv(kv, "Göreve Atanmış", num(v.assigned()));
        kv(kv, "Reddedilen", v.rejected() != null ? String.valueOf(v.rejected()) : NO_DATA);
        kv(kv, "Eğitimli", v.trained() != null ? String.valueOf(v.trained()) : NO_DATA);
        kv(kv, "Kapasite Açığı", v.capacityGap() != null ? String.valueOf(v.capacityGap()) : NO_DATA);
        doc.add(kv);
        barChart(doc, "Gönüllü Durum Dağılımı",
                List.of(new NameNum("Aktif", v.active()), new NameNum("Pasif", v.passive()),
                        new NameNum("Atanmış", v.assigned())));
        neighborhoodChart(doc, "Mahalle Bazlı Gönüllü Dağılımı", v.neighborhoodDistribution());
    }

    // ════════════════════════ PDF — altyapı ════════════════════════

    private interface PdfBody { void render(Document doc); }

    private byte[] buildPdf(ReportMeta meta, PdfBody body) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 72, 50);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new HeaderFooter(meta));
            doc.open();
            titleBlock(doc, meta);
            body.render(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF rapor üretilemedi: {}", e.getMessage(), e);
            throw new IllegalStateException("PDF raporu oluşturulamadı", e);
        }
    }

    private void titleBlock(Document doc, ReportMeta m) {
        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);
        PdfPCell logo = new PdfPCell(new Phrase("AFETKOORDİNASYON", font(15, Font.BOLD, Color.WHITE)));
        logo.setBackgroundColor(BRAND);
        logo.setBorder(Rectangle.NO_BORDER);
        logo.setPadding(8);
        band.addCell(logo);
        doc.add(band);

        Paragraph title = new Paragraph(safe(m.typeLabel()), font(16, Font.BOLD, BRAND));
        title.setSpacingBefore(10);
        title.setSpacingAfter(2);
        doc.add(title);

        PdfPTable meta = new PdfPTable(new float[]{28, 72});
        meta.setWidthPercentage(100);
        metaRow(meta, "Dönem", safe(m.periodLabel()));
        metaRow(meta, "Kapsam", scopeText(m));
        metaRow(meta, "Hazırlayan", safe(m.generatedBy()) + " (" + safe(m.role()) + ")");
        metaRow(meta, "Oluşturma", safe(m.generatedAt()));
        doc.add(meta);
    }

    private String scopeText(ReportMeta m) {
        if (m.neighborhoodName() != null) return safe(m.districtName()) + " / " + safe(m.neighborhoodName());
        if (m.districtName() != null) return safe(m.districtName());
        return "Sistem Geneli";
    }

    private void kpiCards(Document doc, List<NameValue> kpis) {
        sectionTitle(doc, "Özet Göstergeler (KPI)");
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setSpacingBefore(2);
        for (NameValue kv : kpis) {
            PdfPCell cell = new PdfPCell();
            cell.setBorderColor(GRID);
            cell.setPadding(8);
            cell.setBackgroundColor(BRAND_LIGHT);
            Paragraph label = new Paragraph(kv.label(), font(8, Font.NORMAL, BRAND));
            Paragraph value = new Paragraph(kv.value(), font(14, Font.BOLD, ACCENT));
            cell.addElement(label);
            cell.addElement(value);
            t.addCell(cell);
        }
        int rem = kpis.size() % 4;
        if (rem != 0) for (int i = 0; i < 4 - rem; i++) {
            PdfPCell empty = new PdfPCell();
            empty.setBorder(Rectangle.NO_BORDER);
            t.addCell(empty);
        }
        doc.add(t);
    }

    /** Oransal yatay çubuk "grafik" (ek bağımlılık olmadan tablo hücreleriyle). */
    private void barChart(Document doc, String title, List<NameNum> data) {
        Paragraph p = new Paragraph(title, font(10, Font.BOLD, BRAND));
        p.setSpacingBefore(8);
        p.setSpacingAfter(3);
        doc.add(p);
        if (data == null || data.isEmpty() || data.stream().allMatch(d -> d.value() <= 0)) {
            doc.add(new Paragraph(NO_DATA, font(9, Font.ITALIC, Color.GRAY)));
            return;
        }
        double max = data.stream().mapToDouble(NameNum::value).max().orElse(1);
        if (max <= 0) max = 1;
        PdfPTable t = new PdfPTable(new float[]{30, 55, 15});
        t.setWidthPercentage(100);
        for (NameNum d : data) {
            t.addCell(noBorder(new Phrase(d.label(), font(9, Font.NORMAL, Color.DARK_GRAY))));
            int filled = (int) Math.max(1, Math.round((d.value() / max) * 100));
            PdfPTable bar = new PdfPTable(2);
            bar.setWidthPercentage(100);
            try { bar.setWidths(new int[]{Math.max(1, filled), Math.max(1, 100 - filled)}); } catch (Exception ignored) {}
            PdfPCell fill = new PdfPCell();
            fill.setBackgroundColor(BAR);
            fill.setFixedHeight(10);
            fill.setBorder(Rectangle.NO_BORDER);
            PdfPCell rest = new PdfPCell();
            rest.setBorder(Rectangle.NO_BORDER);
            bar.addCell(fill);
            if (filled < 100) bar.addCell(rest); else bar.addCell(emptyNoBorder());
            PdfPCell barCell = new PdfPCell(bar);
            barCell.setBorder(Rectangle.NO_BORDER);
            barCell.setPadding(2);
            t.addCell(barCell);
            t.addCell(noBorder(new Phrase(numD(d.value()), font(9, Font.BOLD, Color.DARK_GRAY))));
        }
        doc.add(t);
    }

    private void neighborhoodChart(Document doc, String title, List<ReportModules.NeighborhoodMetric> rows) {
        if (rows == null || rows.isEmpty()) {
            Paragraph p = new Paragraph(title, font(10, Font.BOLD, BRAND));
            p.setSpacingBefore(8);
            doc.add(p);
            doc.add(new Paragraph(NO_DATA, font(9, Font.ITALIC, Color.GRAY)));
            return;
        }
        barChart(doc, title, rows.stream()
                .map(n -> new NameNum(n.displayLabel(), n.value())).toList());
    }

    private void managerSummary(Document doc, String text) {
        if (text == null || text.isBlank()) return;
        sectionTitle(doc, "Yönetici Değerlendirmesi");
        PdfPTable wrap = new PdfPTable(1);
        wrap.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Paragraph(text, font(10, Font.ITALIC, Color.DARK_GRAY)));
        c.setBackgroundColor(BRAND_LIGHT);
        c.setBorderColor(GRID);
        c.setPadding(8);
        wrap.addCell(c);
        doc.add(wrap);
    }

    private void sectionTitle(Document doc, String text) {
        Paragraph p = new Paragraph(text, font(12, Font.BOLD, BRAND));
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        doc.add(p);
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(ACCENT);
        c.setFixedHeight(2);
        c.setBorder(Rectangle.NO_BORDER);
        rule.addCell(c);
        doc.add(rule);
    }

    private void bullet(Document doc, String text) {
        Paragraph p = new Paragraph("•  " + text, font(10, Font.NORMAL, Color.DARK_GRAY));
        p.setSpacingAfter(2);
        p.setIndentationLeft(8);
        doc.add(p);
    }

    private PdfPTable kvTable() {
        PdfPTable t = new PdfPTable(new float[]{1, 1, 1, 1});
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        return t;
    }

    private void kv(PdfPTable t, String k, String v) {
        PdfPCell kc = new PdfPCell(new Phrase(k, font(8, Font.NORMAL, BRAND)));
        kc.setBackgroundColor(BRAND_LIGHT);
        kc.setBorderColor(GRID);
        kc.setPadding(4);
        PdfPCell vc = new PdfPCell(new Phrase(v, font(9, Font.BOLD, Color.DARK_GRAY)));
        vc.setBorderColor(GRID);
        vc.setPadding(4);
        t.addCell(kc);
        t.addCell(vc);
    }

    private PdfPCell noBorder(Phrase p) {
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3);
        return c;
    }

    private PdfPCell emptyNoBorder() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private void metaRow(PdfPTable t, String k, String v) {
        PdfPCell kc = new PdfPCell(new Phrase(k, font(9, Font.BOLD, BRAND)));
        kc.setBackgroundColor(BRAND_LIGHT);
        kc.setBorderColor(GRID);
        kc.setPadding(4);
        PdfPCell vc = new PdfPCell(new Phrase(v, font(9, Font.NORMAL, Color.DARK_GRAY)));
        vc.setBorderColor(GRID);
        vc.setPadding(4);
        t.addCell(kc);
        t.addCell(vc);
    }

    private Font font(float size, int style, Color color) {
        return FontFactory.getFont(BaseFont.HELVETICA, ENC, BaseFont.EMBEDDED, size, style, color);
    }

    private class HeaderFooter extends PdfPageEventHelper {
        private final ReportMeta meta;
        HeaderFooter(ReportMeta meta) { this.meta = meta; }
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle rect = document.getPageSize();
            Font small = font(8, Font.NORMAL, Color.GRAY);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("AfetKoordinasyon — " + safe(meta.typeLabel()), small),
                    rect.getLeft(40), rect.getTop(30), 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(safe(meta.periodLabel()), small),
                    rect.getRight(40), rect.getTop(30), 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase("Sayfa " + writer.getPageNumber(), small),
                    (rect.getLeft() + rect.getRight()) / 2, rect.getBottom(30), 0);
        }
    }

    // ════════════════════════ Excel — altyapı ════════════════════════

    private interface ExcelBody { void render(Workbook wb); }

    private byte[] buildExcel(ExcelBody body) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            body.render(wb);
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Excel rapor üretilemedi: {}", e.getMessage(), e);
            throw new IllegalStateException("Excel raporu oluşturulamadı", e);
        }
    }

    private void generalSheet(Workbook wb, ReportMeta m, List<NameValue> kpis, String summary) {
        Sheet s = wb.createSheet("Genel");
        CellStyle title = titleStyle(wb), header = headerStyle(wb);
        int row = 0;
        row = titleRow(s, row, safe(m.typeLabel()), title);
        row = kvRow(s, row, "Dönem", safe(m.periodLabel()));
        row = kvRow(s, row, "Kapsam", scopeText(m));
        row = kvRow(s, row, "Hazırlayan", safe(m.generatedBy()) + " (" + safe(m.role()) + ")");
        row = kvRow(s, row, "Oluşturma", safe(m.generatedAt()));
        row++;
        row = titleRow(s, row, "Özet Göstergeler (KPI)", title);
        row = headerRow(s, row, header, "Gösterge", "Değer");
        for (NameValue kv : kpis) row = kvRow(s, row, kv.label(), kv.value());
        row++;
        row = titleRow(s, row, "Yönetici Değerlendirmesi", title);
        s.createRow(row).createCell(0).setCellValue(summary != null ? summary : NO_DATA);
        autoSize(s, 2);
    }

    private void teamSheet(Workbook wb, ReportModules.TeamModule t) {
        Sheet s = wb.createSheet("Ekip");
        CellStyle header = headerStyle(wb);
        int row = headerRow(s, 0, header, "Gösterge", "Değer");
        row = statRow(s, row, "Toplam Ekip", t.totalTeams());
        row = statRow(s, row, "Aktif Ekip", t.activeTeams());
        row = statRow(s, row, "Müsait Ekip", t.availableTeams());
        row = statRow(s, row, "Toplam Görev", t.totalTasks());
        row = statRow(s, row, "Tamamlanan Görev", t.completedTasks());
        row = statRow(s, row, "Devam Eden Görev", t.inProgressTasks());
        row = statRow(s, row, "Açık Görev", t.openTasks());
        row = statRow(s, row, "Gereken Personel", t.requiredPeople());
        row = statRow(s, row, "Ek Personel İhtiyacı", t.shortage());
        row = kvRow(s, row, "Ort. Görev Tamamlama (dk)", t.avgCompletionMinutes() != null ? String.valueOf(t.avgCompletionMinutes()) : NO_DATA);
        row++;
        if (t.teamBreakdown() != null && !t.teamBreakdown().isEmpty()) {
            row = headerRow(s, row, header, "Ekip Tipi", "Görev Sayısı");
            for (var c : t.teamBreakdown()) row = kvNumRow(s, row, c.label(), c.count());
        }
        autoSize(s, 2);
    }

    private void damageSheet(Workbook wb, ReportModules.DamageModule d) {
        Sheet s = wb.createSheet("Hasar");
        CellStyle header = headerStyle(wb);
        int row = headerRow(s, 0, header, "Gösterge", "Değer");
        row = statRow(s, row, "Toplam Hasar", d.total());
        row = statRow(s, row, "Dönemde Yeni", d.newInPeriod());
        row = statRow(s, row, "İnceleme Bekleyen", d.pendingReview());
        row = statRow(s, row, "Sahada Doğrulanan", d.fieldVerified());
        row = statRow(s, row, "Koordinatör Onaylı", d.coordinatorApproved());
        row = statRow(s, row, "Ekip Gönderilen", d.assignedTeam());
        row = statRow(s, row, "Ekip İhtiyacı", d.teamNeeded());
        row = statRow(s, row, "Düşük Risk", d.lightRisk());
        row = statRow(s, row, "Orta Risk", d.moderateRisk());
        row = statRow(s, row, "Yüksek Risk", d.heavyRisk());
        row = statRow(s, row, "Kritik Risk", d.criticalRisk());
        row++;
        row = neighborhoodSheetBlock(s, header, row, d.topNeighborhoods());
        autoSize(s, 2);
    }

    private void resourceSheet(Workbook wb, ReportModules.ResourceModule r) {
        Sheet s = wb.createSheet("Kaynak");
        CellStyle header = headerStyle(wb);
        int row = headerRow(s, 0, header, "Gösterge", "Değer");
        row = statRow(s, row, "Toplam Talep", r.total());
        row = statRow(s, row, "Karşılanan", r.fulfilled());
        row = statRow(s, row, "Bekleyen", r.pending());
        row = statRow(s, row, "Kısmen Karşılanan", r.partial());
        row = statRow(s, row, "Reddedilen", r.rejected());
        row = statRow(s, row, "Karşılama Oranı (%)", r.fulfillmentRate());
        row = statRow(s, row, "Kritik Bekleyen", r.criticalPending());
        row++;
        if (r.topCategories() != null && !r.topCategories().isEmpty()) {
            row = headerRow(s, row, header, "Kategori", "Talep Sayısı");
            for (var c : r.topCategories()) row = kvNumRow(s, row, c.label(), c.count());
            row++;
        }
        neighborhoodSheetBlock(s, header, row, r.topNeighborhoods());
        autoSize(s, 2);
    }

    private void stockSheet(Workbook wb, ReportModules.StockModule st) {
        Sheet s = wb.createSheet("Stok");
        CellStyle header = headerStyle(wb);
        int row = headerRow(s, 0, header, "Gösterge", "Değer");
        row = statRow(s, row, "Toplam Stok Kalemi", st.totalItems());
        row = statRow(s, row, "Toplam Miktar", st.totalQuantity());
        row = statRow(s, row, "Kullanılabilir Miktar", st.availableQuantity());
        row = statRow(s, row, "Kritik Kalem", st.criticalItems());
        row = statRow(s, row, "Tükenen Kalem", st.outOfStockItems());
        row = statRow(s, row, "Dönem Girişi", st.periodInflow());
        row = statRow(s, row, "Dönem Çıkışı", st.periodOutflow());
        row = kvRow(s, row, "Tahmini Kalan Gün", st.estimatedDaysRemaining() != null ? String.valueOf(st.estimatedDaysRemaining()) : NO_DATA);
        row++;
        if (st.categoryDistribution() != null && !st.categoryDistribution().isEmpty()) {
            row = headerRow(s, row, header, "Kategori", "Miktar");
            for (var c : st.categoryDistribution()) row = kvNumRow(s, row, c.label(), c.count());
            row++;
        }
        neighborhoodSheetBlock(s, header, row, st.neighborhoodDistribution());
        autoSize(s, 2);
    }

    private void documentSheet(Workbook wb, ReportModules.DocumentModule d) {
        Sheet s = wb.createSheet("Belge");
        CellStyle header = headerStyle(wb);
        int row = headerRow(s, 0, header, "Gösterge", "Değer");
        row = statRow(s, row, "Yüklenen", d.uploaded());
        row = statRow(s, row, "Onaylanan", d.approved());
        row = statRow(s, row, "Reddedilen", d.rejected());
        row = statRow(s, row, "Bekleyen", d.pending());
        row++;
        if (d.typeDistribution() != null && !d.typeDistribution().isEmpty()) {
            row = headerRow(s, row, header, "Belge Türü", "Sayı");
            for (var c : d.typeDistribution()) row = kvNumRow(s, row, c.label(), c.count());
        }
        autoSize(s, 2);
    }

    private void coordinatorSheet(Workbook wb, ReportModules.CoordinatorModule c) {
        Sheet s = wb.createSheet("Koordinatör");
        CellStyle header = headerStyle(wb);
        int row = headerRow(s, 0, header, "Gösterge", "Değer");
        row = statRow(s, row, "Dönemde Yapılan Atama", c.assignmentsInPeriod());
        row = statRow(s, row, "Atanmış İlçe Koordinatörü", c.districtCoordinators());
        row = statRow(s, row, "Atanmış Mahalle Koordinatörü", c.neighborhoodCoordinators());
        autoSize(s, 2);
    }

    private void volunteerSheet(Workbook wb, ReportModules.VolunteerModule v) {
        Sheet s = wb.createSheet("Gönüllü");
        CellStyle header = headerStyle(wb);
        int row = headerRow(s, 0, header, "Gösterge", "Değer");
        row = statRow(s, row, "Toplam Gönüllü", v.total());
        row = statRow(s, row, "Dönemde Yeni Kayıt", v.newInPeriod());
        row = statRow(s, row, "Onaylı", v.approved());
        row = statRow(s, row, "Bekleyen", v.pending());
        row = statRow(s, row, "Aktif", v.active());
        row = statRow(s, row, "Pasif", v.passive());
        row = statRow(s, row, "Göreve Atanmış", v.assigned());
        row++;
        neighborhoodSheetBlock(s, header, row, v.neighborhoodDistribution());
        autoSize(s, 2);
    }

    private int neighborhoodSheetBlock(Sheet s, CellStyle header, int row, List<ReportModules.NeighborhoodMetric> rows) {
        if (rows == null || rows.isEmpty()) {
            s.createRow(row++).createCell(0).setCellValue("Mahalle dağılımı: " + NO_DATA);
            return row;
        }
        row = headerRow(s, row, header, "Mahalle", "Değer");
        for (var n : rows) row = kvNumRow(s, row, n.displayLabel(), n.value());
        return row;
    }

    // ── Excel yardımcıları ──
    private int titleRow(Sheet s, int row, String text, CellStyle style) {
        Cell c = s.createRow(row).createCell(0);
        c.setCellValue(text);
        c.setCellStyle(style);
        return row + 1;
    }
    private int kvRow(Sheet s, int row, String k, String v) {
        Row r = s.createRow(row);
        r.createCell(0).setCellValue(k);
        r.createCell(1).setCellValue(v);
        return row + 1;
    }
    private int kvNumRow(Sheet s, int row, String k, double v) {
        Row r = s.createRow(row);
        r.createCell(0).setCellValue(k);
        r.createCell(1).setCellValue(v);
        return row + 1;
    }
    private int statRow(Sheet s, int row, String k, double v) {
        return kvNumRow(s, row, k, v);
    }
    private int headerRow(Sheet s, int row, CellStyle style, String... headers) {
        Row r = s.createRow(row);
        for (int i = 0; i < headers.length; i++) {
            Cell c = r.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(style);
        }
        return row + 1;
    }
    private void autoSize(Sheet s, int columns) {
        for (int i = 0; i < columns; i++) s.autoSizeColumn(i);
    }
    private CellStyle headerStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        st.setFont(f);
        st.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return st;
    }
    private CellStyle titleStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 13);
        f.setColor(IndexedColors.DARK_BLUE.getIndex());
        st.setFont(f);
        return st;
    }

    // ── Ortak ──
    private static String safe(String s) { return s == null ? "" : s; }
    private static String num(long v) { return String.valueOf(v); }
    private static String numD(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.1f", v); }
    private static String pct(double v) { return String.format("%%%.0f", v); }
    private static String minutes(Long m) { return m == null ? NO_DATA : m + " dk"; }
}
