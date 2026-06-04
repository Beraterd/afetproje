package com.afet.koordinasyon.notification;

import com.afet.koordinasyon.config.WhatsAppProperties;
import com.afet.koordinasyon.domain.enums.NotificationChannel;
import com.afet.koordinasyon.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WhatsApp bildirim sağlayıcısı.
 * <ul>
 *   <li>enabled=false → sessizce atla (debug log).</li>
 *   <li>testMode=true → tüm mesajları testRecipient numarasına gönder.</li>
 *   <li>Telefonlar PhoneNumberUtil ile E.164 formatına normalize edilir.</li>
 *   <li>Loglarda numara maskelenir; token asla loglanmaz.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationProvider implements NotificationProvider {

    private final WhatsAppProperties properties;
    private final WhatsAppCloudClient client;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WHATSAPP;
    }

    /** NotificationProvider sözleşmesi: subject yok sayılır, body text mesajı olarak gider. */
    @Override
    public void send(String to, String subject, String body) {
        sendText(to, body);
    }

    /** Düz metin WhatsApp mesajı gönderir. Başarısız olursa NotificationException fırlatır. */
    public void sendText(String to, String body) {
        String destination = resolveDestination(to);
        if (destination == null) return;

        WhatsAppSendResult result = client.sendText(destination, body);
        handleResult(result, to, "TEXT", null);
    }

    /** Onaylı template mesajı gönderir. Başarısız olursa NotificationException fırlatır. */
    public void sendTemplate(String to, String templateName, List<String> bodyParams) {
        String destination = resolveDestination(to);
        if (destination == null) return;

        WhatsAppSendResult result = client.sendTemplate(
                destination, templateName, properties.getLanguageCode(), bodyParams);
        handleResult(result, to, "TEMPLATE", templateName);
    }

    /**
     * enabled/testMode kurallarını uygular ve gönderilecek E.164 numarasını döner.
     * Gönderim yapılmayacaksa null döner (devre dışı / boş alıcı).
     */
    private String resolveDestination(String to) {
        if (!properties.isEnabled()) {
            log.debug("WhatsApp gönderimi devre dışı (app.whatsapp.enabled=false), atlanıyor. Alıcı: {}",
                    PhoneNumberUtil.mask(to));
            return null;
        }

        String target = properties.isTestMode() ? properties.getTestRecipient() : to;
        if (target == null || target.isBlank()) {
            log.warn("WhatsApp alıcı numarası boş, atlanıyor.");
            return null;
        }
        String effective = PhoneNumberUtil.toE164(target);
        // Maskeli log: gerçek numara/token asla tam görünmez. testMode'da effective != original olur.
        log.debug("WhatsApp alıcı çözümleme: original={}, normalized={}, effective={} (testMode={})",
                PhoneNumberUtil.mask(to), PhoneNumberUtil.mask(PhoneNumberUtil.toE164(to)),
                PhoneNumberUtil.mask(effective), properties.isTestMode());
        return effective;
    }

    private void handleResult(WhatsAppSendResult result, String originalTo, String messageType, String templateName) {
        if (result == null) return; // devre dışı / atlandı
        if (!result.success()) {
            throw new NotificationException("WhatsApp gönderilemedi: " + result.errorMessage(), null);
        }
        if (templateName != null) {
            log.info("WhatsApp mesajı gönderildi: messageType={}, template={}, alıcı={}, messageId={}",
                    messageType, templateName, PhoneNumberUtil.mask(originalTo), result.providerMessageId());
        } else {
            log.info("WhatsApp mesajı gönderildi: messageType={}, alıcı={}, messageId={}",
                    messageType, PhoneNumberUtil.mask(originalTo), result.providerMessageId());
        }
    }
}
