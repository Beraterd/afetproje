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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Gerçek deprem ve simülasyon için ORTAK bildirim akışı (e-posta + WhatsApp + kısa link).
 * <p>
 * E-posta gönderimi paralel yapılır: her kullanıcı için ayrı bir CompletableFuture
 * emailTaskExecutor üzerinde çalışır; bir kullanıcının hatası diğerlerini bloklamaz.
 */
@Service
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
    private final TaskExecutor emailTaskExecutor;

    public EarthquakeAlertNotificationService(
            EarthquakeEventRepository earthquakeEventRepository,
            EarthquakeSimulationRepository simulationRepository,
            SimulationNotificationLogRepository simulationLogRepository,
            UserRepository userRepository,
            AssemblyAreaRepository assemblyAreaRepository,
            EmailNotificationProvider emailProvider,
            WhatsAppNotificationProvider whatsAppProvider,
            WhatsAppProperties whatsAppProperties,
            ShortLinkService shortLinkService,
            EarthquakeAlertEmailBuilder emailBuilder,
            NotificationService notificationService,
            EmergencyMessageTokenService emergencyMessageTokenService,
            @Qualifier("emailTaskExecutor") TaskExecutor emailTaskExecutor) {
        this.earthquakeEventRepository = earthquakeEventRepository;
        this.simulationRepository = simulationRepository;
        this.simulationLogRepository = simulationLogRepository;
        this.userRepository = userRepository;
        this.assemblyAreaRepository = assemblyAreaRepository;
        this.emailProvider = emailProvider;
        this.whatsAppProvider = whatsAppProvider;
        this.whatsAppProperties = whatsAppProperties;
        this.shortLinkService = shortLinkService;
        this.emailBuilder = emailBuilder;
        this.notificationService = notificationService;
        this.emergencyMessageTokenService = emergencyMessageTokenService;
        this.emailTaskExecutor = emailTaskExecutor;
    }

    @Value("${notifications.earthquake.enabled:true}")
    private boolean earthquakeNotificationsEnabled;

    @Value("${app.base-url}")
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

        try {
            if (alert.isSimulation()) {
                sendSimulationEmails(alert);
            } else {
                sendRealEarthquakeEmails(alert);
            }
        } catch (Exception e) {
            log.error("Deprem bildirimi e-posta aşaması hatası (id={}): {}", alert.alertId(), e.getMessage());
        }

        try {
            sendWhatsAppAlerts(alert);
        } catch (Exception e) {
            log.error("Deprem bildirimi WhatsApp aşaması hatası (id={}): {}", alert.alertId(), e.getMessage());
        }
    }

    // ── E-posta: gerçek deprem (paralel) ────────────────────────────────────

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

        List<User> recipients = userRepository.findActiveWithNeighborhood();
        if (recipients.isEmpty()) {
            log.info("Deprem e-posta atlandı: aktif kullanıcı bulunamadı (id={})", alert.alertId());
            return;
        }

        // Paralel gönderim: her kullanıcı için ayrı CompletableFuture
        List<CompletableFuture<Boolean>> futures = new ArrayList<>(recipients.size());
        for (User user : recipients) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    sendAlertEmail(alert, user);
                    return true;
                } catch (Exception e) {
                    log.error("Deprem e-postası gönderilemedi {}: {}", user.getEmail(), e.getMessage());
                    return false;
                }
            }, emailTaskExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long sent = futures.stream().mapToLong(f -> {
            try { return Boolean.TRUE.equals(f.get()) ? 1L : 0L; } catch (Exception e) { return 0L; }
        }).sum();

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

    // ── E-posta: simülasyon (paralel, log güncellemeli) ──────────────────────

    private void sendSimulationEmails(EarthquakeAlert alert) {
        List<SimulationNotificationLog> logs =
                simulationLogRepository.findBySimulationIdAndStatusUnpaged(
                        alert.alertId(), NotificationStatus.QUEUED);

        if (logs.isEmpty()) {
            log.info("Simülasyon e-posta atlandı: QUEUED kayıt bulunamadı (id={})", alert.alertId());
            return;
        }

        // Paralel gönderim: her log kaydı için ayrı CompletableFuture
        List<CompletableFuture<SimulationNotificationLog>> futures = new ArrayList<>(logs.size());
        for (SimulationNotificationLog logEntry : logs) {
            User user = logEntry.getUser();
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    sendAlertEmail(alert, user);
                    logEntry.setStatus(NotificationStatus.SENT);
                    logEntry.setSentAt(OffsetDateTime.now());
                    logEntry.setLastError(null);
                } catch (Exception e) {
                    logEntry.setStatus(NotificationStatus.FAILED);
                    logEntry.setSentAt(null);
                    logEntry.setLastError(e.getMessage());
                    log.error("Simülasyon e-postası gönderilemedi {}: {}", user.getEmail(), e.getMessage());
                }
                return logEntry;
            }, emailTaskExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<SimulationNotificationLog> completed = futures.stream()
                .map(f -> { try { return f.get(); } catch (Exception e) { return null; } })
                .filter(Objects::nonNull)
                .toList();

        simulationLogRepository.saveAll(completed);

        long failed = completed.stream()
                .filter(l -> l.getStatus() == NotificationStatus.FAILED).count();
        long sent = completed.size() - failed;

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
        String messageActionBaseUrl = buildMessageActionBaseUrl(user, alert);
        emailProvider.send(
                user.getEmail(),
                emailBuilder.buildSubject(alert),
                emailBuilder.buildHtml(alert, fullName, areas, messageActionBaseUrl));
    }

    private String buildMessageActionBaseUrl(User user, EarthquakeAlert alert) {
        try {
            String rawToken = emergencyMessageTokenService.issueToken(user, alert.alertId());
            if (rawToken == null || rawToken.isBlank()) return null;
            return shortLinkService.buildFrontendUrl("/emergency-status/" + rawToken);
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

        String templateName = whatsAppProperties.getEarthquakeTemplateName();
        if (templateName == null || templateName.isBlank()) {
            log.error("WhatsApp template name is required for earthquake alerts.");
            return;
        }

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

    private boolean isParameterlessTemplate(String templateName) {
        return "hello_world".equalsIgnoreCase(templateName);
    }

    private String buildAssemblyAreasUrl(EarthquakeAlert alert) {
        String param = alert.isSimulation()
                ? "?simulationId=" + alert.alertId()
                : "?earthquakeId=" + alert.alertId();
        return shortLinkService.buildFrontendUrl("/emergency/assembly-areas" + param);
    }

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
