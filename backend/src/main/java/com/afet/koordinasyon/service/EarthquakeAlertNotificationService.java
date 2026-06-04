package com.afet.koordinasyon.service;

import com.afet.koordinasyon.config.WhatsAppProperties;
import com.afet.koordinasyon.domain.entity.AssemblyArea;
import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.entity.EarthquakeSimulation;
import com.afet.koordinasyon.domain.entity.SimulationNotificationLog;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.NotificationStatus;
import com.afet.koordinasyon.domain.enums.NotificationType;
import com.afet.koordinasyon.domain.enums.SimulationEmailStatus;
import com.afet.koordinasyon.notification.EmailNotificationProvider;
import com.afet.koordinasyon.notification.WhatsAppNotificationProvider;
import com.afet.koordinasyon.repository.AssemblyAreaRepository;
import com.afet.koordinasyon.repository.EarthquakeEventRepository;
import com.afet.koordinasyon.repository.EarthquakeSimulationRepository;
import com.afet.koordinasyon.repository.SimulationNotificationLogRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.util.PhoneNumberUtil;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Gerçek deprem ve simülasyon için ORTAK bildirim akışı (e-posta + WhatsApp + kısa link).
 * <p>
 * Hem {@code EarthquakeEventService} (gerçek AFAD depremi) hem de {@code SimulationService}
 * (admin simülasyonu) tek bir {@link EarthquakeAlertCreatedEvent} publish eder ve bu servis
 * üzerinden geçer. Böylece iki kaynak da birebir aynı içerik ve yan etkileri üretir;
 * yalnızca başlık/log tarafında "[SİMÜLASYON]" ayrımı yapılır.
 * <p>
 * Kanallar bağımsızdır: WhatsApp başarısız olsa da e-posta gönderimi engellenmez, tersi de geçerlidir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EarthquakeAlertNotificationService {

    private final EarthquakeEventRepository earthquakeEventRepository;
    private final EarthquakeSimulationRepository simulationRepository;
    private final SimulationNotificationLogRepository simulationLogRepository;
    private final UserRepository userRepository;
    private final AssemblyAreaRepository assemblyAreaRepository;
    private final EmailNotificationProvider emailProvider;
    private final WhatsAppNotificationProvider whatsAppProvider;
    private final WhatsAppProperties whatsAppProperties;
    private final ShortLinkService shortLinkService;
    private final EarthquakeAlertEmailBuilder emailBuilder;
    private final NotificationService notificationService;
    private final EmergencyMessageTokenService emergencyMessageTokenService;

    @Value("${notifications.earthquake.enabled:true}")
    private boolean earthquakeNotificationsEnabled;

    @Value("${app.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${notifications.earthquake.min-magnitude:2.5}")
    private double minMagnitude;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Istanbul"));

    // ── Ortak giriş noktası ─────────────────────────────────────────────────

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAlert(EarthquakeAlertCreatedEvent event) {
        EarthquakeAlert alert = event.alert();
        log.info("Deprem bildirim akışı başlıyor: source={}, id={}, M={}",
                alert.sourceType(), alert.alertId(), alert.magnitude());

        // 1) E-POSTA — kaynağa göre alıcı çözümü/sonuç kaydı farklı; içerik aynı.
        try {
            if (alert.isSimulation()) {
                sendSimulationEmails(alert);
            } else {
                sendRealEarthquakeEmails(alert);
            }
        } catch (Exception e) {
            log.error("Deprem bildirimi e-posta aşaması hatası (id={}): {}", alert.alertId(), e.getMessage());
        }

        // 2) WHATSAPP — e-posta aşamasından bağımsız çalışır.
        try {
            sendWhatsAppAlerts(alert);
        } catch (Exception e) {
            log.error("Deprem bildirimi WhatsApp aşaması hatası (id={}): {}", alert.alertId(), e.getMessage());
        }
    }

    // ── E-posta: gerçek deprem ───────────────────────────────────────────────

    private void sendRealEarthquakeEmails(EarthquakeAlert alert) {
        if (!earthquakeNotificationsEnabled) {
            log.debug("Deprem e-posta bildirimleri devre dışı (notifications.earthquake.enabled=false).");
            return;
        }

        double mag = alert.magnitude() != null ? alert.magnitude() : 0.0;
        if (mag < minMagnitude) {
            log.info("Deprem e-posta atlandı: M{} < eşik {}", mag, minMagnitude);
            return;
        }

        EarthquakeEvent eq = earthquakeEventRepository.findById(alert.alertId()).orElse(null);
        if (eq != null && eq.isNotificationSent()) {
            log.info("Deprem e-posta atlandı: zaten gönderilmiş (id={})", alert.alertId());
            return;
        }

        List<User> recipients = userRepository.findActiveVerifiedWithNeighborhood();
        int sent = 0;
        for (User user : recipients) {
            try {
                sendAlertEmail(alert, user);
                sent++;
            } catch (Exception e) {
                log.error("Deprem e-postası gönderilemedi {}: {}", user.getEmail(), e.getMessage());
            }
        }

        if (sent > 0 && eq != null) {
            eq.setNotificationSent(true);
            eq.setNotificationSentAt(OffsetDateTime.now());
            earthquakeEventRepository.save(eq);
            notificationService.createForAdmins(
                    NotificationType.MESSAGE_DELIVERY_STATUS,
                    "Deprem E-posta Teslim Raporu",
                    String.format("M%.1f depremi için %d/%d alıcıya e-posta gönderildi.",
                            mag, sent, recipients.size()),
                    "EarthquakeEvent", alert.alertId().toString());
        }
        log.info("Gerçek deprem e-posta bildirimi tamamlandı: gönderilen={}/{}", sent, recipients.size());
    }

    // ── E-posta: simülasyon (QUEUED loglar üzerinden) ─────────────────────────

    private void sendSimulationEmails(EarthquakeAlert alert) {
        List<SimulationNotificationLog> logs =
                simulationLogRepository.findBySimulationIdAndStatusUnpaged(
                        alert.alertId(), NotificationStatus.QUEUED);

        int sent = 0;
        int failed = 0;
        for (SimulationNotificationLog logEntry : logs) {
            User user = logEntry.getUser();
            try {
                sendAlertEmail(alert, user);
                logEntry.setStatus(NotificationStatus.SENT);
                logEntry.setSentAt(OffsetDateTime.now());
                logEntry.setLastError(null);
                sent++;
            } catch (Exception e) {
                logEntry.setStatus(NotificationStatus.FAILED);
                logEntry.setSentAt(null);
                logEntry.setLastError(e.getMessage());
                failed++;
                log.error("Simülasyon e-postası gönderilemedi {}: {}", user.getEmail(), e.getMessage());
            }
            simulationLogRepository.save(logEntry);
        }

        SimulationEmailStatus finalStatus = (failed == 0)
                ? SimulationEmailStatus.COMPLETED
                : SimulationEmailStatus.PARTIAL_FAILURE;
        simulationRepository.findById(alert.alertId()).ifPresent(sim -> {
            sim.setEmailStatus(finalStatus);
            simulationRepository.save(sim);
        });

        log.info("Simülasyon e-posta bildirimi tamamlandı: gönderilen={}, başarısız={}, durum={}",
                sent, failed, finalStatus);
    }

    /** Tek bir kullanıcıya standardize edilmiş deprem e-postası gönderir. Başarısızsa exception fırlatır. */
    private void sendAlertEmail(EarthquakeAlert alert, User user) {
        List<AssemblyArea> areas = loadAreasForUser(user);
        String fullName = user.getFirstName() + " " + user.getLastName();
        // §4/§6/§8 — Her e-posta için kullanıcıya özel, süreli, tek kullanımlık güvenli token üretilir.
        // Ham token YALNIZCA e-posta linkine yazılır; loglanmaz. Token üretimi başarısız olsa bile
        // e-posta gönderimi engellenmez (butonsuz gönderilir).
        String messageActionBaseUrl = buildMessageActionBaseUrl(user, alert);
        emailProvider.send(
                user.getEmail(),
                emailBuilder.buildSubject(alert),
                emailBuilder.buildHtml(alert, fullName, areas, messageActionBaseUrl));
    }

    /**
     * "Yakınlarıma Mesaj Gönder" butonlarının taban URL'ini üretir:
     * {backend}/api/emergency-message/send?token=XYZ (her butonda &type=ENUM eklenir).
     * Token üretilemezse null döner → e-postada buton bölümü yer almaz.
     */
    private String buildMessageActionBaseUrl(User user, EarthquakeAlert alert) {
        try {
            String rawToken = emergencyMessageTokenService.issueToken(user, alert.alertId());
            if (rawToken == null || rawToken.isBlank()) return null;
            String base = backendBaseUrl != null ? backendBaseUrl.replaceAll("/+$", "") : "";
            return base + "/api/emergency-message/send?token=" + rawToken;
        } catch (Exception e) {
            log.warn("Acil mesaj token üretilemedi (userId={}): {}", user.getId(), e.getMessage());
            return null;
        }
    }

    private List<AssemblyArea> loadAreasForUser(User user) {
        if (user.getNeighborhood() == null) {
            return Collections.emptyList();
        }
        try {
            return assemblyAreaRepository.findActiveApprovedByNeighborhoodId(user.getNeighborhood().getId());
        } catch (Exception e) {
            log.warn("Toplanma alanları yüklenemedi (userId={}): {}", user.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── WhatsApp (gerçek + simülasyon ortak) ─────────────────────────────────

    private void sendWhatsAppAlerts(EarthquakeAlert alert) {
        if (!whatsAppProperties.isEnabled()) {
            log.debug("WhatsApp bildirimleri devre dışı (app.whatsapp.enabled=false), atlanıyor.");
            return;
        }

        List<User> users = userRepository.findWhatsappEligibleUsers();
        if (users.isEmpty()) {
            log.info("Deprem WhatsApp bildirimi: uygun kullanıcı bulunamadı (id={})", alert.alertId());
            return;
        }

        // Deprem/simülasyon broadcast bildirimleri YALNIZCA approved template ile gönderilir.
        // Meta, 24 saatlik servis penceresi dışındaki custom text mesajlarını sessizce düşürebildiği
        // için text fallback KULLANILMAZ. (Text yalnızca yakınlara durum mesajında kullanılır.)
        String templateName = whatsAppProperties.getEarthquakeTemplateName();
        if (templateName == null || templateName.isBlank()) {
            log.error("WhatsApp template name is required for earthquake alerts.");
            return;
        }

        // hello_world gibi parametresiz template'ler gövde parametresi olmadan gönderilir.
        boolean parameterless = isParameterlessTemplate(templateName);

        int sent = 0;
        for (User user : users) {
            String maskedPhone = PhoneNumberUtil.mask(user.getPhone());
            try {
                List<String> params;
                if (parameterless) {
                    params = List.of();
                } else {
                    String shortLink = shortLinkService.createShortLink(
                            buildAssemblyAreasUrl(alert), user.getId(), earthquakeIdForTracking(alert), null);
                    params = buildTemplateParams(alert, shortLink);
                }
                whatsAppProvider.sendTemplate(user.getPhone(), templateName, params);
                sent++;
            } catch (Exception e) {
                log.error("Deprem WhatsApp gönderilemedi: id={}, alıcı={}, hata={}",
                        alert.alertId(), maskedPhone, e.getMessage());
            }
        }
        log.info("Deprem WhatsApp bildirimi tamamlandı: source={}, messageType=TEMPLATE, template={}, gönderilen={}/{}",
                alert.sourceType(), templateName, sent, users.size());
    }

    /** Gövde parametresi gerektirmeyen template'ler (örn. Meta'nın hazır hello_world'ü). */
    private boolean isParameterlessTemplate(String templateName) {
        return "hello_world".equalsIgnoreCase(templateName);
    }

    /** Kısa linkin yönlendireceği frontend adresi (kaynağa göre query param farklı, sayfa aynı). */
    private String buildAssemblyAreasUrl(EarthquakeAlert alert) {
        String param = alert.isSimulation()
                ? "?simulationId=" + alert.alertId()
                : "?earthquakeId=" + alert.alertId();
        return shortLinkService.buildFrontendUrl("/emergency/assembly-areas" + param);
    }

    /** ShortLink izleme alanı yalnızca gerçek deprem için doldurulur; simülasyon için null. */
    private UUID earthquakeIdForTracking(EarthquakeAlert alert) {
        return alert.isSimulation() ? null : alert.alertId();
    }

    private List<String> buildTemplateParams(EarthquakeAlert alert, String shortLink) {
        return List.of(
                formatMagnitude(alert),
                safeLocation(alert),
                formatDate(alert),
                shortLink);
    }

    private String formatMagnitude(EarthquakeAlert alert) {
        return String.format("%.1f", alert.magnitude() != null ? alert.magnitude() : 0.0);
    }

    private String safeLocation(EarthquakeAlert alert) {
        return (alert.location() != null && !alert.location().isBlank()) ? alert.location() : "-";
    }

    private String formatDate(EarthquakeAlert alert) {
        return alert.occurredAt() != null ? DISPLAY_FMT.format(alert.occurredAt()) : "-";
    }
}
