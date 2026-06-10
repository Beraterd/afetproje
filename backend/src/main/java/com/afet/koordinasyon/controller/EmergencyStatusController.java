package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.EmergencyStatusSendRequest;
import com.afet.koordinasyon.dto.response.EmergencyTokenStatusResponse;
import com.afet.koordinasyon.service.EmergencyMessageService;
import com.afet.koordinasyon.service.EmergencyMessageTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public endpoint'ler: token doğrulama (tüketmeden) ve konumlu mesaj gönderimi.
 * Frontend EmergencyStatusPage tarafından kullanılır.
 */
@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EmergencyStatus", description = "Acil durum mesajı — token tabanlı, public, konum desteği")
public class EmergencyStatusController {

    private final EmergencyMessageTokenService tokenService;
    private final EmergencyMessageService emergencyMessageService;

    @GetMapping("/token-status")
    @Operation(summary = "Token geçerliliğini doğrular (tüketmez)")
    public ResponseEntity<EmergencyTokenStatusResponse> tokenStatus(@RequestParam String token) {
        return ResponseEntity.ok(tokenService.validateToken(token));
    }

    @PostMapping("/send")
    @Operation(summary = "Acil durum mesajı gönder (konum desteğiyle, JSON yanıt)")
    public ResponseEntity<Map<String, Object>> send(
            @RequestBody EmergencyStatusSendRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = resolveClientIp(httpRequest);
        String userAgent = request.getUserAgent() != null
                ? request.getUserAgent()
                : httpRequest.getHeader("User-Agent");

        EmergencyMessageService.SendResult result = emergencyMessageService.sendStatusMessageWithLocation(
                request.getToken(), request.getType(),
                request.getLatitude(), request.getLongitude(),
                request.getLocationAccuracy(), request.getLocationSource(),
                clientIp, userAgent);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result", result.result().name());
        body.put("success", result.result() == EmergencyMessageService.Result.SUCCESS
                || result.result() == EmergencyMessageService.Result.NO_CONTACTS);
        body.put("sentCount", result.sentCount());
        if (result.messageType() != null) {
            body.put("messageType", result.messageType().name());
        }
        return ResponseEntity.ok(body);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
