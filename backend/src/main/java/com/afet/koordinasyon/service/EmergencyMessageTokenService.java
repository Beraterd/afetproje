package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.EmergencyMessageToken;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.EmergencyMessageType;
import com.afet.koordinasyon.domain.enums.EmergencyMessageTokenStatus;
import com.afet.koordinasyon.dto.response.EmergencyTokenStatusResponse;
import com.afet.koordinasyon.repository.EmergencyMessageTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * §6 — Acil mesaj token'larının üretimi ve güvenli tüketimi.
 *
 * <p>Güvenlik kuralları:
 * <ul>
 *   <li>Ham token kriptografik olarak rastgeledir (256-bit) ve YALNIZCA e-postada görünür.</li>
 *   <li>DB'de yalnızca SHA-256 hash saklanır; düz token loglanmaz.</li>
 *   <li>Token 24 saat sonra geçersizdir.</li>
 *   <li>Token tek kullanımlıktır; tüketim atomik olarak ACTIVE→USED geçişidir.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyMessageTokenService {

    private final EmergencyMessageTokenRepository tokenRepository;

    @Value("${app.emergency-message.token-ttl-hours:24}")
    private long tokenTtlHours;

    private static final SecureRandom RNG = new SecureRandom();

    /** Tüketim sonucu. VALID ise {@code user} doludur. */
    public enum Outcome { VALID, EXPIRED, INVALID }

    public record ConsumeResult(Outcome outcome, User user) {}

    /**
     * Yeni token üretir, hash'ini kaydeder ve HAM token'ı döner (e-posta linki için).
     * Çağıran ham token'ı loglamamalıdır.
     */
    @Transactional
    public String issueToken(User user, UUID earthquakeNotificationId) {
        String rawToken = generateRawToken();
        EmergencyMessageToken entity = EmergencyMessageToken.builder()
                .tokenHash(sha256Hex(rawToken))
                .user(user)
                .earthquakeNotificationId(earthquakeNotificationId)
                .status(EmergencyMessageTokenStatus.ACTIVE)
                .expiresAt(OffsetDateTime.now().plusHours(tokenTtlHours))
                .build();
        tokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Token'ı doğrular ve geçerliyse ATOMİK olarak USED'a çeker (tek kullanım).
     * Süre dolduysa EXPIRED işaretler. Bulunamaz/zaten kullanılmışsa INVALID döner.
     *
     * <p>REQUIRES_NEW: tüketim, çağıran akıştan BAĞIMSIZ olarak hemen commit edilir.
     * Böylece mesaj gönderimi (sonraki adım) hata verse bile token tekrar kullanılamaz.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConsumeResult consume(String rawToken, EmergencyMessageType messageType) {
        if (rawToken == null || rawToken.isBlank()) {
            return new ConsumeResult(Outcome.INVALID, null);
        }
        EmergencyMessageToken token = tokenRepository.findByTokenHash(sha256Hex(rawToken)).orElse(null);
        if (token == null) {
            return new ConsumeResult(Outcome.INVALID, null);
        }
        if (token.getStatus() != EmergencyMessageTokenStatus.ACTIVE) {
            return new ConsumeResult(Outcome.INVALID, null);
        }
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            token.setStatus(EmergencyMessageTokenStatus.EXPIRED);
            tokenRepository.save(token);
            return new ConsumeResult(Outcome.EXPIRED, null);
        }
        // Atomik tek kullanım: ACTIVE → USED.
        token.setStatus(EmergencyMessageTokenStatus.USED);
        token.setUsedAt(OffsetDateTime.now());
        token.setMessageType(messageType);
        tokenRepository.save(token);
        return new ConsumeResult(Outcome.VALID, token.getUser());
    }

    /**
     * Token'ı TÜKETMEDEN doğrular. Geçerliyse gönderen adını ve son bilinen konumu döner.
     * Frontend EmergencyStatusPage tarafından sayfa açılışında çağrılır.
     */
    @Transactional(readOnly = true)
    public EmergencyTokenStatusResponse validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return EmergencyTokenStatusResponse.builder().valid(false).errorCode("INVALID").build();
        }
        EmergencyMessageToken token = tokenRepository.findByTokenHash(sha256Hex(rawToken)).orElse(null);
        if (token == null) {
            return EmergencyTokenStatusResponse.builder().valid(false).errorCode("INVALID").build();
        }
        if (token.getStatus() == EmergencyMessageTokenStatus.USED) {
            return EmergencyTokenStatusResponse.builder().valid(false).errorCode("USED").build();
        }
        if (token.getStatus() == EmergencyMessageTokenStatus.EXPIRED
                || (token.getExpiresAt() != null && token.getExpiresAt().isBefore(OffsetDateTime.now()))) {
            return EmergencyTokenStatusResponse.builder().valid(false).errorCode("EXPIRED").build();
        }
        User user = token.getUser();
        return EmergencyTokenStatusResponse.builder()
                .valid(true)
                .senderDisplayName(user.getFirstName() + " " + user.getLastName())
                .lastKnownLatitude(user.getLastKnownLatitude())
                .lastKnownLongitude(user.getLastKnownLongitude())
                .lastKnownLocationAccuracy(user.getLastKnownLocationAccuracy())
                .lastKnownLocationUpdatedAt(user.getLastKnownLocationUpdatedAt())
                .build();
    }

    /** Tüketilen token'a konum bilgisini kaydeder (ayrı TX). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveTokenLocation(String rawToken, BigDecimal lat, BigDecimal lon,
                                   BigDecimal accuracy, String source, String mapsUrl) {
        if (rawToken == null || rawToken.isBlank() || lat == null || lon == null) return;
        tokenRepository.findByTokenHash(sha256Hex(rawToken)).ifPresent(t -> {
            t.setLatitude(lat);
            t.setLongitude(lon);
            t.setLocationAccuracy(accuracy);
            t.setLocationSource(source);
            t.setMapsUrl(mapsUrl);
            tokenRepository.save(t);
        });
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────────

    private String generateRawToken() {
        byte[] bytes = new byte[32]; // 256-bit
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Token hash hesaplanamadı", e);
        }
    }
}
