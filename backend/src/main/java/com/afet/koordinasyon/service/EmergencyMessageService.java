package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.AssemblyArea;
import com.afet.koordinasyon.domain.entity.EmergencyContact;
import com.afet.koordinasyon.domain.entity.EmergencyContactMessageLog;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.domain.enums.EmergencyMessageType;
import com.afet.koordinasyon.domain.enums.NotificationType;
import com.afet.koordinasyon.notification.EmailNotificationProvider;
import com.afet.koordinasyon.repository.AssemblyAreaRepository;
import com.afet.koordinasyon.repository.EmergencyContactMessageLogRepository;
import com.afet.koordinasyon.repository.EmergencyContactRepository;
import com.afet.koordinasyon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * §4-9 — Deprem e-postasındaki hazır mesaj linkine tıklanınca, kullanıcının KENDİ yakınlarına
 * (en fazla 3) durum mesajı gönderir. Tüm yetki kontrolü token üzerinden backend'de yapılır:
 * token hangi kullanıcıya aitse YALNIZCA onun yakınlarına mesaj gider.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyMessageService {

    private static final int MAX_RECIPIENTS = 3;
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Istanbul"));

    private final EmergencyMessageTokenService tokenService;
    private final EmergencyContactRepository emergencyContactRepository;
    private final EmergencyContactMessageLogRepository messageLogRepository;
    private final AssemblyAreaRepository assemblyAreaRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailNotificationProvider emailProvider;
    private final AuditLogService auditLogService;

    public enum Result { SUCCESS, NO_CONTACTS, EXPIRED, INVALID, ERROR }

    public record SendResult(Result result, int sentCount) {}

    /**
     * E-posta linkinden gelen isteği işler. type yalnızca enum üzerinden kabul edilir
     * (serbest metin reddedilir). Token tek kullanımlık + süreli + kullanıcıya bağlıdır.
     */
    @Transactional
    public SendResult sendStatusMessage(String rawToken, String typeParam) {
        EmergencyMessageType type;
        try {
            type = EmergencyMessageType.fromParam(typeParam);
        } catch (Exception e) {
            log.warn("Acil mesaj: geçersiz mesaj tipi parametresi");
            return new SendResult(Result.INVALID, 0);
        }

        EmergencyMessageTokenService.ConsumeResult consume = tokenService.consume(rawToken, type);
        switch (consume.outcome()) {
            case EXPIRED -> { return new SendResult(Result.EXPIRED, 0); }
            case INVALID -> { return new SendResult(Result.INVALID, 0); }
            default -> { /* VALID — devam */ }
        }

        // Managed entity'ye eriş (lazy alanlar için aktif transaction).
        User sender = userRepository.findById(consume.user().getId()).orElse(null);
        if (sender == null) {
            log.warn("Acil mesaj: gönderen kullanıcı bulunamadı id={}", consume.user().getId());
            return new SendResult(Result.ERROR, 0);
        }

        // §4/§5 — YALNIZCA kullanıcının kendi yakınları; öncelik alanı yok → createdAt sırası; ilk 3.
        List<EmergencyContact> contacts = emergencyContactRepository
                .findByOwnerIdOrderByCreatedAtAsc(sender.getId());
        if (contacts.isEmpty()) {
            log.info("Acil mesaj: kullanıcının kayıtlı yakını yok (userId={})", sender.getId());
            auditLogService.logSystemAction(AuditActionType.EMERGENCY_MESSAGE_SENT, "EmergencyMessage", null,
                    "Acil durum mesajı isteği — kayıtlı yakın yok",
                    Map.of("senderUserId", sender.getId().toString(), "messageType", type.name(), "recipients", 0));
            return new SendResult(Result.NO_CONTACTS, 0);
        }
        List<EmergencyContact> targets = contacts.size() > MAX_RECIPIENTS
                ? contacts.subList(0, MAX_RECIPIENTS) : contacts;

        String messageText = buildMessageText(sender, type);
        String emailSubject = "Afet Durum Bildirimi — " + fullName(sender);
        String emailHtml = buildMessageHtml(sender, type, messageText);

        int sent = 0;
        for (EmergencyContact contact : targets) {
            User recipient = contact.getContact();
            if (recipient == null) continue;
            boolean delivered = false;

            // 1) Sistem içi bildirim (her zaman mevcut kanal)
            try {
                notificationService.createForUser(recipient, NotificationType.EMERGENCY_CONTACT_MESSAGE,
                        fullName(sender) + " size afet durum bildirimi gönderdi", messageText,
                        "EmergencyMessage", sender.getId().toString());
                logMessage(sender, recipient, type, messageText, "IN_APP", "SENT", null);
                delivered = true;
            } catch (Exception e) {
                log.error("Acil mesaj sistem-içi bildirimi başarısız (recipientId={}): {}", recipient.getId(), e.getMessage());
                logMessage(sender, recipient, type, messageText, "IN_APP", "FAILED", e.getMessage());
            }

            // 2) E-posta (adres varsa)
            if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
                try {
                    emailProvider.send(recipient.getEmail(), emailSubject, emailHtml);
                    logMessage(sender, recipient, type, messageText, "EMAIL", "SENT", null);
                    delivered = true;
                } catch (Exception e) {
                    log.error("Acil mesaj e-postası başarısız (recipientId={}): {}", recipient.getId(), e.getMessage());
                    logMessage(sender, recipient, type, messageText, "EMAIL", "FAILED", e.getMessage());
                }
            }

            if (delivered) sent++;
        }

        auditLogService.logSystemAction(AuditActionType.EMERGENCY_MESSAGE_SENT, "EmergencyMessage", null,
                "Acil durum mesajı yakınlara gönderildi",
                Map.of("senderUserId", sender.getId().toString(),
                        "messageType", type.name(),
                        "recipients", sent));

        log.info("Acil mesaj gönderildi: senderId={}, type={}, alıcı={}/{}",
                sender.getId(), type.name(), sent, targets.size());
        return new SendResult(Result.SUCCESS, sent);
    }

    // ── İçerik üretimi (§7) ───────────────────────────────────────────────────

    private String buildMessageText(User sender, EmergencyMessageType type) {
        StringBuilder sb = new StringBuilder();
        sb.append(fullName(sender)).append(" size afet durum bildirimi gönderdi.\n\n");
        sb.append("Mesaj: ").append(type.getMessageText()).append("\n\n");
        sb.append("Tarih: ").append(DISPLAY_FMT.format(OffsetDateTime.now())).append("\n");

        String lastArea = lastKnownArea(sender);
        if (lastArea != null) {
            sb.append("Son bilinen bölge: ").append(lastArea).append("\n");
        }
        String assembly = nearestAssemblyArea(sender);
        if (assembly != null) {
            sb.append("Varsa en yakın toplanma alanı: ").append(assembly).append("\n");
        }
        sb.append("\nBu mesaj AFET Yönetim Sistemi üzerinden gönderilmiştir.");
        return sb.toString();
    }

    private String buildMessageHtml(User sender, EmergencyMessageType type, String plainText) {
        String assembly = nearestAssemblyArea(sender);
        String area = lastKnownArea(sender);
        return "<!DOCTYPE html><html lang=\"tr\"><head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"font-family:Arial,sans-serif;color:#333;max-width:600px;margin:0 auto;padding:24px;\">"
            + "<div style=\"background:#b91c1c;padding:18px 24px;border-radius:8px 8px 0 0;\">"
            + "<h2 style=\"color:#fff;margin:0;font-size:18px;\">Afet Durum Bildirimi</h2>"
            + "<p style=\"color:#fde2e2;margin:4px 0 0;\">AFET Yönetim Sistemi</p></div>"
            + "<div style=\"background:#fff;border:1px solid #e2e8f0;border-top:none;border-radius:0 0 8px 8px;padding:24px;\">"
            + "<p style=\"margin:0 0 12px;\"><strong>" + esc(fullName(sender)) + "</strong> size afet durum bildirimi gönderdi.</p>"
            + "<div style=\"background:#fef2f2;border:1px solid #fecaca;border-radius:6px;padding:14px 18px;margin-bottom:16px;\">"
            + "<p style=\"margin:0;font-size:16px;font-weight:600;color:#991b1b;\">" + esc(type.getMessageText()) + "</p></div>"
            + "<table style=\"border-collapse:collapse;width:100%;font-size:14px;\">"
            + rowHtml("Tarih", DISPLAY_FMT.format(OffsetDateTime.now()))
            + (area != null ? rowHtml("Son bilinen bölge", area) : "")
            + (assembly != null ? rowHtml("En yakın toplanma alanı", assembly) : "")
            + "</table>"
            + "<hr style=\"margin:20px 0;border:none;border-top:1px solid #e2e8f0;\"/>"
            + "<p style=\"font-size:11px;color:#94a3b8;margin:0;\">Bu mesaj AFET Yönetim Sistemi üzerinden gönderilmiştir.</p>"
            + "</div></body></html>";
    }

    private String lastKnownArea(User user) {
        if (user.getNeighborhood() == null) return null;
        String n = user.getNeighborhood().getName();
        String d = user.getNeighborhood().getDistrict() != null
                ? user.getNeighborhood().getDistrict().getName() : null;
        return d != null ? d + " / " + n : n;
    }

    private String nearestAssemblyArea(User user) {
        if (user.getNeighborhood() == null) return null;
        try {
            List<AssemblyArea> areas = assemblyAreaRepository
                    .findActiveApprovedByNeighborhoodId(user.getNeighborhood().getId());
            if (areas == null || areas.isEmpty()) return null;
            AssemblyArea a = areas.get(0);
            return a.getAddress() != null && !a.getAddress().isBlank()
                    ? a.getName() + " (" + a.getAddress() + ")" : a.getName();
        } catch (Exception e) {
            return null;
        }
    }

    private void logMessage(User sender, User recipient, EmergencyMessageType type,
                            String text, String channel, String status, String error) {
        try {
            messageLogRepository.save(EmergencyContactMessageLog.builder()
                    .sender(sender).recipient(recipient)
                    .messageType(type).messageText(text)
                    .channel(channel).status(status)
                    .errorMessage(error)
                    .sentAt("SENT".equals(status) ? OffsetDateTime.now() : null)
                    .build());
        } catch (Exception e) {
            log.warn("Acil mesaj logu kaydedilemedi: {}", e.getMessage());
        }
    }

    private String fullName(User u) {
        return u.getFirstName() + " " + u.getLastName();
    }

    private String rowHtml(String k, String v) {
        return "<tr><td style=\"padding:8px 12px;color:#64748b;border:1px solid #e2e8f0;width:160px;\">" + esc(k) + "</td>"
            + "<td style=\"padding:8px 12px;border:1px solid #e2e8f0;\">" + esc(v) + "</td></tr>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
