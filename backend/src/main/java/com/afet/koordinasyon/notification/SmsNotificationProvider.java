package com.afet.koordinasyon.notification;

import com.afet.koordinasyon.config.SmsProperties;
import com.afet.koordinasyon.domain.enums.NotificationChannel;
import com.afet.koordinasyon.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationProvider implements NotificationProvider {

    private final SmsProperties smsProperties;
    private final IletiMerkeziSmsClient smsClient;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (!smsProperties.isEnabled()) {
            log.debug("SMS gönderimi devre dışı (app.sms.enabled=false), atlanıyor. Alıcı: {}",
                    PhoneNumberUtil.mask(to));
            return;
        }

        String destination = smsProperties.isTestMode() ? smsProperties.getTestRecipient() : to;

        if (destination == null || destination.isBlank()) {
            log.warn("SMS alıcı adresi boş, atlanıyor.");
            return;
        }

        String normalized = PhoneNumberUtil.normalize(destination);
        SmsSendResult result = smsClient.send(normalized, body);

        if (!result.success()) {
            throw new NotificationException("SMS gönderilemedi: " + result.errorMessage(), null);
        }

        log.info("SMS gönderildi: alıcı={}, orderId={}", PhoneNumberUtil.mask(to), result.providerMessageId());
    }
}
