package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.ResendSmsLoginRequest;
import com.afet.koordinasyon.dto.request.StartSmsLoginRequest;
import com.afet.koordinasyon.dto.request.VerifySmsLoginRequest;
import com.afet.koordinasyon.dto.response.LoginResponse;
import com.afet.koordinasyon.dto.response.StartSmsLoginResponse;
import com.afet.koordinasyon.service.SmsAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Two-step SMS login endpoints")
public class SmsAuthController {

    private final SmsAuthService smsAuthService;

    @PostMapping("/login/start")
    @Operation(summary = "Step 1: verify email+password and send OTP via SMS")
    public ResponseEntity<StartSmsLoginResponse> startSmsLogin(
            @Valid @RequestBody StartSmsLoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(smsAuthService.start(request, ip, ua));
    }

    @PostMapping("/login/verify-sms")
    @Operation(summary = "Step 2: submit OTP and receive JWT token")
    public ResponseEntity<LoginResponse> verifySmsLogin(@Valid @RequestBody VerifySmsLoginRequest request) {
        return ResponseEntity.ok(smsAuthService.verifySms(request));
    }

    @PostMapping("/login/resend-sms")
    @Operation(summary = "Resend OTP for an active challenge")
    public ResponseEntity<StartSmsLoginResponse> resendSmsLogin(@Valid @RequestBody ResendSmsLoginRequest request) {
        return ResponseEntity.ok(smsAuthService.resendSms(request));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
