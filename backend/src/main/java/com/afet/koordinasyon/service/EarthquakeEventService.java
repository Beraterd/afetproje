package com.afet.koordinasyon.service;

import com.afet.koordinasyon.client.AfadEarthquakeClient;
import com.afet.koordinasyon.client.KandilliEarthquakeClient;
import com.afet.koordinasyon.client.dto.AfadEventDto;
import com.afet.koordinasyon.client.dto.KandilliEventDto;
import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.enums.EarthquakeRiskLevel;
import com.afet.koordinasyon.domain.enums.EarthquakeSource;
import com.afet.koordinasyon.dto.response.EarthquakeEventResponse;
import com.afet.koordinasyon.dto.response.EarthquakeSyncResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.earthquake.EarthquakeLocationResolver;
import com.afet.koordinasyon.earthquake.PhysicalEarthquakeMatcher;
import com.afet.koordinasyon.earthquake.TurkishProvinces;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.EarthquakeEventRepository;
import com.afet.koordinasyon.repository.EarthquakeEventSpecification;
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
    private final KandilliEarthquakeClient kandilliClient;
    private final EarthquakeLocationResolver locationResolver;
    private final PhysicalEarthquakeMatcher physicalEarthquakeMatcher;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final List<DateTimeFormatter> AFAD_DATE_PARSERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    );

    // ── AFAD sync ────────────────────────────────────────────────────────────

    @Transactional
    public EarthquakeSyncResponse syncFromAfad(int hoursBefore) {
        long startMs = System.currentTimeMillis();

        String newestDbBefore = repository.findTopByOrderByEventTimeDescCreatedAtAsc()
                .map(e -> e.getExternalId() + " @ " + e.getEventTime())
                .orElse("(henüz kayıt yok)");

        List<AfadEventDto> events = afadClient.fetchRecentEvents(hoursBefore);

        List<String> latestAfadIds = events.stream()
                .limit(10)
                .map(this::buildAfadExternalId)
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

        // Tüm AFAD indekslerini sıfırla — eski eventler NULL olacak
        repository.resetAllAfadOrderIndices();

        int saved = 0;
        int skipped = 0;

        for (int i = 0; i < events.size(); i++) {
            AfadEventDto dto = events.get(i);
            String externalId = buildAfadExternalId(dto);
            if (externalId == null) continue;

            if (repository.existsBySourceAndExternalId(EarthquakeSource.AFAD, externalId)) {
                repository.updateAfadOrderIndex(externalId, i);
                skipped++;
                continue;
            }
            try {
                EarthquakeEvent entity = toEntityFromAfad(dto, externalId);
                entity.setAfadOrderIndex(i);
                saveAndNotify(entity);
                saved++;
                log.info("Yeni deprem kaydedildi: source=AFAD externalId={} time={} M{} loc={} afadIdx={}",
                        externalId, dto.getDate(), dto.getMagnitude(), dto.getLocation(), i);
            } catch (Exception e) {
                log.warn("Deprem kaydı sırasında hata (source=AFAD, externalId={}): {}", externalId, e.getMessage());
            }
        }

        String newestDbAfter = repository.findTopByOrderByEventTimeDescCreatedAtAsc()
                .map(e -> e.getExternalId() + " @ " + e.getEventTime())
                .orElse("(boş)");

        List<String> latestDbIds = repository.findLatestByAfadOrder().stream()
                .map(EarthquakeEvent::getExternalId)
                .collect(Collectors.toList());

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

    // ── Kandilli sync ────────────────────────────────────────────────────────

    /**
     * AFAD sync'inden TAMAMEN bağımsız — Kandilli erişilemezse burada fırlatılan/loglanan
     * hata AFAD sync'ini etkilemez (ayrı scheduled task, ayrı try/catch — bkz.
     * KandilliEarthquakePollingService).
     */
    @Transactional
    public EarthquakeSyncResponse syncFromKandilli(int hoursBefore) {
        long startMs = System.currentTimeMillis();

        List<KandilliEventDto> events = kandilliClient.fetchRecentEvents(hoursBefore);

        int saved = 0;
        int skipped = 0;

        for (KandilliEventDto dto : events) {
            Double magnitude = dto.resolvedMagnitude();
            if (magnitude == null) continue; // parser zaten eledi ama savunma amaçlı

            String externalId = buildKandilliExternalId(dto, magnitude);
            if (repository.existsBySourceAndExternalId(EarthquakeSource.KANDILLI, externalId)) {
                skipped++;
                continue;
            }
            try {
                EarthquakeEvent entity = toEntityFromKandilli(dto, externalId, magnitude);
                saveAndNotify(entity);
                saved++;
                log.info("Yeni deprem kaydedildi: source=KANDILLI externalId={} time={} M{} loc={}",
                        externalId, entity.getEventTime(), magnitude, dto.rawLocation());
            } catch (Exception e) {
                log.warn("Deprem kaydı sırasında hata (source=KANDILLI, externalId={}): {}", externalId, e.getMessage());
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        log.info("Kandilli sync tamamlandı: alındı={}, kaydedildi={}, tekrar={}, süre={}ms",
                events.size(), saved, skipped, durationMs);

        return EarthquakeSyncResponse.builder()
                .fetchedCount(events.size())
                .savedCount(saved)
                .newEventsCount(saved)
                .skippedDuplicateCount(skipped)
                .durationMs(durationMs)
                .message(String.format("%d yeni kayıt eklendi (%d toplam, %d tekrar, %dms)",
                        saved, events.size(), skipped, durationMs))
                .build();
    }

    // ── Ortak: kayıt + cross-source bildirim dedup (item 17) ────────────────

    private void saveAndNotify(EarthquakeEvent entity) {
        EarthquakeEvent saved = repository.save(entity);

        EarthquakeEvent duplicateOf = findRecentCrossSourceDuplicate(saved);
        if (duplicateOf != null) {
            saved.setNotificationSent(true);
            saved.setNotificationSentAt(OffsetDateTime.now());
            repository.save(saved);
            log.info("Bildirim bastırıldı — aynı fiziksel deprem başka kaynaktan zaten bildirilmiş: " +
                            "yeni={}/{} zatenBildirilen={}/{}",
                    saved.getSource(), saved.getExternalId(), duplicateOf.getSource(), duplicateOf.getExternalId());
            return;
        }

        eventPublisher.publishEvent(new EarthquakeAlertCreatedEvent(EarthquakeAlert.fromEvent(saved)));
    }

    private EarthquakeEvent findRecentCrossSourceDuplicate(EarthquakeEvent candidate) {
        if (candidate.getEventTime() == null) {
            return null;
        }
        OffsetDateTime from = candidate.getEventTime().minus(PhysicalEarthquakeMatcher.TIME_TOLERANCE);
        OffsetDateTime to = candidate.getEventTime().plus(PhysicalEarthquakeMatcher.TIME_TOLERANCE);
        List<EarthquakeEvent> candidates = repository.findBySourceNotAndNotificationSentTrueAndEventTimeBetween(
                candidate.getSource(), from, to);
        return candidates.stream()
                .filter(other -> physicalEarthquakeMatcher.isSamePhysicalEvent(candidate, other))
                .findFirst()
                .orElse(null);
    }

    // ── Listeleme / filtreleme ───────────────────────────────────────────────

    public record EarthquakeFilter(
            EarthquakeSource source,
            String province,
            String district,
            Double minMagnitude,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    ) {
    }

    /**
     * Hem filtreli hem filtresiz (source dahil hiçbir kriter verilmemiş — "Tüm Kaynaklar")
     * çağrılar AYNI yoldan geçer: {@link EarthquakeEventSpecification} + global {@code eventTime
     * DESC} sıralaması. Filtre boşsa Specification hiçbir predicate üretmez (tüm kayıtlar eşleşir)
     * — bu, AFAD ve Kandilli kayıtlarını GERÇEKTEN eventTime'a göre iç içe (interleaved) birleştirir.
     * <p>
     * Eskiden "filtresiz" çağrı {@code findAllOrdered()}'a gidiyordu; o sorgu AFAD'ın kendi
     * afadOrderIndex'ini eventTime'dan önce önceliklendiriyordu — Kandilli kayıtları bu alanı
     * hiç kullanmadığından (her zaman NULL), "Tüm Kaynaklar" görünümünde gerçek zamanından
     * bağımsız olarak TÜM AFAD kayıtlarının arkasına düşüyorlardı. Bu metod artık AFAD'ın ham
     * batch sırasını YOK SAYAR — o sıralama yalnızca /latest ve /debug/status'un kasıtlı olarak
     * AFAD'a özel debug görünümünde kalır (bkz. {@code findLatestByAfadOrder}).
     */
    @Transactional(readOnly = true)
    public PagedResponse<EarthquakeEventResponse> listEarthquakes(int page, int size, EarthquakeFilter filter) {
        EarthquakeFilter effectiveFilter = filter != null ? filter : EMPTY_FILTER;

        String provinceNormalized = com.afet.koordinasyon.earthquake.LocationNormalizer.normalize(effectiveFilter.province());
        String districtNormalized = com.afet.koordinasyon.earthquake.LocationNormalizer.normalize(effectiveFilter.district());

        // Criteria API (EarthquakeEventSpecification) — yalnızca verilen filtreler için
        // predicate üretir, null tarih/parametreler hiç bind edilmez. Eski JPQL
        // "(:param IS NULL OR ...)" deseni PostgreSQL'de timestamptz parametreleri null
        // geçildiğinde tip çıkarımı hatasıyla (500) patlıyordu — bkz. EarthquakeEventRepository.
        var spec = EarthquakeEventSpecification.withFilters(
                effectiveFilter.source(), provinceNormalized, districtNormalized,
                effectiveFilter.minMagnitude(), effectiveFilter.startDate(), effectiveFilter.endDate());
        var pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "eventTime"));

        return PagedResponse.from(repository.findAll(spec, pageable).map(this::toResponse));
    }

    private static final EarthquakeFilter EMPTY_FILTER =
            new EarthquakeFilter(null, null, null, null, null, null);

    @Transactional(readOnly = true)
    public List<String> getProvinces() {
        return TurkishProvinces.ALL;
    }

    @Transactional(readOnly = true)
    public List<String> getDistrictsForProvince(String province) {
        String provinceNormalized = com.afet.koordinasyon.earthquake.LocationNormalizer.normalize(province);
        if (provinceNormalized == null) {
            return List.of();
        }
        return repository.findDistinctDistrictsByProvinceNormalized(provinceNormalized);
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

    // ── AFAD yardımcı metodlar ───────────────────────────────────────────────

    private String buildAfadExternalId(AfadEventDto dto) {
        if (dto.getEventId() != null && !dto.getEventId().isBlank()) {
            return dto.getEventId();
        }
        if (dto.getDate() != null && dto.getLatitude() != null
                && dto.getLongitude() != null && dto.getMagnitude() != null) {
            // Locale.ROOT ZORUNLU: JVM default locale'i ondalık ayıracı olarak virgül
            // kullanıyorsa (örn. tr-TR), bu ID sunucu yeniden başlatıldığında/farklı bir
            // ortamda farklı üretilir ve dedup sessizce bozulur (aynı fiziksel kayıt tekrar
            // eklenir). Bu, eventId'nin null olduğu nadir fallback yolunda kritik.
            return String.format(java.util.Locale.ROOT, "%s_%.4f_%.4f_%.1f",
                    dto.getDate(), dto.getLatitude(), dto.getLongitude(), dto.getMagnitude());
        }
        return null;
    }

    private EarthquakeEvent toEntityFromAfad(AfadEventDto dto, String externalId) {
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
                location = String.format(java.util.Locale.ROOT, "%.4f, %.4f", dto.getLatitude(), dto.getLongitude());
            }
        }

        EarthquakeLocationResolver.Resolution loc = locationResolver.fromStructuredFields(
                dto.getProvince(), dto.getDistrict());

        return EarthquakeEvent.builder()
                .externalId(externalId)
                .eventTime(eventTime)
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .depth(dto.getDepth())
                .magnitude(dto.getMagnitude())
                .magnitudeType(dto.getType())
                .location(location)
                .province(loc.province())
                .provinceNormalized(loc.provinceNormalized())
                .district(loc.district())
                .districtNormalized(loc.districtNormalized())
                .source(EarthquakeSource.AFAD)
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

    // ── Kandilli yardımcı metodlar ────────────────────────────────────────────

    /**
     * Kandilli sayfası kalıcı bir eventId vermez — deterministik sentetik ID üretilir.
     * Aynı satır tekrar sync edildiğinde AYNI ID'yi üretmesi kritik (dedup'ın çalışması
     * için); bu yüzden ham (parse edilmiş, sabit hassasiyetli) alanlardan türetilir.
     */
    private String buildKandilliExternalId(KandilliEventDto dto, double magnitude) {
        // Locale.ROOT ZORUNLU — bkz. buildAfadExternalId'deki not. Kandilli'nin native ID'si
        // olmadığından TÜM Kandilli kayıtları bu formatı kullanır (AFAD'daki nadir fallback'in
        // aksine burası her zaman devrede), yani bu bug burada çok daha kritik olurdu.
        return String.format(java.util.Locale.ROOT, "KANDILLI_%s_%s_%.4f_%.4f_%.1f",
                dto.date(), dto.time(), dto.latitude(), dto.longitude(), magnitude);
    }

    private EarthquakeEvent toEntityFromKandilli(KandilliEventDto dto, String externalId, double magnitude) {
        OffsetDateTime eventTime = LocalDateTime.of(dto.date(), dto.time()).atZone(ISTANBUL).toOffsetDateTime();

        String rawPayload;
        try {
            rawPayload = objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            rawPayload = null;
        }

        EarthquakeLocationResolver.Resolution loc = locationResolver.fromFreeText(dto.rawLocation());
        String location = dto.rawLocation() != null && !dto.rawLocation().isBlank()
                ? dto.rawLocation()
                : String.format(java.util.Locale.ROOT, "%.4f, %.4f", dto.latitude(), dto.longitude());

        return EarthquakeEvent.builder()
                .externalId(externalId)
                .eventTime(eventTime)
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .depth(dto.depth())
                .magnitude(magnitude)
                .magnitudeType(dto.resolvedMagnitudeType())
                .location(location)
                .province(loc.province())
                .provinceNormalized(loc.provinceNormalized())
                .district(loc.district())
                .districtNormalized(loc.districtNormalized())
                .source(EarthquakeSource.KANDILLI)
                .riskLevel(calculateRiskLevel(magnitude))
                .rawPayload(rawPayload)
                .build();
    }

    // ── Ortak yardımcı metodlar ───────────────────────────────────────────────

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
                .magnitudeType(entity.getMagnitudeType())
                .location(entity.getLocation())
                .province(entity.getProvince())
                .district(entity.getDistrict())
                .source(entity.getSource())
                .riskLevel(entity.getRiskLevel())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
