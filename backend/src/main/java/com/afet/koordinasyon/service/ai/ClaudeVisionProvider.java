package com.afet.koordinasyon.service.ai;

import com.afet.koordinasyon.domain.enums.AiConfidence;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeVisionProvider implements AiDamageAssessmentProvider {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String SYSTEM_PROMPT = """
            Sen bir yapı hasarı değerlendirme uzmanısın. Sağlanan fotoğrafları inceleyerek kısa bir Türkçe ön değerlendirme yapman gerekiyor.

            KURALLAR:
            - Yanıtın YALNIZCA aşağıdaki JSON formatında olmalı, başka hiçbir metin içermemeli.
            - comment: 1-2 cümle, Türkçe ön değerlendirme yorumu.
            - Kesin yargılar kullanma: "Kesinlikle yıkılmalı", "Kesin güvenli", "Resmi karar" ifadelerini kesinlikle kullanma.
            - confidence: LOW (görsel yetersiz veya hasar belirsiz), MEDIUM (orta düzey hasar işaretleri), HIGH (net ve ciddi hasar)
            - priority: Müdahale önceliği 1 (en acil) - 5 (en az acil). Yıkık/çökmüş binalar 1, hafif hasar 4-5.
            - riskScore: 0-100 arası risk skoru. Yıkık=90-100, ağır hasar=70-89, orta=40-69, hafif=10-39.
            - recommendations: Virgülle ayrılmış kısa aksiyon önerileri (maks 3 madde, Türkçe).

            Yanıtını tam olarak bu JSON formatında ver:
            {"comment": "...", "confidence": "LOW|MEDIUM|HIGH", "priority": 1, "riskScore": 75, "recommendations": "Tahliye önerilebilir, Statik inceleme gerekli, Acil destek ekibi çağırılmalı"}
            """;

    @Value("${ai.claude-api-key:}")
    private String apiKey;

    @Value("${ai.model:claude-3-5-sonnet-latest}")
    private String model;

    private final ObjectMapper objectMapper;

    @Override
    public AiAssessmentResult analyze(List<PhotoData> photos) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 500);
        // System prompt goes in the top-level "system" field, not inside the user message
        body.put("system", SYSTEM_PROMPT);

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        ArrayNode content = userMessage.putArray("content");

        for (PhotoData photo : photos) {
            ObjectNode imageBlock = content.addObject();
            imageBlock.put("type", "image");
            ObjectNode source = imageBlock.putObject("source");
            source.put("type", "base64");
            source.put("media_type", photo.mimeType());
            source.put("data", photo.base64Data());
        }

        ObjectNode textBlock = content.addObject();
        textBlock.put("type", "text");
        textBlock.put("text", "Lütfen sağlanan fotoğrafları analiz ediniz ve yapı hasarı değerlendirmesi yapınız.");

        String requestBody = objectMapper.writeValueAsString(body);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(45))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorBody = response.body();
            log.error("Claude API HTTP {}: {}", response.statusCode(), errorBody);
            throw new RuntimeException("Claude API HTTP " + response.statusCode() + ": " + errorBody);
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String rawText = responseJson.at("/content/0/text").asText();

        String jsonText = rawText.trim();
        if (jsonText.startsWith("```")) {
            jsonText = jsonText.replaceAll("```[a-z]*\\n?", "").replace("```", "").trim();
        }

        JsonNode result = objectMapper.readTree(jsonText);
        String comment = result.path("comment").asText("Değerlendirme yapılamadı.");
        String confidenceStr = result.path("confidence").asText("LOW").toUpperCase();

        AiConfidence confidence;
        try {
            confidence = AiConfidence.valueOf(confidenceStr);
        } catch (IllegalArgumentException e) {
            confidence = AiConfidence.LOW;
        }

        Integer priority = result.hasNonNull("priority") ? result.path("priority").asInt() : null;
        Integer riskScore = result.hasNonNull("riskScore") ? result.path("riskScore").asInt() : null;
        String recommendations = result.hasNonNull("recommendations") ? result.path("recommendations").asText() : null;

        return new AiAssessmentResult(comment, confidence, model, priority, recommendations, riskScore);
    }
}
