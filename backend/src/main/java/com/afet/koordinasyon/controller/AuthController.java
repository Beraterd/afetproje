package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.ForgotPasswordRequest;
import com.afet.koordinasyon.dto.request.LoginRequest;
import com.afet.koordinasyon.dto.request.RegisterRequest;
import com.afet.koordinasyon.dto.request.ResetPasswordRequest;
import com.afet.koordinasyon.dto.response.LoginResponse;
import com.afet.koordinasyon.dto.response.MessageResponse;
import com.afet.koordinasyon.dto.response.ResetTokenValidationResponse;
import com.afet.koordinasyon.dto.response.TokenRefreshResponse;
import com.afet.koordinasyon.dto.response.UserResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.security.JwtTokenProvider;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.AuthService;
import com.afet.koordinasyon.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, register and current-user endpoints")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "afet_refresh_token";

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.security.refresh-cookie-secure:true}")
    private boolean refreshCookieSecure;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password — returns JWT token, sets refresh token cookie")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthService.LoginResult result = authService.login(request, httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.rawRefreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/demo-login")
    @Operation(summary = "Start a read-only demo admin session — no credentials required")
    public ResponseEntity<LoginResponse> demoLogin(HttpServletRequest httpRequest) {
        AuthService.LoginResult result = authService.demoLogin(httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.rawRefreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new volunteer account")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token cookie for a new access token (rotates the refresh token)")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessRuleException("Refresh token bulunamadı. Lütfen tekrar giriş yapın.",
                    HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISSING");
        }
        AuthService.TokenPair pair = authService.refresh(refreshToken, httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(pair.rawRefreshToken()).toString())
                .body(pair.response());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh token and clear the cookie")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            authService.logout(refreshToken);
        }
        ResponseCookie cleared = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .body(new MessageResponse("Çıkış yapıldı."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
    }

    private ResponseCookie buildRefreshCookie(String rawToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link via e-mail")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        passwordResetService.requestPasswordReset(request.getEmail(), httpRequest);
        return ResponseEntity.ok(new MessageResponse(
                "Eğer bu e-posta sisteme kayıtlıysa şifre sıfırlama bağlantısı gönderildi."));
    }

    @GetMapping("/reset-password/validate")
    @Operation(summary = "Check whether a password reset token is still valid")
    public ResponseEntity<ResetTokenValidationResponse> validateResetToken(@RequestParam String token) {
        boolean valid = passwordResetService.validateResetToken(token);
        return ResponseEntity.ok(new ResetTokenValidationResponse(valid));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using a valid reset token")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok(new MessageResponse("Şifreniz başarıyla güncellendi."));
    }
}
