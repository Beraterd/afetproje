package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.*;
import com.afet.koordinasyon.domain.enums.EmergencyStatusTemplateKey;
import com.afet.koordinasyon.domain.enums.NotificationChannel;
import com.afet.koordinasyon.domain.enums.NotificationStatus;
import com.afet.koordinasyon.dto.request.SendStatusMessageRequest;
import com.afet.koordinasyon.dto.response.StatusMessageResponse;
import com.afet.koordinasyon.dto.response.StatusTemplateResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.config.WhatsAppProperties;
import com.afet.koordinasyon.notification.EmailNotificationProvider;
import com.afet.koordinasyon.notification.WhatsAppNotificationProvider;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyStatusMessageService {

    private final EmergencyContactRepository emergencyContactRepository;
    private final EmergencyStatusMessageRepository statusMessageRepository;
    private final EmergencyStatusMessageLogRepository statusMessageLogRepository;
    private final EarthquakeSimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final EmailNotificationProvider emailProvider;
    private final WhatsAppNotificationProvider whatsAppProvider;
    private final WhatsAppProperties whatsAppProperties;
    private final EmergencyStatusTemplateResolver templateResolver;

    /** Konum eklenebilecek acil şablonlar. Güvenli şablonlarda konum istenmez/eklenmez. */
    private static final Set<EmergencyStatusTemplateKey> EMERGENCY_TEMPLATES =
            EnumSet.of(EmergencyStatusTemplateKey.NEED_HELP, EmergencyStatusTemplateKey.TRAPPED_UNDER_RUBBLE);

    /** Returns the canonical template list that the frontend must use. */
    public List<StatusTemplateResponse> getTemplates() {
        return templateResolver.allTemplates().entrySet().stream()
                .map(e -> StatusTemplateResponse.builder()
                        .key(e.getKey().name())
                        .label(e.getValue())
                        .build())
                .toList();
    }

    @Transactional
    public StatusMessageResponse sendStatusMessage(SendStatusMessageRequest request, UserPrincipal principal) {
        // Offline-queue idempotency: return existing record if clientGeneratedId already processed
        if (request.getClientGeneratedId() != null && !request.getClientGeneratedId().isBlank()) {
            var existing = statusMessageRepository.findByClientGeneratedId(request.getClientGeneratedId());
            if (existing.isPresent()) {
                log.info("Duplicate status message (offline queue replay) atlandı: clientGeneratedId={}",
                        request.getClientGeneratedId());
                return toResponse(existing.get());
            }
        }

        User sender = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        EmergencyStatusTemplateKey templateKey;
        try {
            templateKey = EmergencyStatusTemplateKey.valueOf(request.getTemplateKey());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Geçersiz mesaj şablonu: " + request.getTemplateKey());
        }

        String messageText = templateResolver.resolve(templateKey);

        EarthquakeSimulation simulation = null;
        if (request.getSimulationId() != null) {
            simulation = simulationRepository.findById(request.getSimulationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Simulation", "id", request.getSimulationId()));
        }

        List<EmergencyContact> contacts = emergencyContactRepository
                .findByOwnerIdOrderByCreatedAtAsc(principal.getId());

        if (contacts.isEmpty()) {
            throw new BusinessRuleException("Kayıtlı yakınınız bulunmamaktadır. Önce yakın ekleyiniz.");
        }

        EmergencyStatusMessage statusMessage = EmergencyStatusMessage.builder()
                .sender(sender)
                .simulation(simulation)
                .templateKey(templateKey.name())
                .messageText(messageText)
                .channel(NotificationChannel.EMAIL)
                .recipientCount(contacts.size())
                .clientGeneratedId(request.getClientGeneratedId())
                .build();
        EmergencyStatusMessage saved = statusMessageRepository.save(statusMessage);

        String senderFullName = sender.getFirstName() + " " + sender.getLastName();
        String subject = "Acil Durum Mesajı - " + senderFullName;
        String now = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        boolean isEmergency = EMERGENCY_TEMPLATES.contains(templateKey);
        String locationLink = buildLocationLink(isEmergency, request.getLatitude(), request.getLongitude());
        String whatsAppBody = buildWhatsAppBody(senderFullName, messageText, locationLink);

        int emailSentCount = 0;
        int whatsappSentCount = 0;
        for (EmergencyContact ec : contacts) {
            User recipient = ec.getContact();

            // ── E-posta kanalı (WhatsApp başarısızlığından bağımsız) ──
            if (sendEmail(saved, recipient, subject, buildEmailBody(senderFullName, messageText, now))) {
                emailSentCount++;
            }

            // ── WhatsApp kanalı (e-posta başarısızlığından bağımsız) ──
            if (whatsAppProperties.isEnabled()
                    && sendWhatsApp(saved, recipient, whatsAppBody)) {
                whatsappSentCount++;
            }
        }

        return StatusMessageResponse.builder()
                .id(saved.getId())
                .templateKey(templateKey.name())
                .messageText(messageText)
                .recipientCount(contacts.size())
                .sentCount(emailSentCount + whatsappSentCount)
                .emailSentCount(emailSentCount)
                .whatsappSentCount(whatsappSentCount)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /** E-posta gönderir ve EMAIL kanallı log kaydı oluşturur. Başarılıysa true döner. */
    private boolean sendEmail(EmergencyStatusMessage message, User recipient, String subject, String body) {
        NotificationStatus logStatus;
        String errorMsg = null;
        try {
            emailProvider.send(recipient.getEmail(), subject, body);
            logStatus = NotificationStatus.SENT;
        } catch (Exception e) {
            logStatus = NotificationStatus.FAILED;
            errorMsg = e.getMessage();
            log.error("Acil durum e-postası gönderilemedi {}: {}", recipient.getEmail(), e.getMessage());
        }

        statusMessageLogRepository.save(EmergencyStatusMessageLog.builder()
                .message(message)
                .recipient(recipient)
                .recipientEmail(recipient.getEmail())
                .channel(NotificationChannel.EMAIL)
                .status(logStatus)
                .errorMessage(errorMsg)
                .sentAt(logStatus == NotificationStatus.SENT ? OffsetDateTime.now() : null)
                .build());
        return logStatus == NotificationStatus.SENT;
    }

    /** WhatsApp gönderir ve WHATSAPP kanallı log kaydı oluşturur. Başarılıysa true döner. */
    private boolean sendWhatsApp(EmergencyStatusMessage message, User recipient, String body) {
        String phone = recipient.getPhone();
        String maskedPhone = PhoneNumberUtil.mask(phone);
        NotificationStatus logStatus;
        String errorMsg = null;

        if (phone == null || phone.isBlank()) {
            logStatus = NotificationStatus.FAILED;
            errorMsg = "Alıcının telefon numarası yok";
        } else {
            try {
                whatsAppProvider.sendText(phone, body);
                logStatus = NotificationStatus.SENT;
            } catch (Exception e) {
                logStatus = NotificationStatus.FAILED;
                errorMsg = e.getMessage();
                log.error("Acil durum WhatsApp mesajı gönderilemedi {}: {}", maskedPhone, e.getMessage());
            }
        }

        statusMessageLogRepository.save(EmergencyStatusMessageLog.builder()
                .message(message)
                .recipient(recipient)
                .recipientPhone(maskedPhone)
                .channel(NotificationChannel.WHATSAPP)
                .status(logStatus)
                .errorMessage(errorMsg)
                .sentAt(logStatus == NotificationStatus.SENT ? OffsetDateTime.now() : null)
                .build());
        return logStatus == NotificationStatus.SENT;
    }

    /** Acil şablon + konum varsa Google Maps linki döner; aksi halde null (güvenli şablonlarda konum yok). */
    private String buildLocationLink(boolean isEmergency, Double latitude, Double longitude) {
        if (!isEmergency || latitude == null || longitude == null) {
            return null;
        }
        return "https://www.google.com/maps?q=" + latitude + "," + longitude;
    }

    private String buildWhatsAppBody(String senderFullName, String messageText, String locationLink) {
        StringBuilder sb = new StringBuilder();
        sb.append(senderFullName).append(" size acil durum mesajı gönderdi: ").append(messageText);
        if (locationLink != null) {
            sb.append("\nKonum: ").append(locationLink);
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<StatusMessageResponse> getMyMessages(UUID simulationId, UserPrincipal principal) {
        List<EmergencyStatusMessage> messages = simulationId != null
                ? statusMessageRepository.findBySenderIdAndSimulationIdOrderByCreatedAtDesc(principal.getId(), simulationId)
                : statusMessageRepository.findBySenderIdOrderByCreatedAtDesc(principal.getId());
        return messages.stream().map(m -> StatusMessageResponse.builder()
                .id(m.getId())
                .templateKey(m.getTemplateKey())
                .messageText(m.getMessageText())
                .recipientCount(m.getRecipientCount())
                .sentCount(0)
                .createdAt(m.getCreatedAt())
                .build()).toList();
    }

    private StatusMessageResponse toResponse(EmergencyStatusMessage m) {
        return StatusMessageResponse.builder()
                .id(m.getId())
                .templateKey(m.getTemplateKey())
                .messageText(m.getMessageText())
                .recipientCount(m.getRecipientCount())
                .sentCount(0)
                .createdAt(m.getCreatedAt())
                .build();
    }

    private String buildEmailBody(String senderFullName, String messageText, String timestamp) {
        return """
                <!DOCTYPE html>
                <html lang="tr">
                <head><meta charset="UTF-8"/></head>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <div style="background:#c0392b; padding:16px; border-radius:6px 6px 0 0;">
                    <h2 style="color:#fff; margin:0;">Acil Durum Mesaji</h2>
                  </div>
                  <div style="border:1px solid #ddd; border-top:none; padding:20px; border-radius:0 0 6px 6px;">
                    <p><strong>%s</strong> adli kisiden size bir acil durum iletisi var.</p>
                    <p><strong>Mesaj:</strong></p>
                    <blockquote style="border-left:4px solid #c0392b; margin:12px 0; padding:12px 16px; background:#fff5f5; font-size:15px;">
                      %s
                    </blockquote>
                    <p style="color:#888; font-size:13px;"><strong>Gonderim zamani:</strong> %s</p>
                    <hr style="border:none; border-top:1px solid #eee; margin:16px 0;"/>
                    <p style="font-size:12px; color:#aaa;">Bu mesaj afet koordinasyon sistemi uzerinden gonderilmistir.</p>
                  </div>
                </body>
                </html>
                """.formatted(senderFullName, messageText, timestamp);
    }
}
