package com.afet.koordinasyon.service;

import com.afet.koordinasyon.dto.response.DailyReportResponse;
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
import java.util.List;

/**
 * §11 — Rapor export servisi.
 * Kurumsal formatlı PDF (OpenPDF) ve çok-sayfalı (sheet) Excel (Apache POI) üretir.
 * Tüm metinler Türkçe karakter desteği için Cp1254 kodlamasıyla render edilir.
 */
@Service
@Slf4j
public class ReportExportService {

    // Kurumsal renk paleti
    private static final Color BRAND = new Color(30, 58, 95);     // koyu lacivert
    private static final Color BRAND_LIGHT = new Color(224, 231, 243);
    private static final Color ACCENT = new Color(190, 30, 45);   // afet kırmızısı
    private static final Color GRID = new Color(210, 215, 222);

    private static final String ENC = "Cp1254"; // Türkçe karakter seti (Latin-5)

    // ── PDF ──────────────────────────────────────────────────────────────────

    public byte[] toPdf(DailyReportResponse r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 70, 50);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new HeaderFooter(r.meta()));
            doc.open();

            addTitleBlock(doc, r.meta());
            addExecutiveSummary(doc, r);
            addRiskAnalysis(doc, r);
            addKpiAndCoreStats(doc, r);
            addOperationPerformance(doc, r);
            addTrendAnalysis(doc, r);
            addNeighborhoodComparison(doc, r);
            addAiCommentary(doc, r);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF rapor üretilemedi: {}", e.getMessage(), e);
            throw new IllegalStateException("PDF raporu oluşturulamadı", e);
        }
    }

    private void addTitleBlock(Document doc, DailyReportResponse.Meta m) {
        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell(new Phrase("AFETKOORDİNASYON", font(15, Font.BOLD, Color.WHITE)));
        logoCell.setBackgroundColor(BRAND);
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(8);
        band.addCell(logoCell);
        doc.add(band);

        Paragraph title = new Paragraph(safe(m.title()), font(16, Font.BOLD, BRAND));
        title.setSpacingBefore(10);
        title.setSpacingAfter(2);
        doc.add(title);

        Paragraph sub = new Paragraph(safe(m.scopeLabel()), font(11, Font.NORMAL, ACCENT));
        sub.setSpacingAfter(8);
        doc.add(sub);

        PdfPTable meta = new PdfPTable(new float[]{30, 70});
        meta.setWidthPercentage(100);
        addMetaRow(meta, "Tarih", safe(m.date()) + " " + safe(m.time()));
        addMetaRow(meta, "Kapsam", scopeText(m));
        addMetaRow(meta, "Hazırlayan", safe(m.generatedBy()) + " (" + safe(m.role()) + ")");
        doc.add(meta);
    }

    private String scopeText(DailyReportResponse.Meta m) {
        if (m.neighborhoodName() != null) return safe(m.districtName()) + " / " + safe(m.neighborhoodName());
        if (m.districtName() != null) return safe(m.districtName());
        return "Sistem Geneli";
    }

    private void addMetaRow(PdfPTable t, String k, String v) {
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

    private void addExecutiveSummary(Document doc, DailyReportResponse r) {
        sectionTitle(doc, "Genel Durum Özeti");
        if (r.executiveSummary() != null) {
            for (String h : r.executiveSummary().highlights()) {
                bullet(doc, h);
            }
        }
    }

    private void addRiskAnalysis(Document doc, DailyReportResponse r) {
        sectionTitle(doc, "Risk Analizi");
        DailyReportResponse.RiskAnalysis risk = r.riskAnalysis();
        if (risk == null) return;

        Paragraph score = new Paragraph();
        score.add(new Phrase("Risk Puanı: ", font(11, Font.BOLD, BRAND)));
        score.add(new Phrase(risk.score() + "/100  ", font(11, Font.BOLD, ACCENT)));
        score.add(new Phrase("(" + risk.level() + ")", font(11, Font.BOLD, levelColor(risk.level()))));
        score.setSpacingAfter(6);
        doc.add(score);

        PdfPTable t = new PdfPTable(new float[]{4, 1.2f, 5});
        t.setWidthPercentage(100);
        headerCell(t, "Faktör");
        headerCell(t, "Puan");
        headerCell(t, "Açıklama");
        for (DailyReportResponse.RiskFactor f : risk.factors()) {
            bodyCell(t, f.label());
            bodyCell(t, String.valueOf(f.points()));
            bodyCell(t, f.detail());
        }
        doc.add(t);

        if (risk.warnings() != null && !risk.warnings().isEmpty()) {
            Paragraph w = new Paragraph("Yöneticinin dikkat etmesi gereken hususlar:", font(10, Font.BOLD, ACCENT));
            w.setSpacingBefore(6);
            doc.add(w);
            for (String warn : risk.warnings()) bullet(doc, warn);
        }
    }

    private void addKpiAndCoreStats(Document doc, DailyReportResponse r) {
        sectionTitle(doc, "Operasyonel Göstergeler (KPI)");
        if (r.kpis() != null) for (String k : r.kpis()) bullet(doc, k);

        PdfPTable t = new PdfPTable(new float[]{3, 1.4f, 3, 1.4f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(8);
        kv(t, "Toplam Talep", num(r.resources().total()));
        kv(t, "Karşılanan", num(r.resources().fulfilled()));
        kv(t, "Bekleyen Talep", num(r.resources().pending()));
        kv(t, "Karşılama Oranı", pct(r.resources().fulfillmentRate()));
        kv(t, "Toplam Görev", num(r.tasks().total()));
        kv(t, "Tamamlanan", num(r.tasks().completed()));
        kv(t, "Kritik Stok", num(r.stock().criticalItems() + r.stock().outOfStockItems()));
        kv(t, "Son 24s Tüketim", num(r.stock().last24hConsumed()));
        kv(t, "Bildirilen Hasar", num(r.damage().reported()));
        kv(t, "Doğrulanan Hasar", num(r.damage().verified()));
        kv(t, "Toplanma Alanı", num(r.assemblyAreas().total()));
        kv(t, "Aktif Gönüllü", num(r.volunteers().active()));
        doc.add(t);
    }

    private void addOperationPerformance(Document doc, DailyReportResponse r) {
        DailyReportResponse.OperationPerformance p = r.operationPerformance();
        if (p == null) return;
        sectionTitle(doc, "Operasyon Performansı");
        PdfPTable t = new PdfPTable(new float[]{3, 2});
        t.setWidthPercentage(100);
        headerCell(t, "Metrik");
        headerCell(t, "Değer");
        bodyCell(t, "Görev Tamamlama Oranı"); bodyCell(t, pct(p.taskCompletionRate()));
        bodyCell(t, "Talep Karşılama Oranı"); bodyCell(t, pct(p.requestFulfillmentRate()));
        bodyCell(t, "Kaynak Dağıtım Performansı"); bodyCell(t, pct(p.distributionPerformance()));
        bodyCell(t, "Ortalama Müdahale Süresi"); bodyCell(t, minutes(p.avgResponseMinutes()));
        bodyCell(t, "Ortalama Talep Çözüm Süresi"); bodyCell(t, minutes(p.avgResolutionMinutes()));
        doc.add(t);
    }

    private void addTrendAnalysis(Document doc, DailyReportResponse r) {
        DailyReportResponse.TrendAnalysis tr = r.trendAnalysis();
        if (tr == null) return;
        sectionTitle(doc, "Trend Analizi (Bugün / Son 7 Gün / Son 30 Gün)");
        PdfPTable t = new PdfPTable(new float[]{3, 1.4f, 1.6f, 1.6f, 1.8f});
        t.setWidthPercentage(100);
        headerCell(t, "Metrik");
        headerCell(t, "Bugün");
        headerCell(t, "Son 7 Gün");
        headerCell(t, "Son 30 Gün");
        headerCell(t, "Değişim (7g)");
        trendRow(t, "Talepler", tr.requests());
        trendRow(t, "Görevler", tr.tasks());
        trendRow(t, "Kaynak Tüketimi", tr.resourceConsumption());
        trendRow(t, "Stok Değişimi", tr.stockChange());
        doc.add(t);
    }

    private void trendRow(PdfPTable t, String label, DailyReportResponse.TrendMetric m) {
        bodyCell(t, label);
        bodyCell(t, num(m.today()));
        bodyCell(t, num(m.last7Days()));
        bodyCell(t, num(m.last30Days()));
        bodyCell(t, m.changeRate() == null ? "—" : String.format("%%%.1f", m.changeRate()));
    }

    private void addNeighborhoodComparison(Document doc, DailyReportResponse r) {
        List<DailyReportResponse.NeighborhoodComparison> list = r.neighborhoodComparisons();
        if (list == null || list.isEmpty()) return;
        sectionTitle(doc, "Mahalle Karşılaştırmaları");
        PdfPTable t = new PdfPTable(new float[]{3, 1.4f, 1.6f, 1.4f, 1.4f, 1.4f});
        t.setWidthPercentage(100);
        headerCell(t, "Mahalle");
        headerCell(t, "Talep");
        headerCell(t, "Kaynak Kul.");
        headerCell(t, "Görev");
        headerCell(t, "Hasar");
        headerCell(t, "Risk");
        for (DailyReportResponse.NeighborhoodComparison c : list) {
            bodyCell(t, c.neighborhoodName());
            bodyCell(t, num(c.requestCount()));
            bodyCell(t, num(c.resourceUsage()));
            bodyCell(t, num(c.taskCount()));
            bodyCell(t, num(c.damageCount()));
            bodyCell(t, String.valueOf(c.riskScore()));
        }
        doc.add(t);
    }

    private void addAiCommentary(Document doc, DailyReportResponse r) {
        if (r.aiCommentary() == null || r.aiCommentary().isBlank()) return;
        sectionTitle(doc, "Yapay Zeka Destekli Yönetici Değerlendirmesi");
        Paragraph p = new Paragraph(r.aiCommentary(), font(10, Font.ITALIC, Color.DARK_GRAY));
        p.setSpacingAfter(6);
        PdfPTable wrap = new PdfPTable(1);
        wrap.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(p);
        c.setBackgroundColor(BRAND_LIGHT);
        c.setBorderColor(GRID);
        c.setPadding(8);
        wrap.addCell(c);
        doc.add(wrap);
    }

    // ── PDF yardımcıları ──────────────────────────────────────────────────────

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

    private void headerCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(9, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(BRAND);
        c.setBorderColor(GRID);
        c.setPadding(5);
        t.addCell(c);
    }

    private void bodyCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(safe(text), font(9, Font.NORMAL, Color.DARK_GRAY)));
        c.setBorderColor(GRID);
        c.setPadding(4);
        t.addCell(c);
    }

    private void kv(PdfPTable t, String k, String v) {
        PdfPCell kc = new PdfPCell(new Phrase(k, font(9, Font.NORMAL, BRAND)));
        kc.setBackgroundColor(BRAND_LIGHT);
        kc.setBorderColor(GRID);
        kc.setPadding(4);
        PdfPCell vc = new PdfPCell(new Phrase(v, font(9, Font.BOLD, Color.DARK_GRAY)));
        vc.setBorderColor(GRID);
        vc.setPadding(4);
        t.addCell(kc);
        t.addCell(vc);
    }

    private Font font(float size, int style, Color color) {
        return FontFactory.getFont(BaseFont.HELVETICA, ENC, BaseFont.EMBEDDED, size, style, color);
    }

    private Color levelColor(String level) {
        return switch (level) {
            case "KRİTİK" -> ACCENT;
            case "YÜKSEK" -> new Color(214, 120, 20);
            case "ORTA" -> new Color(180, 150, 0);
            default -> new Color(40, 130, 70);
        };
    }

    /** Sayfa başlığı (üst bant) ve alt bilgi (sayfa numarası) — her sayfada. */
    private class HeaderFooter extends PdfPageEventHelper {
        private final DailyReportResponse.Meta meta;

        HeaderFooter(DailyReportResponse.Meta meta) {
            this.meta = meta;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle rect = document.getPageSize();
            Font small = font(8, Font.NORMAL, Color.GRAY);
            // Üst bilgi
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("AfetKoordinasyon — " + safe(meta.title()), small),
                    rect.getLeft(40), rect.getTop(30), 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(safe(meta.date()) + " " + safe(meta.time()), small),
                    rect.getRight(40), rect.getTop(30), 0);
            // Alt bilgi — sayfa numarası
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase("Sayfa " + writer.getPageNumber(), small),
                    (rect.getLeft() + rect.getRight()) / 2, rect.getBottom(30), 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Gizli — Kuruma Özel", small),
                    rect.getRight(40), rect.getBottom(30), 0);
        }
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    public byte[] toExcel(DailyReportResponse r) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(wb);
            CellStyle title = titleStyle(wb);

            sheetGeneral(wb, r, title, header);
            sheetSummaryKpi(wb, r, title, header);
            sheetRisk(wb, r, title, header);
            sheetOperation(wb, r, title, header);
            sheetTrend(wb, r, title, header);
            sheetNeighborhoods(wb, r, title, header);
            sheetDetailStats(wb, r, title, header);

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Excel rapor üretilemedi: {}", e.getMessage(), e);
            throw new IllegalStateException("Excel raporu oluşturulamadı", e);
        }
    }

    private void sheetGeneral(Workbook wb, DailyReportResponse r, CellStyle title, CellStyle header) {
        Sheet s = wb.createSheet("Genel Bilgi");
        DailyReportResponse.Meta m = r.meta();
        int row = 0;
        row = titleRow(s, row, safe(m.title()), title);
        row = kvRow(s, row, "Kapsam", scopeText(m));
        row = kvRow(s, row, "Tarih", safe(m.date()) + " " + safe(m.time()));
        row = kvRow(s, row, "Hazırlayan", safe(m.generatedBy()) + " (" + safe(m.role()) + ")");
        row++;
        row = titleRow(s, row, "Genel Durum Özeti", title);
        if (r.executiveSummary() != null) {
            for (String h : r.executiveSummary().highlights()) {
                s.createRow(row++).createCell(0).setCellValue("• " + h);
            }
        }
        autoSize(s, 2);
    }

    private void sheetSummaryKpi(Workbook wb, DailyReportResponse r, CellStyle title, CellStyle header) {
        Sheet s = wb.createSheet("Özet & KPI");
        int row = 0;
        row = titleRow(s, row, "Operasyonel Göstergeler", title);
        if (r.kpis() != null) for (String k : r.kpis()) s.createRow(row++).createCell(0).setCellValue("• " + k);
        row++;
        row = headerRow(s, row, header, "Gösterge", "Değer");
        row = statRow(s, row, "Toplam Talep", r.resources().total());
        row = statRow(s, row, "Karşılanan Talep", r.resources().fulfilled());
        row = statRow(s, row, "Bekleyen Talep", r.resources().pending());
        row = statRow(s, row, "Karşılama Oranı (%)", r.resources().fulfillmentRate());
        row = statRow(s, row, "Toplam Görev", r.tasks().total());
        row = statRow(s, row, "Tamamlanan Görev", r.tasks().completed());
        autoSize(s, 2);
    }

    private void sheetRisk(Workbook wb, DailyReportResponse r, CellStyle title, CellStyle header) {
        Sheet s = wb.createSheet("Risk Analizi");
        int row = 0;
        DailyReportResponse.RiskAnalysis risk = r.riskAnalysis();
        row = titleRow(s, row, "Risk Analizi", title);
        if (risk != null) {
            row = kvRow(s, row, "Risk Puanı", risk.score() + "/100");
            row = kvRow(s, row, "Risk Seviyesi", risk.level());
            row++;
            row = headerRow(s, row, header, "Faktör", "Puan", "Açıklama");
            for (DailyReportResponse.RiskFactor f : risk.factors()) {
                Row rr = s.createRow(row++);
                rr.createCell(0).setCellValue(f.label());
                rr.createCell(1).setCellValue(f.points());
                rr.createCell(2).setCellValue(f.detail());
            }
            if (risk.warnings() != null && !risk.warnings().isEmpty()) {
                row++;
                row = titleRow(s, row, "Uyarılar", title);
                for (String w : risk.warnings()) s.createRow(row++).createCell(0).setCellValue("• " + w);
            }
        }
        autoSize(s, 3);
    }

    private void sheetOperation(Workbook wb, DailyReportResponse r, CellStyle title, CellStyle header) {
        Sheet s = wb.createSheet("Operasyon");
        int row = 0;
        DailyReportResponse.OperationPerformance p = r.operationPerformance();
        row = titleRow(s, row, "Operasyon Performansı", title);
        if (p != null) {
            row = headerRow(s, row, header, "Metrik", "Değer");
            row = statRow(s, row, "Görev Tamamlama Oranı (%)", p.taskCompletionRate());
            row = statRow(s, row, "Talep Karşılama Oranı (%)", p.requestFulfillmentRate());
            row = statRow(s, row, "Kaynak Dağıtım Performansı (%)", p.distributionPerformance());
            row = kvRow(s, row, "Ortalama Müdahale Süresi (dk)", p.avgResponseMinutes() == null ? "—" : String.valueOf(p.avgResponseMinutes()));
            row = kvRow(s, row, "Ortalama Talep Çözüm Süresi (dk)", p.avgResolutionMinutes() == null ? "—" : String.valueOf(p.avgResolutionMinutes()));
        }
        autoSize(s, 2);
    }

    private void sheetTrend(Workbook wb, DailyReportResponse r, CellStyle title, CellStyle header) {
        Sheet s = wb.createSheet("Trend");
        int row = 0;
        DailyReportResponse.TrendAnalysis tr = r.trendAnalysis();
        row = titleRow(s, row, "Trend Analizi", title);
        if (tr != null) {
            row = headerRow(s, row, header, "Metrik", "Bugün", "Son 7 Gün", "Son 30 Gün", "Değişim 7g (%)");
            row = trendRow(s, row, "Talepler", tr.requests());
            row = trendRow(s, row, "Görevler", tr.tasks());
            row = trendRow(s, row, "Kaynak Tüketimi", tr.resourceConsumption());
            row = trendRow(s, row, "Stok Değişimi", tr.stockChange());
        }
        autoSize(s, 5);
    }

    private int trendRow(Sheet s, int row, String label, DailyReportResponse.TrendMetric m) {
        Row rr = s.createRow(row);
        rr.createCell(0).setCellValue(label);
        rr.createCell(1).setCellValue(m.today());
        rr.createCell(2).setCellValue(m.last7Days());
        rr.createCell(3).setCellValue(m.last30Days());
        rr.createCell(4).setCellValue(m.changeRate() == null ? "—" : String.valueOf(m.changeRate()));
        return row + 1;
    }

    private void sheetNeighborhoods(Workbook wb, DailyReportResponse r, CellStyle title, CellStyle header) {
        Sheet s = wb.createSheet("Mahalle Karşılaştırma");
        int row = 0;
        row = titleRow(s, row, "Mahalle Karşılaştırmaları", title);
        List<DailyReportResponse.NeighborhoodComparison> list = r.neighborhoodComparisons();
        if (list == null || list.isEmpty()) {
            s.createRow(row).createCell(0).setCellValue("Bu kapsam için mahalle karşılaştırması bulunmamaktadır.");
        } else {
            row = headerRow(s, row, header, "Mahalle", "Talep", "Kaynak Kullanımı", "Görev", "Hasar", "Risk Puanı");
            for (DailyReportResponse.NeighborhoodComparison c : list) {
                Row rr = s.createRow(row++);
                rr.createCell(0).setCellValue(c.neighborhoodName());
                rr.createCell(1).setCellValue(c.requestCount());
                rr.createCell(2).setCellValue(c.resourceUsage());
                rr.createCell(3).setCellValue(c.taskCount());
                rr.createCell(4).setCellValue(c.damageCount());
                rr.createCell(5).setCellValue(c.riskScore());
            }
        }
        autoSize(s, 6);
    }

    private void sheetDetailStats(Workbook wb, DailyReportResponse r, CellStyle title, CellStyle header) {
        Sheet s = wb.createSheet("Detay İstatistik");
        int row = 0;
        row = titleRow(s, row, "Stok Durumu", title);
        row = headerRow(s, row, header, "Gösterge", "Değer");
        row = statRow(s, row, "Toplam Kalem", r.stock().totalItems());
        row = statRow(s, row, "Kritik Kalem", r.stock().criticalItems());
        row = statRow(s, row, "Tükenen Kalem", r.stock().outOfStockItems());
        row = statRow(s, row, "Son 24s Tüketim", r.stock().last24hConsumed());
        row++;
        row = titleRow(s, row, "Hasar Durumu", title);
        row = headerRow(s, row, header, "Gösterge", "Değer");
        row = statRow(s, row, "Bildirilen", r.damage().reported());
        row = statRow(s, row, "Doğrulanan", r.damage().verified());
        row = statRow(s, row, "Hafif", r.damage().light());
        row = statRow(s, row, "Orta", r.damage().moderate());
        row = statRow(s, row, "Ağır", r.damage().heavy());
        row = statRow(s, row, "Yıkık", r.damage().collapsed());
        row++;
        row = titleRow(s, row, "Gönüllü & İletişim", title);
        row = headerRow(s, row, header, "Gösterge", "Değer");
        row = statRow(s, row, "Toplam Gönüllü", r.volunteers().total());
        row = statRow(s, row, "Aktif Gönüllü", r.volunteers().active());
        row = statRow(s, row, "Görevli Gönüllü", r.volunteers().assigned());
        row = statRow(s, row, "Gönderilen Bildirim", r.communication().sent());
        row = statRow(s, row, "Okunan Bildirim", r.communication().read());
        autoSize(s, 2);
    }

    // ── Excel yardımcıları ────────────────────────────────────────────────────

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

    private int statRow(Sheet s, int row, String k, double v) {
        Row r = s.createRow(row);
        r.createCell(0).setCellValue(k);
        r.createCell(1).setCellValue(v);
        return row + 1;
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

    // ── Ortak yardımcılar ─────────────────────────────────────────────────────

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String num(long v) {
        return String.valueOf(v);
    }

    private static String pct(double v) {
        return String.format("%%%.0f", v);
    }

    private static String minutes(Long m) {
        return m == null ? "veri yok" : m + " dk";
    }
}
