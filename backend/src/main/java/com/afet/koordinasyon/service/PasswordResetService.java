package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.PasswordResetToken;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.repository.PasswordResetTokenRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.service.email.PasswordChangedEmailEvent;
import com.afet.koordinasyon.service.email.PasswordResetEmailEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final long TTL_MINUTES = 60;
    private static final int MAX_ACTIVE_TOKENS_PER_USER = 3;
    private static final long MIN_RESEND_SECONDS = 60;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional
    public void requestPasswordReset(String email, HttpServletRequest httpRequest) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.debug("Password reset requested for unknown email, ignoring");
            return;
        }

        User user = userOpt.get();

        Instant cutoff = Instant.now().minusSeconds(MIN_RESEND_SECONDS);
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository
                .findByUserIdAndUsedAtIsNullAndExpiresAtAfter(user.getId(), Instant.now());
        boolean tooSoon = activeTokens.stream().anyMatch(t -> t.getCreatedAt().isAfter(cutoff));
        if (tooSoon) {
            log.debug("Password reset rate-limited for userId={}", user.getId());
            return;
        }

        if (activeTokens.size() >= MAX_ACTIVE_TOKENS_PER_USER) {
            passwordResetTokenRepository.invalidateAllForUser(user.getId(), Instant.now());
        }

        String rawToken = generateRawToken();
        String tokenHash = sha256Hex(rawToken);

        String ipAddress = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        PasswordResetToken prt = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(TTL_MINUTES * 60))
                .ipAddress(ipAddress != null ? ipAddress.substring(0, Math.min(ipAddress.length(), 100)) : null)
                .userAgent(userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 500)) : null)
                .build();
        passwordResetTokenRepository.save(prt);

        String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
        log.info("Password reset token created for userId={}", user.getId()); // raw token NOT logged

        // Fire AFTER_COMMIT so the token is committed before the email reaches the user
        eventPublisher.publishEvent(new PasswordResetEmailEvent(user.getId(), resetLink));
    }

    @Transactional(readOnly = true)
    public boolean validateResetToken(String rawToken) {
        String hash = sha256Hex(rawToken);
        return passwordResetTokenRepository.findByTokenHash(hash)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessRuleException("Şifreler eşleşmiyor");
        }

        String hash = sha256Hex(rawToken);
        PasswordResetToken prt = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessRuleException("Geçersiz veya süresi dolmuş şifre sıfırlama bağlantısı"));

        if (!prt.isValid()) {
            throw new BusinessRuleException("Geçersiz veya süresi dolmuş şifre sıfırlama bağlantısı");
        }

        // Load the user directly — do NOT rely on the lazy proxy from prt.getUser()
        UUID userId = prt.getUser().getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Kullanıcı bulunamadı"));

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encodedPassword);
        // saveAndFlush forces the UPDATE into the DB within this TX before anything else runs
        userRepository.saveAndFlush(user);

        log.info("Password hash written to DB for userId={} email={}", user.getId(), user.getEmail());

        // Mark this token as used first, then invalidate any remaining active tokens
        prt.setUsedAt(Instant.now());
        passwordResetTokenRepository.saveAndFlush(prt);
        passwordResetTokenRepository.invalidateAllForUser(userId, Instant.now());

        // Publish AFTER_COMMIT event — email fires only after the TX successfully commits
        eventPublisher.publishEvent(new PasswordChangedEmailEvent(userId));
        log.info("Password reset completed for userId={}", userId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
