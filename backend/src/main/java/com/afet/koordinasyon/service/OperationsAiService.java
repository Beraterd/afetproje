package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.enums.EventStatus;
import com.afet.koordinasyon.domain.enums.ResourceRequestStatus;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.OperationsAiRequest;
import com.afet.koordinasyon.dto.response.OperationsAiResponse;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.EventRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.ResourceRequestRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationsAiService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String SYSTEM_PROMPT = """
            Sen afet koordinasyon merkezi için çalışan bir operasyon analisti asistanısın.

            KRİTİK KURAL: Aşağıda sağlanan GERÇEK SİSTEM VERİLERİNİ kullan.
            - Bölge adı, hasar sayısı veya risk puanı UYDURMAK YASAKTIR.
            - topRiskDistricts veya mahalle risk sıralaması varsa ilçe/mahalle adlarını ve sayısal değerleri açıkça yaz.
            - Hasar verileri varsa sıralı şekilde listele.
            - Herhangi bir veri seti boşsa "Bu veri seti şu an boş." yaz; asla genel yorum yapma.
            - Kesin afet kararı verme; nihai karar her zaman koordinatöre aittir.
            - Yanıtı Türkçe ver.

            ÇIKTI FORMATI (her zaman bu başlıkları kullan):

            ## GENEL DURUM
            (Sistemdeki genel operasyon tablosu — 1-2 cümle)

            ## EN RİSKLİ İLÇELER / MAHALLELER
            (riskScore sıralaması, gerçek sayısal değerlerle)

            ## EN YOĞUN HASAR ALAN BÖLGELER
            (hasar kaydı sayısına göre sıralı)

            ## KAYNAK ÖNCELİKLERİ
            (açık kaynak talebi sayısına göre sıralı)

            ## OPERASYON ÖNERİSİ
            (En yüksek riskli bölge için somut, uygulanabilir adım)
            """;

    private static final List<EventStatus> ACTIVE_EVENT_STATUSES =
            List.of(EventStatus.OPEN, EventStatus.IN_PROGRESS);
    private static final List<ResourceRequestStatus> ACTIVE_RESOURCE_STATUSES =
            List.of(ResourceRequestStatus.OPEN, ResourceRequestStatus.IN_PROGRESS);

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.claude-api-key:}")
    private String apiKey;

    @Value("${ai.model:claude-sonnet-4-6}")
    private String model;

    private final DamageAssessmentRepository damageAssessmentRepository;
    private final ResourceRequestRepository resourceRequestRepository;
    private final EventRepository eventRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final RiskCalculationService riskCalculationService;
    private final ObjectMapper objectMapper;

    // ── Public entry point ────────────────────────────────────────────────────

    public OperationsAiResponse analyze(OperationsAiRequest request, UserPrincipal principal) {
        if (!aiEnabled || apiKey == null || apiKey.isBlank()) {
            return new OperationsAiResponse(
                    "AI servisi şu anda yapılandırılmamış veya devre dışı. Lütfen sistem yöneticinize başvurun.",
                    Instant.now()
            );
        }

        OffsetDateTime since = OffsetDateTime.now().minusHours(24);

        String contextData;
        try {
            contextData = buildContextData(principal, since);
        } catch (Exception e) {
            log.error("buildContextData failed for user {}: {}", principal.getEmail(), e.getMessage(), e);
            contextData = "Risk verisi eksik veya format hatalı.";
        }

        String enrichedPrompt = "=== SİSTEM ANALİTİK VERİLERİ ===\n" + contextData
                + "\n=== YÖNETİCİ SORUSU ===\n" + request.prompt();

        try {
            String answer = callClaudeApi(enrichedPrompt);
            return new OperationsAiResponse(answer, Instant.now());
        } catch (Exception e) {
            log.error("Operations AI analysis failed for user {}: {}", principal.getEmail(), e.getMessage());
            return new OperationsAiResponse(
                    "AI analizi sırasında bir hata oluştu. Lütfen birkaç saniye sonra tekrar deneyin.",
                    Instant.now()
            );
        }
    }

    // ── Context router ────────────────────────────────────────────────────────

    private String buildContextData(UserPrincipal principal, OffsetDateTime since) {
        UserRole role = principal.getRole();
        UUID districtId = principal.getDistrictId();
        UUID neighborhoodId = principal.getNeighborhoodId();

        StringBuilder sb = new StringBuilder();
        sb.append("Tarih/Saat: ").append(OffsetDateTime.now()).append("\n");
        sb.append("Analiz Penceresi: Son 24 saat\n");
        sb.append("Yetkili Rol: ").append(role.name()).append("\n\n");

        if (role == UserRole.ADMIN) {
            appendAdminAnalytics(sb, since);
        } else if (role == UserRole.DISTRICT_COORDINATOR && districtId != null) {
            appendDistrictAnalytics(sb, districtId, since);
        } else if (role == UserRole.NEIGHBORHOOD_COORDINATOR && neighborhoodId != null) {
            appendNeighborhoodAnalytics(sb, neighborhoodId, since);
        } else {
            sb.append("Bu rol için analitik kapsam tanımlı değil.\n");
        }

        return sb.toString();
    }

    // ── Admin analytics (tüm ilçeler) ─────────────────────────────────────────

    private void appendAdminAnalytics(StringBuilder sb, OffsetDateTime since) {
        appendTopRiskDistricts(sb);
        appendTopDamageDistricts(sb, since);
        appendTopResourceDistricts(sb);
        long eventCount = eventRepository.countByStatusIn(ACTIVE_EVENT_STATUSES);
        sb.append("[AKTİF GÖREVLER]\n");
        sb.append("  Toplam aktif görev: ").append(eventCount).append("\n\n");
        appendRiskFlags(sb, damageAssessmentRepository.findRiskFlagsSummary(since));
    }

    private void appendTopRiskDistricts(StringBuilder sb) {
        sb.append("[EN RİSKLİ İLÇELER — Risk Skoru Sıralaması]\n");
        List<District> sorted = districtRepository.findByActiveTrue().stream()
                .filter(d -> d.getRiskScore() != null && d.getRiskScore().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(District::getRiskScore).reversed())
                .limit(5)
                .toList();

        if (sorted.isEmpty()) {
            sb.append("  Risk skoru hesaplanmış ilçe bulunmamaktadır.\n\n");
            return;
        }
        for (int i = 0; i < sorted.size(); i++) {
            District d = sorted.get(i);
            sb.append(String.format("  %d. %s | riskScore=%.0f | Seviye=%s | Renk=%s\n",
                    i + 1,
                    d.getName(),
                    d.getRiskScore().doubleValue(),
                    riskCalculationService.getRiskLevel(d.getRiskScore()),
                    riskCalculationService.getRiskColor(d.getRiskScore())));
        }
        sb.append("\n");
    }

    private void appendTopDamageDistricts(StringBuilder sb, OffsetDateTime since) {
        sb.append("[EN FAZLA HASAR KAYDEDİLEN İLÇELER — Son 24 Saat]\n");
        List<Object[]> rows = damageAssessmentRepository.findDistrictDamageSummary(since);
        if (rows.isEmpty()) {
            sb.append("  Son 24 saatte hasar kaydı bulunmamaktadır.\n\n");
            return;
        }

        // rows: [districtName, damageLevel, count] — aggregate by districtName
        Map<String, long[]> agg = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String name  = row[0] != null ? row[0].toString() : "Bilinmeyen";
            String level = row[1] != null ? row[1].toString() : "";
            long count   = toLong(row[2]);
            agg.computeIfAbsent(name, k -> new long[]{0L, 0L});
            agg.get(name)[0] += count;
            if ("HEAVY".equals(level) || "COLLAPSED".equals(level)) {
                agg.get(name)[1] += count;
            }
        }

        agg.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .forEach(e -> sb.append(String.format(
                        "  - %s | toplam=%d kayıt | kritik(HEAVY+COLLAPSED)=%d\n",
                        e.getKey(), e.getValue()[0], e.getValue()[1])));
        sb.append("\n");
    }

    private void appendTopResourceDistricts(StringBuilder sb) {
        sb.append("[AÇIK KAYNAK TALEPLERİ — İlçe Bazında]\n");
        List<Object[]> rows = resourceRequestRepository.findOpenRequestCountByDistrict();
        if (rows.isEmpty()) {
            sb.append("  Açık kaynak talebi bulunmamaktadır.\n\n");
            return;
        }

        Map<UUID, String> nameMap = districtRepository.findByActiveTrue().stream()
                .collect(Collectors.toMap(District::getId, District::getName));

        rows.stream()
                .sorted((a, b) -> Long.compare(toLong(b[1]), toLong(a[1])))
                .limit(5)
                .forEach(row -> {
                    UUID id    = row[0] instanceof UUID ? (UUID) row[0] : null;
                    long count = toLong(row[1]);
                    String name = id != null ? nameMap.getOrDefault(id, id.toString()) : "Bilinmeyen";
                    sb.append(String.format("  - %s | %d açık talep\n", name, count));
                });
        sb.append("\n");
    }

    // ── District coordinator analytics ────────────────────────────────────────

    private void appendDistrictAnalytics(StringBuilder sb, UUID districtId, OffsetDateTime since) {
        districtRepository.findById(districtId).ifPresent(d -> {
            BigDecimal score = d.getRiskScore() != null ? d.getRiskScore() : BigDecimal.ZERO;
            sb.append("[İLÇE: ").append(d.getName()).append("]\n");
            sb.append(String.format("  İlçe Risk Skoru: %.0f | Seviye=%s | Renk=%s\n\n",
                    score.doubleValue(),
                    riskCalculationService.getRiskLevel(score),
                    riskCalculationService.getRiskColor(score)));
        });

        sb.append("[MAHALLE RİSK SIRALAMASI]\n");
        List<Neighborhood> sortedNeighborhoods = neighborhoodRepository.findByDistrictId(districtId).stream()
                .filter(n -> n.getRiskScore() != null && n.getRiskScore().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Neighborhood::getRiskScore).reversed())
                .limit(5)
                .toList();
        if (sortedNeighborhoods.isEmpty()) {
            sb.append("  Risk skoru hesaplanmış mahalle bulunmamaktadır.\n");
        } else {
            for (int i = 0; i < sortedNeighborhoods.size(); i++) {
                Neighborhood n = sortedNeighborhoods.get(i);
                sb.append(String.format("  %d. %s | riskScore=%.0f | Seviye=%s\n",
                        i + 1, n.getName(),
                        n.getRiskScore().doubleValue(),
                        riskCalculationService.getRiskLevel(n.getRiskScore())));
            }
        }
        sb.append("\n");

        sb.append("[HASAR KAYITLARI — Son 24 Saat]\n");
        List<Object[]> damageRows = damageAssessmentRepository
                .findNeighborhoodDamageSummaryByDistrict(districtId, since);
        if (damageRows.isEmpty()) {
            sb.append("  Son 24 saatte ilçenizde hasar kaydı bulunmamaktadır.\n");
        } else {
            damageRows.forEach(r -> sb.append("  ").append(r[0])
                    .append(" | ").append(r[1])
                    .append(": ").append(r[2]).append(" kayıt\n"));
        }
        appendRiskFlags(sb, damageAssessmentRepository.findRiskFlagsSummaryByDistrict(districtId, since));
        sb.append("\n");

        sb.append("[KAYNAK TALEPLERİ]\n");
        List<Object[]> resourceRows = resourceRequestRepository
                .findResourceTypeSummaryByDistrict(districtId, ACTIVE_RESOURCE_STATUSES);
        if (resourceRows.isEmpty()) {
            sb.append("  Aktif kaynak talebi bulunmamaktadır.\n");
        } else {
            resourceRows.forEach(r -> sb.append("  ").append(r[0])
                    .append(": ").append(r[1]).append(" talep\n"));
        }
        sb.append("\n");

        sb.append("[AKTİF GÖREVLER]\n");
        long eventCount = eventRepository.countByDistrictIdAndStatusIn(districtId, ACTIVE_EVENT_STATUSES);
        sb.append("  Aktif görev sayısı: ").append(eventCount).append("\n\n");
    }

    // ── Neighborhood coordinator analytics ────────────────────────────────────

    private void appendNeighborhoodAnalytics(StringBuilder sb, UUID neighborhoodId, OffsetDateTime since) {
        neighborhoodRepository.findById(neighborhoodId).ifPresent(n -> {
            BigDecimal score = n.getRiskScore() != null ? n.getRiskScore() : BigDecimal.ZERO;
            sb.append("[MAHALLE: ").append(n.getName());
            if (n.getDistrict() != null) sb.append(" / ").append(n.getDistrict().getName());
            sb.append("]\n");
            sb.append(String.format("  Mahalle Risk Skoru: %.0f | Seviye=%s | Renk=%s\n\n",
                    score.doubleValue(),
                    riskCalculationService.getRiskLevel(score),
                    riskCalculationService.getRiskColor(score)));
        });

        sb.append("[HASAR KAYITLARI — Son 24 Saat]\n");
        List<Object[]> damageRows = damageAssessmentRepository
                .findDamageLevelSummaryByNeighborhood(neighborhoodId, since);
        if (damageRows.isEmpty()) {
            sb.append("  Son 24 saatte mahallenizde hasar kaydı bulunmamaktadır.\n");
        } else {
            damageRows.forEach(r -> sb.append("  ").append(r[0])
                    .append(": ").append(r[1]).append(" kayıt\n"));
        }
        appendRiskFlags(sb, damageAssessmentRepository.findRiskFlagsSummaryByNeighborhood(neighborhoodId, since));
        sb.append("\n");

        sb.append("[KAYNAK TALEPLERİ]\n");
        List<Object[]> resourceRows = resourceRequestRepository
                .findResourceTypeSummaryByNeighborhood(neighborhoodId, ACTIVE_RESOURCE_STATUSES);
        if (resourceRows.isEmpty()) {
            sb.append("  Aktif kaynak talebi bulunmamaktadır.\n");
        } else {
            resourceRows.forEach(r -> sb.append("  ").append(r[0])
                    .append(": ").append(r[1]).append(" talep\n"));
        }
        sb.append("\n");

        sb.append("[AKTİF GÖREVLER]\n");
        long eventCount = eventRepository.countByNeighborhoodIdAndStatusIn(neighborhoodId, ACTIVE_EVENT_STATUSES);
        sb.append("  Aktif görev sayısı: ").append(eventCount).append("\n\n");
    }

    // ── Risk flags helper ─────────────────────────────────────────────────────

    private void appendRiskFlags(StringBuilder sb, Object[] risks) {
        if (risks == null) return;
        if (risks.length < 4) {
            log.warn("Malformed risk flags array: expected 4 elements, got {}", risks.length);
            return;
        }
        if (risks[0] == null && risks[1] == null && risks[2] == null && risks[3] == null) return;
        long collapse   = toLong(risks[0]);
        long gas        = toLong(risks[1]);
        long evac       = toLong(risks[2]);
        long casualties = toLong(risks[3]);
        if (collapse > 0 || gas > 0 || evac > 0 || casualties > 0) {
            sb.append("  Kritik Risk Bayrakları → ")
              .append("Çökme riski: ").append(collapse)
              .append(" | Gaz sızıntısı: ").append(gas)
              .append(" | Tahliye gerekli: ").append(evac)
              .append(" | Kayıp şüphesi: ").append(casualties).append("\n");
        }
    }

    // ── Claude API call ───────────────────────────────────────────────────────

    private String callClaudeApi(String userMessage) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 2000);
        body.put("system", SYSTEM_PROMPT);

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

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
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Claude API HTTP {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("Claude API döndü: HTTP " + response.statusCode());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        return responseJson.at("/content/0/text").asText();
    }

    private long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }
}
