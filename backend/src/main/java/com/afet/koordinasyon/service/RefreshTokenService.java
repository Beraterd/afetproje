package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.RefreshToken;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.repository.RefreshTokenRepository;
import com.afet.koordinasyon.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;

/**
 * Refresh token yaşam döngüsü: üretim, rotation (her kullanımda eskisi iptal edilip yenisi
 * verilir) ve iptal. Ham token yalnızca istemciye (HttpOnly cookie olarak) döner; DB'de yalnızca
 * SHA-256 hash'i tutulur — {@link PasswordResetService} ile aynı desen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public record IssuedToken(String rawToken, RefreshToken entity) {
    }

    @Transactional
    public IssuedToken issue(User user, HttpServletRequest request) {
        String raw = jwtTokenProvider.generateRefreshTokenRaw();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256Hex(raw))
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpirationMs())))
                .userAgent(truncate(request.getHeader("User-Agent"), 500))
                .ipAddress(truncate(resolveClientIp(request), 45))
                .build();
        refreshTokenRepository.save(entity);
        return new IssuedToken(raw, entity);
    }

    /**
     * Geçerli bir refresh token'ı iptal edip yerine yenisini üretir (rotation). Zaten iptal
     * edilmiş bir token tekrar sunulursa (rotation sonrası eski token'ın tekrar kullanılması —
     * olası çalıntı token belirtisi) kullanıcının TÜM oturumları güvenlik amacıyla iptal edilir.
     */
    @Transactional
    public IssuedToken rotate(String rawToken, HttpServletRequest request) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> invalidToken());

        if (existing.isRevoked()) {
            log.warn("Revoked refresh token reuse detected for userId={}", existing.getUser().getId());
            refreshTokenRepository.revokeAllByUserId(existing.getUser().getId());
            throw invalidToken();
        }
        if (existing.isExpired()) {
            throw invalidToken();
        }

        existing.setRevoked(true);
        existing.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(existing);

        return issue(existing.getUser(), request);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256Hex(rawToken)).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(rt);
        });
    }

    private BusinessRuleException invalidToken() {
        return new BusinessRuleException("Geçersiz veya süresi dolmuş oturum. Lütfen tekrar giriş yapın.",
                HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
