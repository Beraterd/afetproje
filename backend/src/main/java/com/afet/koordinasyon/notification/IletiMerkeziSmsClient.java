package com.afet.koordinasyon.notification;

import com.afet.koordinasyon.config.SmsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
@Slf4j
public class IletiMerkeziSmsClient {

    private final SmsProperties smsProperties;
    private final ObjectMapper objectMapper;

    public SmsSendResult send(String normalizedPhone, String text) {
        try {
            String hash = sha256(smsProperties.getApiKey() + smsProperties.getApiSecret());

            ObjectNode authNode = objectMapper.createObjectNode();
            authNode.put("key", smsProperties.getApiKey());
            authNode.put("hash", hash);

            ObjectNode receipentsNode = objectMapper.createObjectNode();
            receipentsNode.putArray("number").add(normalizedPhone);

            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("text", text);
            messageNode.set("receipents", receipentsNode);

            ObjectNode orderNode = objectMapper.createObjectNode();
            orderNode.put("sender", smsProperties.getSender());
            orderNode.put("sendDateTime", "");
            orderNode.set("message", messageNode);

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.set("authentication", authNode);
            requestNode.set("order", orderNode);

            ObjectNode body = objectMapper.createObjectNode();
            body.set("request", requestNode);

            String requestBody = objectMapper.writeValueAsString(body);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(smsProperties.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("İletiMerkezi HTTP {}: {}", response.statusCode(), response.body());
                return SmsSendResult.fail("HTTP " + response.statusCode());
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode statusNode = responseJson.at("/response/status/code");
            String code = statusNode.asText("");

            if ("200".equals(code)) {
                String orderId = responseJson.at("/response/order/id").asText(null);
                return SmsSendResult.ok(orderId);
            } else {
                String message = responseJson.at("/response/status/message").asText("Bilinmeyen hata");
                log.error("İletiMerkezi API hatası: code={}, message={}", code, message);
                return SmsSendResult.fail("API hata kodu " + code + ": " + message);
            }

        } catch (Exception e) {
            log.error("İletiMerkezi SMS gönderilemedi: {}", e.getMessage());
            return SmsSendResult.fail(e.getMessage());
        }
    }

    private static String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
