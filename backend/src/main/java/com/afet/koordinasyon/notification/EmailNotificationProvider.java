package com.afet.koordinasyon.notification;

import com.afet.koordinasyon.domain.enums.NotificationChannel;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationProvider implements NotificationProvider {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@afetkoordinasyon.tr}")
    private String fromAddress;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String effectiveFrom = (fromAddress != null && !fromAddress.isBlank())
                    ? fromAddress : "noreply@afetkoordinasyon.tr";
            helper.setFrom(effectiveFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Email sent to {} subject='{}'", to, subject);
        } catch (MessagingException e) {
            log.error("Email send failed to {}: {}", to, e.getMessage());
            throw new NotificationException("E-posta gönderilemedi: " + e.getMessage(), e);
        }
    }
}
