package com.afet.koordinasyon.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /**
     * Her toplanma alanı için ad ve Google Maps bağlantısını taşır.
     */
    public record AssemblyAreaInfo(String name, String googleMapsUrl) {}

    public void sendSimulationNotification(String to, String firstName, String lastName,
                                           String neighborhoodName,
                                           List<AssemblyAreaInfo> areas) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("AFET KOORDİNASYON - Deprem Simülasyonu Bildirimi");
            helper.setText(buildHtmlBody(firstName, lastName, neighborhoodName, areas), true);

            mailSender.send(message);
            log.info("Simulation notification sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send simulation notification to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    private String buildHtmlBody(String firstName, String lastName,
                                 String neighborhoodName,
                                 List<AssemblyAreaInfo> areas) {
        String neighborhoodLine = (neighborhoodName != null && !neighborhoodName.isBlank())
                ? "<p>Kayıtlı mahalleniz: <strong>" + escapeHtml(neighborhoodName) + "</strong></p>"
                : "";

        String areaSection;
        if (areas == null || areas.isEmpty()) {
            areaSection = "<p style=\"color:#c0392b;\">Mahalleniz için tanımlanmış bir toplanma alanı bulunmamaktadır. "
                    + "Lütfen yetkilileri takip edin.</p>";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("<p><strong>Mahallenize ait toplanma alanları:</strong></p>");
            sb.append("<ol style=\"line-height:2;\">");
            for (AssemblyAreaInfo area : areas) {
                sb.append("<li>");
                sb.append("<strong>").append(escapeHtml(area.name())).append("</strong><br/>");
                sb.append("Google Maps: <a href=\"")
                  .append(area.googleMapsUrl())
                  .append("\">")
                  .append(area.googleMapsUrl())
                  .append("</a>");
                sb.append("</li>");
            }
            sb.append("</ol>");
            areaSection = sb.toString();
        }

        return """
                <!DOCTYPE html>
                <html lang="tr">
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto; padding: 24px;">
                  <h2 style="color: #c0392b;">⚠️ Deprem Simülasyonu Bildirimi</h2>
                  <p>Sayın <strong>%s %s</strong>,</p>
                  %s
                  <p>İstanbul Afet Koordinasyon Sistemi kapsamında bir deprem simülasyonu başlatılmıştır.</p>
                  %s
                  <hr style="margin-top: 32px;"/>
                  <p style="font-size: 12px; color: #888;">Bu e-posta otomatik olarak gönderilmiştir. Lütfen yanıtlamayınız.</p>
                </body>
                </html>
                """.formatted(firstName, lastName, neighborhoodLine, areaSection);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
