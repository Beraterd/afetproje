package com.afet.koordinasyon.client;

import com.afet.koordinasyon.client.dto.AfadEventDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AfadEarthquakeClient {

    // AFAD tarihleri ve query parametreleri Turkey Standard Time (UTC+3) bazlıdır.
    static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter AFAD_QUERY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ISTANBUL);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String primaryUrl;
    private final String fallbackUrl;

    public AfadEarthquakeClient(
            RestTemplateBuilder builder,
            ObjectMapper objectMapper,
            @Value("${app.afad.base-url:https://deprem.afad.gov.tr/apiv2/event/filter}") String primaryUrl,
            @Value("${app.afad.fallback-url:https://deprem.afad.gov.tr/EventData/GetEventsByFilter}") String fallbackUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(45))
                .build();
        this.objectMapper = objectMapper;
        this.primaryUrl = primaryUrl;
        this.fallbackUrl = fallbackUrl;
    }

    public List<AfadEventDto> fetchRecentEvents(int hoursBefore) {
        ZonedDateTime nowIst   = ZonedDateTime.now(ISTANBUL);
        ZonedDateTime startIst = nowIst.minusHours(hoursBefore);

        log.info("AFAD fetch başladı: start={} end={} (Istanbul TZ) hoursBefore={}",
                AFAD_QUERY_FMT.format(startIst), AFAD_QUERY_FMT.format(nowIst), hoursBefore);

        List<AfadEventDto> events = fetchFromPrimary(startIst, nowIst);

        if (events.isEmpty()) {
            log.warn("Primary AFAD endpoint boş döndü, fallback deneniyor...");
            events = fetchFromFallback(startIst, nowIst);
        }

        if (events.isEmpty()) {
            log.warn("Her iki AFAD endpoint de boş döndü");
            return Collections.emptyList();
        }

        log.info("AFAD fetch tamamlandı: toplam {} event (AFAD raw sırası korundu)", events.size());
        logTopEvents(events, 10);

        return events;
    }

    // ── Primary: GET /apiv2/event/filter ──────────────────────────────────────

    private List<AfadEventDto> fetchFromPrimary(ZonedDateTime startIst, ZonedDateTime nowIst) {
        String url = UriComponentsBuilder.fromHttpUrl(primaryUrl)
                .queryParam("start",   AFAD_QUERY_FMT.format(startIst))
                .queryParam("end",     AFAD_QUERY_FMT.format(nowIst))
                .queryParam("minmag",  "-1")
                .queryParam("maxmag",  "10")
                .queryParam("orderby", "timedesc")
                .queryParam("limit",   "200")
                .queryParam("format",  "json")
                .queryParam("_t",      System.currentTimeMillis())
                .build()
                .toUriString();

        log.info("[PRIMARY] GET {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return parseListResponse("[PRIMARY]", response.getBody());

        } catch (Exception e) {
            log.error("[PRIMARY] Hata: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Fallback: POST /EventData/GetEventsByFilter ───────────────────────────

    private List<AfadEventDto> fetchFromFallback(ZonedDateTime startIst, ZonedDateTime nowIst) {
        String startStr = AFAD_QUERY_FMT.format(startIst);
        String endStr   = AFAD_QUERY_FMT.format(nowIst);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("EventSearchFilterList", List.of(
                Map.of("FilterType", 8, "Value", startStr),
                Map.of("FilterType", 9, "Value", endStr)
        ));
        body.put("Skip", 0);
        body.put("Take", 200);
        body.put("SortDescriptor", Map.of("field", "eventDate", "dir", "desc"));

        log.info("[FALLBACK] POST {} body={}", fallbackUrl, body);

        try {
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            ResponseEntity<String> response = restTemplate.exchange(
                    fallbackUrl, HttpMethod.POST, new HttpEntity<>(bodyJson, headers), String.class);

            // Fallback yanıtı direkt liste veya sarılı obje olabilir
            if (response.getBody() == null || response.getBody().isBlank()) {
                log.warn("[FALLBACK] Boş yanıt");
                return Collections.emptyList();
            }

            String raw = response.getBody().trim();

            if (raw.startsWith("[")) {
                return parseListResponse("[FALLBACK]", raw);
            }

            // Sarılı obje: {"EventList":[...]} veya {"data":[...]} vb.
            JsonNode root = objectMapper.readTree(raw);
            for (String key : new String[]{"EventList", "eventList", "data", "events", "result"}) {
                JsonNode node = root.get(key);
                if (node != null && node.isArray()) {
                    List<AfadEventDto> events = objectMapper.readValue(
                            node.toString(), new TypeReference<List<AfadEventDto>>() {});
                    log.info("[FALLBACK] {} event döndü (key={})", events.size(), key);
                    return events;
                }
            }

            log.error("[FALLBACK] Yanıt formatı tanınamadı: {}",
                    raw.substring(0, Math.min(300, raw.length())));
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("[FALLBACK] Hata: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<AfadEventDto> parseListResponse(String tag, String body) {
        if (body == null || body.isBlank()) {
            log.warn("{} Boş yanıt", tag);
            return Collections.emptyList();
        }
        try {
            List<AfadEventDto> events = objectMapper.readValue(
                    body, new TypeReference<List<AfadEventDto>>() {});
            log.info("{} {} event döndü", tag, events.size());
            return events;
        } catch (Exception e) {
            log.error("{} Liste parse hatası: {} | body prefix: {}",
                    tag, e.getMessage(), body.substring(0, Math.min(200, body.length())));
            return Collections.emptyList();
        }
    }

    private void logTopEvents(List<AfadEventDto> events, int count) {
        int n = Math.min(count, events.size());
        for (int i = 0; i < n; i++) {
            AfadEventDto e = events.get(i);
            log.info("AFAD top[{}]: id={} date={} M={} loc={}",
                    i, e.getEventId(), e.getDate(), e.getMagnitude(), e.getLocation());
        }
    }

}
