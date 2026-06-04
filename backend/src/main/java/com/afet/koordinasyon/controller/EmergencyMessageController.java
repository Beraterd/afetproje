package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.service.EmergencyMessageService;
import com.afet.koordinasyon.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * §4-6/§9-10 — Deprem e-postasındaki "Yakınlarıma Mesaj Gönder" linklerinin PUBLIC giriş noktası.
 *
 * <p>Kimlik token üzerinden doğrulanır (oturum gerektirmez). İşlem sonrası kullanıcı
 * frontend'de güvenli bir sonuç sayfasına yönlendirilir. Düz token loglanmaz.
 */
@RestController
@RequestMapping("/api/emergency-message")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EmergencyMessage", description = "Yakınlara acil durum mesajı (token tabanlı, public)")
public class EmergencyMessageController {

    private final EmergencyMessageService emergencyMessageService;
    private final ShortLinkService shortLinkService;

    @GetMapping("/send")
    @Operation(summary = "Hazır mesajı kullanıcının yakınlarına gönderir (token + type ile). "
            + "Sonuç sayfasına 302 ile yönlendirir.")
    public ResponseEntity<Void> send(@RequestParam String token, @RequestParam String type) {
        EmergencyMessageService.SendResult result;
        try {
            result = emergencyMessageService.sendStatusMessage(token, type);
        } catch (Exception e) {
            log.error("Acil mesaj gönderiminde beklenmeyen hata: {}", e.getMessage());
            result = new EmergencyMessageService.SendResult(EmergencyMessageService.Result.ERROR, 0);
        }

        String redirect = buildResultRedirect(result);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirect)).build();
    }

    private String buildResultRedirect(EmergencyMessageService.SendResult result) {
        String status = result.result().name();
        String query = "?status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);
        if (result.result() == EmergencyMessageService.Result.SUCCESS) {
            query += "&count=" + result.sentCount();
        }
        return shortLinkService.buildFrontendUrl("/emergency-message-result" + query);
    }
}
