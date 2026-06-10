package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.ShortLink;
import com.afet.koordinasyon.repository.ShortLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Kısa link üretimi ve çözümlemesi.
 * Üretilen kod base62 (a-zA-Z0-9), varsayılan 7 karakter; çakışma olursa yeniden denenir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShortLinkService {

    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();

    private final ShortLinkRepository shortLinkRepository;

    /**
     * Backend tabanlı kısa link mutlak adresi (örn. http://localhost:8080/s/abc1234).
     * /s/{code} redirect endpoint'i backend tarafından sunulduğu için backend public
     * adresine işaret etmelidir.
     */
    @Value("${app.short-link.base-url}")
    private String shortLinkBaseUrl;

    /** Kısa linkin yönlendireceği frontend uygulamasının ana adresi. */
    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Verilen hedef URL için kısa link oluşturur ve tam /s/{code} adresini döner.
     *
     * @param targetUrl         yönlendirilecek tam URL
     * @param userId            opsiyonel kullanıcı (izleme amaçlı)
     * @param earthquakeEventId opsiyonel deprem kaydı (izleme amaçlı)
     * @param expiresAt         opsiyonel son geçerlilik tarihi
     * @return /s/{code} formatında tam kısa link adresi
     */
    @Transactional
    public String createShortLink(String targetUrl, UUID userId, UUID earthquakeEventId, OffsetDateTime expiresAt) {
        String code = generateUniqueCode();
        ShortLink link = ShortLink.builder()
                .code(code)
                .targetUrl(targetUrl)
                .userId(userId)
                .earthquakeEventId(earthquakeEventId)
                .expiresAt(expiresAt)
                .build();
        shortLinkRepository.save(link);
        return buildFullUrl(code);
    }

    /** Tam /s/{code} adresini üretir. */
    public String buildFullUrl(String code) {
        return trimTrailingSlash(shortLinkBaseUrl) + "/s/" + code;
    }

    /**
     * Verilen yola karşılık gelen tam frontend URL'i üretir.
     * @param path "/emergency/assembly-areas?earthquakeId=..." gibi baştan eğik çizgili yol
     */
    public String buildFrontendUrl(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return trimTrailingSlash(frontendUrl) + normalizedPath;
    }

    /**
     * Koda karşılık gelen, süresi dolmamış hedef URL'i döner.
     * Bulunamaz veya süresi dolmuşsa Optional.empty().
     */
    @Transactional(readOnly = true)
    public Optional<String> resolveTargetUrl(String code) {
        return shortLinkRepository.findByCode(code)
                .filter(link -> link.getExpiresAt() == null
                        || link.getExpiresAt().isAfter(OffsetDateTime.now()))
                .map(ShortLink::getTargetUrl);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!shortLinkRepository.existsByCode(code)) {
                return code;
            }
        }
        // Son çare: daha uzun kod ile çakışma olasılığını düşür.
        return randomCode() + randomCode().substring(0, 3);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String trimTrailingSlash(String s) {
        if (s == null || s.isBlank()) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
