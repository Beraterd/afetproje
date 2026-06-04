package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.AssemblyArea;
import com.afet.koordinasyon.domain.enums.EmergencyMessageType;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gerçek deprem ve simülasyon için ORTAK, standardize edilmiş e-posta içeriğini üretir.
 * Tek fark başlık/etikettir; gövde yapısı (büyüklük, konum, tarih, derinlik, toplanma
 * alanları, navigasyon linkleri, bilgilendirme metinleri) her iki kaynak için aynıdır.
 */
@Component
public class EarthquakeAlertEmailBuilder {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Istanbul"));

    static final String NO_AREAS_MESSAGE =
            "Kayıtlı mahalleniz için sistemde aktif/onaylı toplanma alanı bulunamadı.";

    /** Konu satırı. Simülasyon için "[SİMÜLASYON]" etiketi eklenir. */
    public String buildSubject(EarthquakeAlert alert) {
        return alert.isSimulation()
                ? "[SİMÜLASYON] Deprem Bildirimi — AFET Koordinasyon"
                : "Deprem Bildirimi — AFET Koordinasyon";
    }

    /**
     * Kullanıcıya özel HTML gövdeyi üretir.
     *
     * @param alert     ortak deprem verisi
     * @param fullName  alıcının adı soyadı
     * @param areas     kullanıcının mahallesindeki aktif/onaylı toplanma alanları (boş olabilir)
     */
    public String buildHtml(EarthquakeAlert alert, String fullName, List<AssemblyArea> areas) {
        return buildHtml(alert, fullName, areas, null);
    }

    /**
     * §8 — messageActionBaseUrl verilirse "Yakınlarıma Mesaj Gönder" bölümü ve hazır mesaj
     * butonları eklenir. Base URL formatı: {backend}/api/emergency-message/send?token=XYZ
     * (her butonda &type=ENUM eklenir). Token kullanıcıya özeldir ve e-posta başına üretilir.
     */
    public String buildHtml(EarthquakeAlert alert, String fullName, List<AssemblyArea> areas,
                            String messageActionBaseUrl) {
        boolean sim = alert.isSimulation();
        String heading = sim ? "Deprem Simülasyonu Bildirimi" : "Deprem Bildirimi";
        String simBadge = sim
                ? "<div style=\"background:#7c3aed;color:#fff;padding:6px 12px;border-radius:6px;"
                  + "display:inline-block;font-size:12px;font-weight:700;margin-bottom:12px;\">SİMÜLASYON</div>"
                : "";

        String magStr = String.format("%.1f", alert.magnitude() != null ? alert.magnitude() : 0.0);
        String location = escapeHtml(buildLocationString(alert));
        String dateTime = alert.occurredAt() != null ? DISPLAY_FMT.format(alert.occurredAt()) : "-";
        String depthRow = alert.depth() != null
                ? row(false, "Derinlik", escapeHtml(String.format("%.1f km", alert.depth())))
                : "";

        return "<!DOCTYPE html><html lang=\"tr\">"
            + "<head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"font-family:Arial,sans-serif;color:#333;max-width:600px;margin:0 auto;padding:24px;\">"

            + "<div style=\"background-color:" + (sim ? "#5b21b6" : "#1e3a5f") + ";padding:20px 24px;border-radius:8px 8px 0 0;\">"
            + "<h2 style=\"color:#ffffff;margin:0;font-size:20px;\">" + escapeHtml(heading) + "</h2>"
            + "<p style=\"color:#cbd5e1;margin:4px 0 0;\">AFET Koordinasyon Sistemi</p>"
            + "</div>"

            + "<div style=\"background:#ffffff;border:1px solid #e2e8f0;border-top:none;"
            +      "border-radius:0 0 8px 8px;padding:24px;\">"

            + simBadge
            + "<p style=\"margin:0 0 6px;\">Sayın <strong>" + escapeHtml(fullName) + "</strong>,</p>"
            + "<p style=\"margin:0 0 20px;\">"
            + (sim ? "Bir deprem simülasyonu başlatıldı." : "Yeni bir deprem kaydedildi.")
            + "</p>"

            + "<table style=\"border-collapse:collapse;width:100%;margin-bottom:24px;\">"
            + row(true,  "Büyüklük", "<strong style=\"font-size:18px;\">M" + magStr + "</strong>")
            + row(false, "Konum",    location)
            + row(true,  "Tarih",    escapeHtml(dateTime))
            + depthRow
            + "</table>"

            + buildAreasSection(areas)

            + buildEmergencyMessageSection(messageActionBaseUrl)

            + "<div style=\"background:#f1f5f9;border-radius:6px;padding:14px 18px;font-size:14px;color:#475569;margin-top:20px;\">"
            + "AFAD ve yetkili kurumların duyurularını takip edin."
            + "</div>"

            + "<hr style=\"margin:24px 0;border:none;border-top:1px solid #e2e8f0;\"/>"
            + "<p style=\"font-size:11px;color:#94a3b8;margin:0;\">"
            + "Bu bildirim afet koordinasyon platformu tarafından gönderilmiştir."
            + "</p>"
            + "</div>"
            + "</body></html>";
    }

    /**
     * §4/§8 — "Yakınlarıma Mesaj Gönder" bölümü. Her hazır mesaj, güvenli token taşıyan
     * tıklanabilir bir butondur. messageActionBaseUrl null ise bölüm hiç eklenmez.
     */
    private String buildEmergencyMessageSection(String messageActionBaseUrl) {
        if (messageActionBaseUrl == null || messageActionBaseUrl.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"margin-top:24px;padding:16px 18px;background:#fff7ed;border:1px solid #fed7aa;border-radius:8px;\">");
        sb.append("<p style=\"font-weight:700;margin:0 0 4px;color:#9a3412;font-size:15px;\">Yakınlarıma Mesaj Gönder</p>");
        sb.append("<p style=\"margin:0 0 14px;font-size:13px;color:#7c2d12;\">")
          .append("Aşağıdaki hazır mesajlardan birine tıklayarak kayıtlı yakınlarınıza (en fazla 3 kişi) durum bildirebilirsiniz.")
          .append("</p>");
        sb.append("<table style=\"border-collapse:collapse;width:100%;\"><tbody>");
        for (EmergencyMessageType type : EmergencyMessageType.values()) {
            String href = messageActionBaseUrl + "&type=" + type.name();
            sb.append("<tr><td style=\"padding:4px 0;\">")
              .append("<a href=\"").append(href).append("\" ")
              .append("style=\"display:block;text-align:center;background:#ea580c;color:#ffffff;")
              .append("text-decoration:none;padding:11px 16px;border-radius:6px;font-size:14px;font-weight:600;\">")
              .append(escapeHtml(type.getLabel()))
              .append("</a></td></tr>");
        }
        sb.append("</tbody></table>");
        sb.append("<p style=\"margin:12px 0 0;font-size:11px;color:#9a3412;\">")
          .append("Bu bağlantılar güvenlik amacıyla 24 saat geçerlidir ve yalnızca bir kez kullanılabilir.")
          .append("</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildAreasSection(List<AssemblyArea> areas) {
        if (areas == null || areas.isEmpty()) {
            return "<div style=\"background:#fef2f2;border:1px solid #fecaca;border-radius:6px;"
                 + "padding:14px 18px;color:#b91c1c;font-size:14px;\">"
                 + escapeHtml(NO_AREAS_MESSAGE)
                 + "</div>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<p style=\"font-weight:600;margin:0 0 8px;\">Mahallenizdeki Toplanma Alanları:</p>");
        sb.append("<ol style=\"line-height:1.6;padding-left:20px;margin:0;\">");
        for (AssemblyArea area : areas) {
            String navUrl = buildNavUrl(area);
            sb.append("<li style=\"margin-bottom:10px;\">");
            sb.append("<strong>").append(escapeHtml(area.getName())).append("</strong>");
            if (area.getAddress() != null && !area.getAddress().isBlank()) {
                sb.append("<br/><span style=\"color:#64748b;font-size:13px;\">")
                  .append(escapeHtml(area.getAddress())).append("</span>");
            }
            if (navUrl != null) {
                sb.append("<br/><a href=\"").append(navUrl).append("\" ")
                  .append("style=\"color:#2563eb;font-size:13px;\">Yol tarifi al (Google Haritalar)</a>");
            }
            sb.append("</li>");
        }
        sb.append("</ol>");
        return sb.toString();
    }

    /**
     * Google Maps navigasyon linki: googleMapsUrl varsa onu, yoksa lat/lon'dan üretir,
     * ikisi de yoksa null döner.
     */
    String buildNavUrl(AssemblyArea area) {
        if (area.getGoogleMapsUrl() != null && !area.getGoogleMapsUrl().isBlank()) {
            return area.getGoogleMapsUrl();
        }
        if (area.getLatitude() != null && area.getLongitude() != null) {
            return "https://www.google.com/maps/dir/?api=1&destination="
                    + area.getLatitude() + "," + area.getLongitude();
        }
        return null;
    }

    private String row(boolean shaded, String label, String valueHtml) {
        String bg = shaded ? "background-color:#f8fafc;" : "";
        return "<tr style=\"" + bg + "\">"
            + "<td style=\"padding:10px 16px;font-weight:600;border:1px solid #e2e8f0;"
            +      "width:120px;color:#64748b;font-size:13px;\">" + escapeHtml(label) + "</td>"
            + "<td style=\"padding:10px 16px;border:1px solid #e2e8f0;font-size:14px;\">"
            +      valueHtml + "</td>"
            + "</tr>";
    }

    private String buildLocationString(EarthquakeAlert alert) {
        if (alert.location() != null && !alert.location().isBlank()) {
            String detail = Stream.of(alert.province(), alert.district())
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(" / "));
            return detail.isBlank() ? alert.location() : alert.location() + " (" + detail + ")";
        }
        return Stream.of(alert.province(), alert.district())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
