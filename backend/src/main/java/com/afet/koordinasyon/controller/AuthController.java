package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.ForgotPasswordRequest;
import com.afet.koordinasyon.dto.request.LoginRequest;
import com.afet.koordinasyon.dto.request.RegisterRequest;
import com.afet.koordinasyon.dto.request.ResetPasswordRequest;
import com.afet.koordinasyon.dto.response.LoginResponse;
import com.afet.koordinasyon.dto.response.MessageResponse;
import com.afet.koordinasyon.dto.response.ResetTokenValidationResponse;
import com.afet.koordinasyon.dto.response.UserResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.AuthService;
import com.afet.koordinasyon.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, register and current-user endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password — returns JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/demo-login")
    @Operation(summary = "Start a read-only demo admin session — no credentials required")
    public ResponseEntity<LoginResponse> demoLogin() {
        return ResponseEntity.ok(authService.demoLogin());
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new volunteer account")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
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
