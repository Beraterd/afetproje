package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.NotificationType;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.notification.EmailNotificationProvider;
import com.afet.koordinasyon.repository.EarthquakeEventRepository;
import com.afet.koordinasyon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AfadEarthquakeNotificationService {

    private final EarthquakeEventRepository repository;
    private final UserRepository userRepository;
    private final EmailNotificationProvider emailNotificationProvider;
    private final NotificationService notificationService;

    // ── Config: notifications.earthquake.* ────────────────────────────────────

    @Value("${notifications.earthquake.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${notifications.earthquake.min-magnitude:2.5}")
    private double minMagnitude;

    @Value("${notifications.earthquake.recipient-roles:ADMIN,DISTRICT_COORDINATOR,NEIGHBORHOOD_COORDINATOR}")
    private List<String> recipientRoles;

    // ── Config: app.afad.* (test mode unchanged) ──────────────────────────────

    @Value("${app.afad.email-test-mode:false}")
    private boolean emailTestMode;

    @Value("${app.afad.email-test-recipient:}")
    private String testRecipient;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Istanbul"));

    // ── Event listener ────────────────────────────────────────────────────────

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNewEarthquake(AfadNewEarthquakeEvent event) {
        if (!notificationsEnabled) {
            log.debug("Earthquake notifications disabled, skipping");
            return;
        }

        EarthquakeEvent eq = repository.findById(event.earthquakeEventId()).orElse(null);
        if (eq == null) {
            log.warn("Earthquake alert: event not found id={}", event.earthquakeEventId());
            return;
        }

        // Duplicate guard
        if (eq.isNotificationSent()) {
            log.info("Earthquake alert skipped: already processed externalId={}", eq.getExternalId());
            return;
        }

        // Magnitude threshold
        double mag = eq.getMagnitude() != null ? eq.getMagnitude() : 0.0;
        if (mag < minMagnitude) {
            log.info("Earthquake alert skipped: magnitude={} < threshold={}", mag, minMagnitude);
            return;
        }

        // Resolve recipients
        List<User> recipients = resolveRecipients();
        if (recipients.isEmpty()) {
            log.warn("Earthquake alert: no eligible recipients found (externalId={})", eq.getExternalId());
            return;
        }

        String location = buildLocationString(eq);
        log.info("Earthquake alert triggered: externalId={} magnitude={} location={} recipients={}",
                eq.getExternalId(), mag, location, recipients.size());

        if (emailTestMode) {
            log.info("Earthquake alert test mode — sending only to: {}", testRecipient);
            if (testRecipient != null && !testRecipient.isBlank()) {
                sendEmail(testRecipient, eq);
            } else {
                log.warn("Earthquake alert test mode: no test recipient configured, skipping (externalId={})",
                        eq.getExternalId());
            }
            return;
        }

        int successCount = 0;
        for (User user : recipients) {
            if (sendEmail(user.getEmail(), eq)) {
                successCount++;
                log.info("Earthquake alert email sent: recipient={} externalId={}",
                        user.getEmail(), eq.getExternalId());
            }
        }

        if (successCount > 0) {
            markNotificationSent(eq);
            String deliveryMsg = String.format("M%.1f depremi için %d/%d alıcıya e-posta gönderildi.",
                    mag, successCount, recipients.size());
            notificationService.createForAdmins(
                    NotificationType.MESSAGE_DELIVERY_STATUS,
                    "Deprem E-posta Teslim Raporu",
                    deliveryMsg,
                    "EarthquakeEvent", eq.getId().toString());
        } else {
            log.warn("Earthquake alert: no emails delivered (externalId={})", eq.getExternalId());
        }
    }

    // ── Recipient resolution ──────────────────────────────────────────────────

    private List<User> resolveRecipients() {
        List<UserRole> roles = recipientRoles.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try { return UserRole.valueOf(s); }
                    catch (IllegalArgumentException e) {
                        log.warn("Earthquake alert: unknown role '{}' in config, skipping", s);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (roles.isEmpty()) {
            return userRepository.findActiveVerifiedWithNeighborhood();
        }
        return userRepository.findActiveVerifiedWithRoles(roles);
    }

    // ── Email dispatch ────────────────────────────────────────────────────────

    private boolean sendEmail(String address, EarthquakeEvent eq) {
        try {
            emailNotificationProvider.send(address,
                    "Deprem Bildirimi — AFET Koordinasyon Sistemi",
                    buildHtmlBody(eq));
            return true;
        } catch (Exception e) {
            log.error("Earthquake alert email failed: recipient={} externalId={} error={}",
                    address, eq.getExternalId(), e.getMessage());
            return false;
        }
    }

    private void markNotificationSent(EarthquakeEvent eq) {
        eq.setNotificationSent(true);
        eq.setNotificationSentAt(OffsetDateTime.now());
        repository.save(eq);
    }

    // ── HTML body ─────────────────────────────────────────────────────────────

    private String buildHtmlBody(EarthquakeEvent eq) {
        double mag        = eq.getMagnitude() != null ? eq.getMagnitude() : 0.0;
        String magStr     = String.format("%.1f", mag);
        String location   = escapeHtml(buildLocationString(eq));
        String dateTime   = eq.getEventTime() != null ? DISPLAY_FMT.format(eq.getEventTime()) : "-";
        String depth      = eq.getDepth() != null ? String.format("%.1f km", eq.getDepth()) : "-";
        String badgeColor = magnitudeColor(mag);
        String badgeLabel = magnitudeLabel(mag);

        return "<!DOCTYPE html><html lang=\"tr\">"
            + "<head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"font-family:Arial,sans-serif;color:#333;max-width:600px;margin:0 auto;padding:24px;\">"

            + "<div style=\"background-color:#1e3a5f;padding:20px 24px;border-radius:8px 8px 0 0;\">"
            + "<h2 style=\"color:#ffffff;margin:0;font-size:20px;\">Deprem Bildirimi</h2>"
            + "<p style=\"color:#94a3b8;margin:4px 0 0;\">AFET Koordinasyon Sistemi</p>"
            + "</div>"

            + "<div style=\"background:#ffffff;border:1px solid #e2e8f0;border-top:none;"
            +      "border-radius:0 0 8px 8px;padding:24px;\">"

            + "<p style=\"margin:0 0 20px;\">Yeni bir deprem kaydedildi.</p>"

            + "<table style=\"border-collapse:collapse;width:100%;margin-bottom:24px;\">"
            + row(true,  "Büyüklük",
                  "<span style=\"display:inline-flex;align-items:center;gap:8px;\">"
                + "<span style=\"font-size:22px;font-weight:700;color:#1e293b;\">" + magStr + "</span>"
                + "<span style=\"padding:3px 10px;border-radius:12px;font-size:12px;font-weight:600;"
                +       "background-color:" + badgeColor + ";color:#fff;\">" + badgeLabel + "</span>"
                + "</span>")
            + row(false, "Konum",    location)
            + row(true,  "Tarih",    escapeHtml(dateTime))
            + row(false, "Derinlik", escapeHtml(depth))
            + "</table>"

            + "<div style=\"background:#f1f5f9;border-radius:6px;padding:14px 18px;font-size:14px;color:#475569;\">"
            + "AFET Koordinasyon Sistemine giriş yaparak detayları inceleyebilirsiniz."
            + "</div>"

            + "<hr style=\"margin:24px 0;border:none;border-top:1px solid #e2e8f0;\"/>"
            + "<p style=\"font-size:11px;color:#94a3b8;margin:0;\">"
            + "Bu bildirim AFET Koordinasyon Sistemi tarafından otomatik olarak gönderilmiştir."
            + "</p>"
            + "</div>"
            + "</body></html>";
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String magnitudeColor(double mag) {
        if (mag >= 6.0) return "#dc2626";   // red
        if (mag >= 4.0) return "#f97316";   // orange
        return "#eab308";                   // yellow
    }

    private String magnitudeLabel(double mag) {
        if (mag >= 6.0) return "Büyük";
        if (mag >= 4.0) return "Orta";
        return "Küçük";
    }

    private String buildLocationString(EarthquakeEvent eq) {
        if (eq.getLocation() != null && !eq.getLocation().isBlank()) {
            String detail = Stream.of(eq.getProvince(), eq.getDistrict())
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(" / "));
            return detail.isBlank() ? eq.getLocation() : eq.getLocation() + " (" + detail + ")";
        }
        return Stream.of(eq.getProvince(), eq.getDistrict())
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
