package com.afet.koordinasyon.service;

import com.afet.koordinasyon.config.SmsProperties;
import com.afet.koordinasyon.domain.entity.AssemblyArea;
import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.SmsDeliveryStatus;
import com.afet.koordinasyon.domain.enums.SmsDeliveryType;
import com.afet.koordinasyon.notification.SmsNotificationProvider;
import com.afet.koordinasyon.repository.AssemblyAreaRepository;
import com.afet.koordinasyon.repository.EarthquakeEventRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AfadEarthquakeSmsNotificationService {

    private final EarthquakeEventRepository earthquakeEventRepository;
    private final UserRepository userRepository;
    private final AssemblyAreaRepository assemblyAreaRepository;
    private final SmsNotificationProvider smsNotificationProvider;
    private final SmsAuthService smsAuthService;
    private final SmsProperties smsProperties;

    private static final int MAX_AREAS_IN_SMS = 3;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNewEarthquake(EarthquakeAlertCreatedEvent event) {
        // SMS yalnızca gerçek deprem için gönderilir; simülasyon SMS göndermez.
        if (event.alert().isSimulation()) {
            return;
        }
        if (!smsProperties.isEnabled()) {
            log.debug("SMS bildirimleri devre dışı (app.sms.enabled=false), deprem SMS atlanıyor.");
            return;
        }

        EarthquakeEvent eq = earthquakeEventRepository.findById(event.alert().alertId()).orElse(null);
        if (eq == null) {
            log.warn("Deprem SMS bildirimi: kayıt bulunamadı id={}", event.alert().alertId());
            return;
        }

        List<User> users = userRepository.findSmsEligibleUsers();
        if (users.isEmpty()) {
            log.info("Deprem SMS bildirimi: SMS'e uygun kullanıcı bulunamadı (externalId={})", eq.getExternalId());
            return;
        }

        log.info("Deprem SMS bildirimi başlıyor: externalId={}, M={}, kullanıcı sayısı={}",
                eq.getExternalId(), eq.getMagnitude(), users.size());

        int successCount = 0;
        for (User user : users) {
            try {
                List<AssemblyArea> areas = loadAreasForUser(user);
                String smsText = buildSmsText(eq, areas);
                String phone = user.getPhone();
                String maskedPhone = PhoneNumberUtil.mask(phone);

                smsNotificationProvider.send(phone, null, smsText);
                smsAuthService.saveDeliveryLog(user.getId(), maskedPhone, SmsDeliveryType.EARTHQUAKE_ALERT,
                        SmsDeliveryStatus.SENT, null, null, eq.getId());
                successCount++;
                log.info("Deprem SMS gönderildi: externalId={}, alıcı={}", eq.getExternalId(), maskedPhone);
            } catch (Exception e) {
                String maskedPhone = PhoneNumberUtil.mask(user.getPhone());
                smsAuthService.saveDeliveryLog(user.getId(), maskedPhone, SmsDeliveryType.EARTHQUAKE_ALERT,
                        SmsDeliveryStatus.FAILED, null, e.getMessage(), eq.getId());
                log.error("Deprem SMS gönderilemedi: externalId={}, alıcı={}, hata={}",
                        eq.getExternalId(), maskedPhone, e.getMessage());
            }
        }

        log.info("Deprem SMS bildirimi tamamlandı: externalId={}, başarılı={}/{}",
                eq.getExternalId(), successCount, users.size());
    }

    private List<AssemblyArea> loadAreasForUser(User user) {
        if (user.getNeighborhood() == null) return Collections.emptyList();
        try {
            return assemblyAreaRepository.findActiveApprovedByNeighborhoodId(user.getNeighborhood().getId());
        } catch (Exception e) {
            log.warn("Toplanma alanları yüklenemedi (userId={}): {}", user.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildSmsText(EarthquakeEvent eq, List<AssemblyArea> areas) {
        StringBuilder sb = new StringBuilder();
        sb.append("AFAD DEPREM: M").append(String.format("%.1f", eq.getMagnitude()));

        if (eq.getLocation() != null && !eq.getLocation().isBlank()) {
            sb.append(" - ").append(eq.getLocation());
        }

        List<AssemblyArea> limited = areas.size() > MAX_AREAS_IN_SMS ? areas.subList(0, MAX_AREAS_IN_SMS) : areas;
        if (!limited.isEmpty()) {
            sb.append(" | Toplanma Alan(lar)ı:");
            for (AssemblyArea area : limited) {
                sb.append(" ").append(area.getName()).append(";");
            }
        }

        sb.append(" | AFAD ve yetkili duyurularını takip edin.");
        return sb.toString();
    }
}
