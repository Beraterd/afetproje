package com.afet.koordinasyon.service;

import com.afet.koordinasyon.config.SmsProperties;
import com.afet.koordinasyon.domain.entity.SmsDeliveryLog;
import com.afet.koordinasyon.domain.entity.SmsLoginCode;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.domain.enums.SmsDeliveryStatus;
import com.afet.koordinasyon.domain.enums.SmsDeliveryType;
import com.afet.koordinasyon.dto.request.ResendSmsLoginRequest;
import com.afet.koordinasyon.dto.request.StartSmsLoginRequest;
import com.afet.koordinasyon.dto.request.VerifySmsLoginRequest;
import com.afet.koordinasyon.dto.response.LoginResponse;
import com.afet.koordinasyon.dto.response.StartSmsLoginResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.notification.SmsNotificationProvider;
import com.afet.koordinasyon.repository.SmsDeliveryLogRepository;
import com.afet.koordinasyon.repository.SmsLoginCodeRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.JwtTokenProvider;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsAuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SmsLoginCodeRepository smsLoginCodeRepository;
    private final SmsDeliveryLogRepository smsDeliveryLogRepository;
    private final SmsNotificationProvider smsNotificationProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final AuthService authService;
    private final SmsProperties smsProperties;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public StartSmsLoginResponse start(StartSmsLoginRequest request, String ipAddress, String userAgent) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BusinessRuleException("Hesabınıza kayıtlı telefon numarası bulunamadı.",
                    HttpStatus.UNPROCESSABLE_ENTITY, "NO_PHONE");
        }

        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
        long recentCount = smsLoginCodeRepository.countActiveCodesSince(user.getId(), oneHourAgo);
        if (recentCount >= smsProperties.getMaxOtpPerHour()) {
            throw new BusinessRuleException("Çok fazla SMS talebi. Lütfen daha sonra tekrar deneyin.",
                    HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMIT");
        }

        String otp = generateOtp();
        String codeHash = passwordEncoder.encode(otp);
        String maskedPhone = PhoneNumberUtil.mask(user.getPhone());
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(smsProperties.getOtpExpirySeconds());

        SmsLoginCode loginCode = SmsLoginCode.builder()
                .user(user)
                .phoneMasked(maskedPhone)
                .codeHash(codeHash)
                .expiresAt(expiresAt)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        loginCode = smsLoginCodeRepository.save(loginCode);
        UUID challengeId = loginCode.getId();

        String smsText = "AFET Koordinasyon: Giriş kodunuz: " + otp + " (5 dakika geçerlidir)";
        SmsDeliveryStatus deliveryStatus = SmsDeliveryStatus.PENDING;
        String deliveryError = null;

        try {
            smsNotificationProvider.send(user.getPhone(), null, smsText);
            deliveryStatus = SmsDeliveryStatus.SENT;
            log.info("OTP SMS gönderildi: challengeId={}, alıcı={}", challengeId, maskedPhone);
        } catch (Exception e) {
            deliveryStatus = SmsDeliveryStatus.FAILED;
            deliveryError = e.getMessage();
            log.error("OTP SMS gönderilemedi: challengeId={}, alıcı={}, hata={}", challengeId, maskedPhone, e.getMessage());
        }

        saveDeliveryLog(user.getId(), maskedPhone, SmsDeliveryType.LOGIN_OTP, deliveryStatus, null, deliveryError, null);

        if (deliveryStatus == SmsDeliveryStatus.FAILED) {
            throw new BusinessRuleException("SMS gönderilemedi. Lütfen tekrar deneyin.",
                    HttpStatus.SERVICE_UNAVAILABLE, "SMS_SEND_FAILED");
        }

        return StartSmsLoginResponse.builder()
                .challengeId(challengeId)
                .maskedPhone(maskedPhone)
                .expiresInSeconds(smsProperties.getOtpExpirySeconds())
                .build();
    }

    @Transactional
    public LoginResponse verifySms(VerifySmsLoginRequest request) {
        SmsLoginCode loginCode = smsLoginCodeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new BusinessRuleException("Geçersiz veya bulunamayan doğrulama kodu.",
                        HttpStatus.BAD_REQUEST, "INVALID_CHALLENGE"));

        if (loginCode.isExpired()) {
            throw new BusinessRuleException("Doğrulama kodunun süresi dolmuştur. Lütfen tekrar giriş yapın.",
                    HttpStatus.BAD_REQUEST, "OTP_EXPIRED");
        }
        if (loginCode.isUsed()) {
            throw new BusinessRuleException("Bu doğrulama kodu daha önce kullanılmış.",
                    HttpStatus.BAD_REQUEST, "OTP_ALREADY_USED");
        }
        if (loginCode.getAttemptCount() >= smsProperties.getMaxVerifyAttempts()) {
            throw new BusinessRuleException("Çok fazla hatalı deneme. Lütfen yeni kod isteyin.",
                    HttpStatus.TOO_MANY_REQUESTS, "OTP_MAX_ATTEMPTS");
        }

        boolean codeMatches = passwordEncoder.matches(request.getCode(), loginCode.getCodeHash());
        if (!codeMatches) {
            loginCode.setAttemptCount(loginCode.getAttemptCount() + 1);
            smsLoginCodeRepository.save(loginCode);
            int remaining = smsProperties.getMaxVerifyAttempts() - loginCode.getAttemptCount();
            throw new BusinessRuleException("Hatalı kod. Kalan deneme hakkı: " + remaining,
                    HttpStatus.BAD_REQUEST, "OTP_INVALID");
        }

        loginCode.setUsedAt(OffsetDateTime.now());
        smsLoginCodeRepository.save(loginCode);

        User user = loginCode.getUser();
        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtTokenProvider.generateAccessToken(principal);

        auditLogService.logUserAction(
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getRole().name(),
                AuditActionType.USER_LOGIN, "USER", user.getId(),
                user.getFirstName() + " " + user.getLastName() + " SMS doğrulamayla giriş yaptı",
                Map.of("email", user.getEmail(), "challengeId", loginCode.getId().toString()));

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs())
                .user(authService.getCurrentUser(user.getId()))
                .build();
    }

    @Transactional
    public StartSmsLoginResponse resendSms(ResendSmsLoginRequest request) {
        SmsLoginCode oldCode = smsLoginCodeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new BusinessRuleException("Geçersiz veya bulunamayan doğrulama kodu.",
                        HttpStatus.BAD_REQUEST, "INVALID_CHALLENGE"));

        if (oldCode.isExpired()) {
            throw new BusinessRuleException("Oturum süresi dolmuştur. Lütfen tekrar giriş yapın.",
                    HttpStatus.BAD_REQUEST, "SESSION_EXPIRED");
        }
        if (oldCode.isUsed()) {
            throw new BusinessRuleException("Bu oturum daha önce tamamlanmış.",
                    HttpStatus.BAD_REQUEST, "SESSION_COMPLETED");
        }

        User user = oldCode.getUser();
        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
        long recentCount = smsLoginCodeRepository.countActiveCodesSince(user.getId(), oneHourAgo);
        if (recentCount >= smsProperties.getMaxOtpPerHour()) {
            throw new BusinessRuleException("Çok fazla SMS talebi. Lütfen daha sonra tekrar deneyin.",
                    HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMIT");
        }

        oldCode.setUsedAt(OffsetDateTime.now());
        smsLoginCodeRepository.save(oldCode);

        String otp = generateOtp();
        String codeHash = passwordEncoder.encode(otp);
        String maskedPhone = PhoneNumberUtil.mask(user.getPhone());
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(smsProperties.getOtpExpirySeconds());

        SmsLoginCode newCode = SmsLoginCode.builder()
                .user(user)
                .phoneMasked(maskedPhone)
                .codeHash(codeHash)
                .expiresAt(expiresAt)
                .ipAddress(oldCode.getIpAddress())
                .userAgent(oldCode.getUserAgent())
                .build();
        newCode = smsLoginCodeRepository.save(newCode);

        String smsText = "AFET Koordinasyon: Giriş kodunuz: " + otp + " (5 dakika geçerlidir)";
        SmsDeliveryStatus deliveryStatus = SmsDeliveryStatus.PENDING;
        String deliveryError = null;

        try {
            smsNotificationProvider.send(user.getPhone(), null, smsText);
            deliveryStatus = SmsDeliveryStatus.SENT;
            log.info("OTP SMS yeniden gönderildi: challengeId={}, alıcı={}", newCode.getId(), maskedPhone);
        } catch (Exception e) {
            deliveryStatus = SmsDeliveryStatus.FAILED;
            deliveryError = e.getMessage();
            log.error("OTP SMS yeniden gönderilemedi: challengeId={}, alıcı={}, hata={}", newCode.getId(), maskedPhone, e.getMessage());
        }

        saveDeliveryLog(user.getId(), maskedPhone, SmsDeliveryType.LOGIN_OTP, deliveryStatus, null, deliveryError, null);

        if (deliveryStatus == SmsDeliveryStatus.FAILED) {
            throw new BusinessRuleException("SMS gönderilemedi. Lütfen tekrar deneyin.",
                    HttpStatus.SERVICE_UNAVAILABLE, "SMS_SEND_FAILED");
        }

        return StartSmsLoginResponse.builder()
                .challengeId(newCode.getId())
                .maskedPhone(maskedPhone)
                .expiresInSeconds(smsProperties.getOtpExpirySeconds())
                .build();
    }

    private String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    void saveDeliveryLog(UUID userId, String phoneMasked, SmsDeliveryType type,
                         SmsDeliveryStatus status, String providerMessageId,
                         String errorMessage, UUID earthquakeEventId) {
        SmsDeliveryLog log = SmsDeliveryLog.builder()
                .userId(userId)
                .phoneMasked(phoneMasked)
                .type(type)
                .provider(smsProperties.getProvider())
                .providerMessageId(providerMessageId)
                .status(status)
                .errorMessage(errorMessage)
                .earthquakeEventId(earthquakeEventId)
                .sentAt(status == SmsDeliveryStatus.SENT ? OffsetDateTime.now() : null)
                .build();
        smsDeliveryLogRepository.save(log);
    }
}
