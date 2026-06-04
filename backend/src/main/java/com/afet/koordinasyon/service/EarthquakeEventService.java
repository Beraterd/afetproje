package com.afet.koordinasyon.service;

import com.afet.koordinasyon.client.AfadEarthquakeClient;
import com.afet.koordinasyon.client.dto.AfadEventDto;
import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.enums.EarthquakeRiskLevel;
import com.afet.koordinasyon.dto.response.EarthquakeEventResponse;
import com.afet.koordinasyon.dto.response.EarthquakeSyncResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.EarthquakeEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarthquakeEventService {

    private final EarthquakeEventRepository repository;
    private final AfadEarthquakeClient afadClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final List<DateTimeFormatter> AFAD_DATE_PARSERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    );

    @Transactional
    public EarthquakeSyncResponse syncFromAfad(int hoursBefore) {
        long startMs = System.currentTimeMillis();

        String newestDbBefore = repository.findTopByOrderByEventTimeDescCreatedAtAsc()
                .map(e -> e.getExternalId() + " @ " + e.getEventTime())
                .orElse("(henüz kayıt yok)");

        List<AfadEventDto> events = afadClient.fetchRecentEvents(hoursBefore);

        List<String> latestAfadIds = events.stream()
                .limit(10)
                .map(this::buildExternalId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        String newestAfadTime = null;
        if (!events.isEmpty() && events.get(0).getDate() != null) {
            newestAfadTime = events.get(0).getEventId() + " @ " + events.get(0).getDate()
                    + " M" + events.get(0).getMagnitude();
        }

        log.info("AFAD sync: alındı={}, AFAD en yeni=[{}], DB en yeni öncesi=[{}]",
                events.size(), newestAfadTime, newestDbBefore);
        log.info("AFAD ilk 10 EventID: {}", latestAfadIds);

        // Raw AFAD date → parsed eventTime karşılaştırma (ilk 10)
        log.info("=== AFAD parse zinciri (ilk 10) ===");
        for (int i = 0; i < Math.min(10, events.size()); i++) {
            AfadEventDto e = events.get(i);
            OffsetDateTime parsed = parseAfadDate(e.getDate());
            log.info("  [{}] externalId={} rawDate='{}' -> parsedEventTime='{}'",
                    i, buildExternalId(e), e.getDate(), parsed);
        }
        log.info("=== parse zinciri bitti ===");

        // Tüm indeksleri sıfırla — eski eventler NULL olacak
        repository.resetAllAfadOrderIndices();

        int saved = 0;
        int skipped = 0;

        for (int i = 0; i < events.size(); i++) {
            AfadEventDto dto = events.get(i);
            String externalId = buildExternalId(dto);
            if (externalId == null) continue;

            if (repository.existsByExternalId(externalId)) {
                // Mevcut event'e AFAD pozisyonunu güncelle
                repository.updateAfadOrderIndex(externalId, i);
                skipped++;
                continue;
            }
            try {
                EarthquakeEvent entity = toEntity(dto, externalId);
                entity.setAfadOrderIndex(i);
                EarthquakeEvent savedEntity = repository.save(entity);
                eventPublisher.publishEvent(
                        new EarthquakeAlertCreatedEvent(EarthquakeAlert.fromEvent(savedEntity)));
                saved++;
                log.info("Yeni deprem kaydedildi: externalId={} time={} M{} loc={} afadIdx={}",
                        externalId, dto.getDate(), dto.getMagnitude(), dto.getLocation(), i);
            } catch (Exception e) {
                log.warn("Deprem kaydı sırasında hata (externalId={}): {}", externalId, e.getMessage());
            }
        }

        String newestDbAfter = repository.findTopByOrderByEventTimeDescCreatedAtAsc()
                .map(e -> e.getExternalId() + " @ " + e.getEventTime())
                .orElse("(boş)");

        List<String> latestDbIds = repository.findLatestByAfadOrder().stream()
                .map(EarthquakeEvent::getExternalId)
                .collect(Collectors.toList());
        log.info("DB ilk 10 EventID (afad_order_index ASC): {}", latestDbIds);

        long durationMs = System.currentTimeMillis() - startMs;

        log.info("AFAD sync tamamlandı: alındı={}, kaydedildi={}, güncellendi(idx)={}, DB en yeni=[{}], süre={}ms",
                events.size(), saved, skipped, newestDbAfter, durationMs);

        String newestAfadDateRaw = events.isEmpty() ? null : events.get(0).getDate();

        return EarthquakeSyncResponse.builder()
                .fetchedCount(events.size())
                .savedCount(saved)
                .newEventsCount(saved)
                .skippedDuplicateCount(skipped)
                .newestAfadEventTime(newestAfadDateRaw)
                .newestDbEventTime(repository.findTopByOrderByEventTimeDescCreatedAtAsc()
                        .map(e -> e.getEventTime().toString())
                        .orElse(null))
                .durationMs(durationMs)
                .message(String.format("%d yeni kayıt eklendi (%d toplam, %d tekrar, %dms)",
                        saved, events.size(), skipped, durationMs))
                .latestAfadExternalIds(latestAfadIds)
                .latestDbExternalIds(latestDbIds)
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<EarthquakeEventResponse> listEarthquakes(int page, int size) {
        return PagedResponse.from(
                repository.findAllOrdered(PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<EarthquakeEventResponse> getLatest() {
        return repository.findLatestByAfadOrder().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EarthquakeEventResponse getById(UUID id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EarthquakeEvent", "id", id)));
    }

    @Transactional(readOnly = true)
    public Optional<EarthquakeEventResponse> getNewestEvent() {
        return repository.findLatestByAfadOrder().stream()
                .findFirst()
                .map(this::toResponse);
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private String buildExternalId(AfadEventDto dto) {
        if (dto.getEventId() != null && !dto.getEventId().isBlank()) {
            return dto.getEventId();
        }
        if (dto.getDate() != null && dto.getLatitude() != null
                && dto.getLongitude() != null && dto.getMagnitude() != null) {
            return String.format("%s_%.4f_%.4f_%.1f",
                    dto.getDate(), dto.getLatitude(), dto.getLongitude(), dto.getMagnitude());
        }
        return null;
    }

    private EarthquakeEvent toEntity(AfadEventDto dto, String externalId) {
        OffsetDateTime eventTime = parseAfadDate(dto.getDate());

        String rawPayload;
        try {
            rawPayload = objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            rawPayload = null;
        }

        String location = dto.getLocation();
        if (location == null || location.isBlank()) {
            String province = dto.getProvince();
            String district = dto.getDistrict();
            if (province != null && !province.isBlank() && district != null && !district.isBlank()) {
                location = province + " / " + district;
            } else if (province != null && !province.isBlank()) {
                location = province;
            } else if (district != null && !district.isBlank()) {
                location = district;
            } else if (dto.getLatitude() != null && dto.getLongitude() != null) {
                location = String.format("%.4f, %.4f", dto.getLatitude(), dto.getLongitude());
            }
        }

        return EarthquakeEvent.builder()
                .externalId(externalId)
                .eventTime(eventTime)
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .depth(dto.getDepth())
                .magnitude(dto.getMagnitude())
                .location(location)
                .province(dto.getProvince())
                .district(dto.getDistrict())
                .source("AFAD")
                .riskLevel(calculateRiskLevel(dto.getMagnitude()))
                .rawPayload(rawPayload)
                .build();
    }

    private OffsetDateTime parseAfadDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            log.warn("AFAD tarih alanı boş, şimdiki zaman kullanılıyor");
            return OffsetDateTime.now(ISTANBUL);
        }
        try {
            return ZonedDateTime.parse(dateStr).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {}

        // AFAD tarih alanı timezone içermez ve UTC bazlıdır — Istanbul olarak yorumlama.
        for (DateTimeFormatter fmt : AFAD_DATE_PARSERS) {
            try {
                return LocalDateTime.parse(dateStr.trim(), fmt)
                        .atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {}
        }

        log.warn("AFAD tarih parse edilemedi ({}), şimdiki zaman kullanılıyor", dateStr);
        return OffsetDateTime.now(ISTANBUL);
    }

    private EarthquakeRiskLevel calculateRiskLevel(Double magnitude) {
        if (magnitude == null) return EarthquakeRiskLevel.LOW;
        if (magnitude >= 6.0) return EarthquakeRiskLevel.CRITICAL;
        if (magnitude >= 5.0) return EarthquakeRiskLevel.HIGH;
        if (magnitude >= 4.0) return EarthquakeRiskLevel.MEDIUM;
        return EarthquakeRiskLevel.LOW;
    }

    private EarthquakeEventResponse toResponse(EarthquakeEvent entity) {
        return EarthquakeEventResponse.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .eventTime(entity.getEventTime())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .depth(entity.getDepth())
                .magnitude(entity.getMagnitude())
                .location(entity.getLocation())
                .province(entity.getProvince())
                .district(entity.getDistrict())
                .source(entity.getSource())
                .riskLevel(entity.getRiskLevel())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
