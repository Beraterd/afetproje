package com.afet.koordinasyon.notification;

import com.afet.koordinasyon.config.WhatsAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * WhatsApp Business Cloud API HTTP istemcisi.
 * POST {baseUrl}/{apiVersion}/{phoneNumberId}/messages — Bearer token ile.
 * <p>
 * GÜVENLİK: accessToken hiçbir log satırına yazılmaz. Hata response'ları
 * (token içermez) loglanır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppCloudClient {

    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Düz metin mesajı gönderir (24 saatlik servis penceresi / test için). */
    public WhatsAppSendResult sendText(String toE164, String body) {
        ObjectNode payload = basePayload(toE164);
        payload.put("type", "text");
        ObjectNode text = payload.putObject("text");
        text.put("preview_url", true);
        text.put("body", body);
        return post(payload);
    }

    /**
     * Onaylı template mesajı gönderir. bodyParams sırayla {{1}}, {{2}}, ... yerine geçer.
     */
    public WhatsAppSendResult sendTemplate(String toE164, String templateName,
                                           String languageCode, List<String> bodyParams) {
        ObjectNode payload = basePayload(toE164);
        payload.put("type", "template");

        ObjectNode template = payload.putObject("template");
        template.put("name", templateName);
        template.putObject("language").put("code", languageCode);

        if (bodyParams != null && !bodyParams.isEmpty()) {
            ArrayNode components = template.putArray("components");
            ObjectNode bodyComponent = components.addObject();
            bodyComponent.put("type", "body");
            ArrayNode parameters = bodyComponent.putArray("parameters");
            for (String param : bodyParams) {
                ObjectNode p = parameters.addObject();
                p.put("type", "text");
                p.put("text", param == null ? "" : param);
            }
        }
        return post(payload);
    }

    private ObjectNode basePayload(String toE164) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", toE164);
        return payload;
    }

    private WhatsAppSendResult post(ObjectNode payload) {
        if (properties.getPhoneNumberId() == null || properties.getPhoneNumberId().isBlank()) {
            return WhatsAppSendResult.fail("WhatsApp phoneNumberId yapılandırılmamış");
        }
        if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            return WhatsAppSendResult.fail("WhatsApp accessToken yapılandırılmamış");
        }

        String url = String.format("%s/%s/%s/messages",
                trimTrailingSlash(properties.getBaseUrl()),
                properties.getApiVersion(),
                properties.getPhoneNumberId());

        try {
            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = objectMapper.readTree(response.body());
                String wamid = json.at("/messages/0/id").asText(null);
                return WhatsAppSendResult.ok(wamid);
            }

            // Hata gövdesi token içermez; güvenle loglanabilir.
            log.error("WhatsApp Cloud API hatası: HTTP {} body={}", response.statusCode(), response.body());
            JsonNode json = safeReadTree(response.body());
            String apiMsg = json != null
                    ? json.at("/error/message").asText("HTTP " + response.statusCode())
                    : "HTTP " + response.statusCode();
            return WhatsAppSendResult.fail(apiMsg);

        } catch (Exception e) {
            log.error("WhatsApp Cloud API gönderilemedi: {}", e.getMessage());
            return WhatsAppSendResult.fail(e.getMessage());
        }
    }

    private JsonNode safeReadTree(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
