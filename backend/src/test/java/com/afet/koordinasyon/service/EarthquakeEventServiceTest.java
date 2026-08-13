package com.afet.koordinasyon.service;

import com.afet.koordinasyon.client.AfadEarthquakeClient;
import com.afet.koordinasyon.client.KandilliEarthquakeClient;
import com.afet.koordinasyon.client.dto.AfadEventDto;
import com.afet.koordinasyon.client.dto.KandilliEventDto;
import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.enums.EarthquakeRiskLevel;
import com.afet.koordinasyon.domain.enums.EarthquakeSource;
import com.afet.koordinasyon.dto.response.EarthquakeSyncResponse;
import com.afet.koordinasyon.earthquake.EarthquakeLocationResolver;
import com.afet.koordinasyon.earthquake.LocationNormalizer;
import com.afet.koordinasyon.earthquake.PhysicalEarthquakeMatcher;
import com.afet.koordinasyon.repository.EarthquakeEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Item 20 — sync dedup, source doğruluğu, filtre delegasyonu ve cross-source bildirim
 * dedup davranışını gerçek DB olmadan (mock repository) doğrular.
 */
@ExtendWith(MockitoExtension.class)
class EarthquakeEventServiceTest {

    @Mock private EarthquakeEventRepository repository;
    @Mock private AfadEarthquakeClient afadClient;
    @Mock private KandilliEarthquakeClient kandilliClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    private EarthquakeEventService service;

    @BeforeEach
    void setUp() {
        LocationNormalizer normalizer = new LocationNormalizer();
        EarthquakeLocationResolver locationResolver = new EarthquakeLocationResolver(normalizer);
        PhysicalEarthquakeMatcher matcher = new PhysicalEarthquakeMatcher();
        service = new EarthquakeEventService(
                repository, afadClient, kandilliClient, locationResolver, matcher,
                new ObjectMapper(), eventPublisher);
    }

    private KandilliEventDto kandilliDto(LocalDate date, LocalTime time, double lat, double lon, Double ml) {
        return new KandilliEventDto(date, time, lat, lon, 5.0, null, ml, null, "SOGUCAK-AFSIN (KAHRAMANMARAS)");
    }

    // ── source doğruluğu ──────────────────────────────────────────────────────

    @Test
    void syncFromKandilli_newRecord_savedWithSourceKandilli() {
        LocalDate date = LocalDate.of(2026, 8, 8);
        LocalTime time = LocalTime.of(19, 40, 26);
        when(kandilliClient.fetchRecentEvents(24)).thenReturn(List.of(kandilliDto(date, time, 38.1440, 36.9340, 2.3)));
        when(repository.existsBySourceAndExternalId(eq(EarthquakeSource.KANDILLI), anyString())).thenReturn(false);
        when(repository.findBySourceNotAndNotificationSentTrueAndEventTimeBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(repository.save(any(EarthquakeEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        EarthquakeSyncResponse response = service.syncFromKandilli(24);

        ArgumentCaptor<EarthquakeEvent> captor = ArgumentCaptor.forClass(EarthquakeEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(EarthquakeSource.KANDILLI);
        assertThat(captor.getValue().getMagnitude()).isEqualTo(2.3);
        assertThat(captor.getValue().getMagnitudeType()).isEqualTo("ML");
        assertThat(captor.getValue().getProvince()).isEqualTo("Kahramanmaraş");
        assertThat(response.getSavedCount()).isEqualTo(1);
        assertThat(response.getSkippedDuplicateCount()).isEqualTo(0);
        verify(eventPublisher, times(1)).publishEvent(any(EarthquakeAlertCreatedEvent.class));
    }

    @Test
    void syncFromAfad_newRecord_sourceStaysAfad() {
        AfadEventDto dto = new AfadEventDto();
        dto.setEventId("725037");
        dto.setLocation("Meram (Konya)");
        dto.setLatitude(37.84367);
        dto.setLongitude(32.0885);
        dto.setDepth(6.98);
        dto.setType("ML");
        dto.setMagnitude(0.7);
        dto.setProvince("Konya");
        dto.setDistrict("Meram");
        dto.setDate("2026-08-08T00:48:41");

        when(afadClient.fetchRecentEvents(24)).thenReturn(List.of(dto));
        when(repository.existsBySourceAndExternalId(eq(EarthquakeSource.AFAD), eq("725037"))).thenReturn(false);
        when(repository.findBySourceNotAndNotificationSentTrueAndEventTimeBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(repository.save(any(EarthquakeEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findTopByOrderByEventTimeDescCreatedAtAsc()).thenReturn(java.util.Optional.empty());
        when(repository.findLatestByAfadOrder()).thenReturn(List.of());

        service.syncFromAfad(24);

        ArgumentCaptor<EarthquakeEvent> captor = ArgumentCaptor.forClass(EarthquakeEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(EarthquakeSource.AFAD);
        assertThat(captor.getValue().getMagnitudeType()).isEqualTo("ML");
        assertThat(captor.getValue().getProvince()).isEqualTo("Konya");
    }

    // ── provider içi dedup ────────────────────────────────────────────────────

    @Test
    void syncFromKandilli_sameExternalIdTwice_secondCallIsSkippedNotSaved() {
        LocalDate date = LocalDate.of(2026, 8, 8);
        LocalTime time = LocalTime.of(19, 40, 26);
        when(kandilliClient.fetchRecentEvents(24)).thenReturn(List.of(kandilliDto(date, time, 38.1440, 36.9340, 2.3)));
        when(repository.existsBySourceAndExternalId(eq(EarthquakeSource.KANDILLI), anyString())).thenReturn(true);

        EarthquakeSyncResponse response = service.syncFromKandilli(24);

        verify(repository, never()).save(any());
        assertThat(response.getSavedCount()).isEqualTo(0);
        assertThat(response.getSkippedDuplicateCount()).isEqualTo(1);
    }

    // ── cross-source bildirim dedup (item 17) ────────────────────────────────

    @Test
    void crossSourceDuplicate_notificationSuppressed_butRecordStillSaved() {
        LocalDate date = LocalDate.of(2026, 8, 8);
        LocalTime time = LocalTime.of(14, 0, 0);
        when(kandilliClient.fetchRecentEvents(24)).thenReturn(List.of(kandilliDto(date, time, 38.1440, 36.9340, 4.1)));
        when(repository.existsBySourceAndExternalId(eq(EarthquakeSource.KANDILLI), anyString())).thenReturn(false);
        when(repository.save(any(EarthquakeEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // AFAD zaten çok yakın zamanda/konumda/büyüklükte bir kayıt için bildirim göndermiş.
        EarthquakeEvent alreadyNotifiedAfadEvent = EarthquakeEvent.builder()
                .source(EarthquakeSource.AFAD)
                .eventTime(OffsetDateTime.parse("2026-08-08T14:00:00+03:00").plusSeconds(10))
                .latitude(38.1450).longitude(36.9350).magnitude(4.2)
                .notificationSent(true)
                .riskLevel(EarthquakeRiskLevel.MEDIUM)
                .externalId("afad-1").build();
        when(repository.findBySourceNotAndNotificationSentTrueAndEventTimeBetween(any(), any(), any()))
                .thenReturn(List.of(alreadyNotifiedAfadEvent));

        service.syncFromKandilli(24);

        verify(eventPublisher, never()).publishEvent(any());
        // save çağrılır: bir kez entity kaydı için, bir kez notificationSent=true güncellemesi için.
        verify(repository, times(2)).save(any(EarthquakeEvent.class));
    }

    // ── filtre delegasyonu ────────────────────────────────────────────────────

    /**
     * Hotfix regresyonu — "Tüm Kaynaklar" (source dahil hiçbir filtre verilmemiş) artık eski
     * AFAD-order-index öncelikli findAllOrdered() yerine, filtreli çağrılarla AYNI Specification
     * + global eventTime DESC yolunu kullanıyor. Eski davranışta Kandilli kayıtları
     * (afadOrderIndex her zaman NULL) gerçek saatinden bağımsız olarak TÜM AFAD kayıtlarının
     * arkasına düşüyordu.
     */
    @Test
    void listEarthquakes_noFilter_usesSpecificationPath_notLegacyFindAllOrdered() {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listEarthquakes(0, 20, new EarthquakeEventService.EarthquakeFilter(
                null, null, null, null, null, null));

        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("eventTime").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    /**
     * "Tüm Kaynaklar" görünümünün AFAD ve Kandilli kayıtlarını GERÇEKTEN eventTime'a göre iç
     * içe (interleaved) birleştirdiğini, tek bir kaynağa öncelik vermediğini doğrular — bu testte
     * repository (mock) zaten DB'nin ORDER BY eventTime DESC ile üreteceği sırayı simüle eder;
     * gerçek DB sıralamasının doğruluğu EarthquakeEventRepositorySearchIT'te ayrıca doğrulanır.
     */
    @Test
    void listEarthquakes_noFilter_mergesAfadAndKandilli_inEventTimeOrder_notSourceGrouped() {
        OffsetDateTime t1 = OffsetDateTime.parse("2026-08-09T00:35:00Z"); // KANDILLI — en yeni
        OffsetDateTime t2 = OffsetDateTime.parse("2026-08-09T00:34:40Z"); // KANDILLI
        OffsetDateTime t3 = OffsetDateTime.parse("2026-08-09T00:21:20Z"); // AFAD
        OffsetDateTime t4 = OffsetDateTime.parse("2026-08-09T00:17:01Z"); // KANDILLI

        EarthquakeEvent k1 = EarthquakeEvent.builder().id(java.util.UUID.randomUUID()).source(EarthquakeSource.KANDILLI)
                .eventTime(t1).latitude(1.0).longitude(1.0).magnitude(3.0).riskLevel(EarthquakeRiskLevel.LOW).externalId("k1").build();
        EarthquakeEvent k2 = EarthquakeEvent.builder().id(java.util.UUID.randomUUID()).source(EarthquakeSource.KANDILLI)
                .eventTime(t2).latitude(1.0).longitude(1.0).magnitude(3.0).riskLevel(EarthquakeRiskLevel.LOW).externalId("k2").build();
        EarthquakeEvent a1 = EarthquakeEvent.builder().id(java.util.UUID.randomUUID()).source(EarthquakeSource.AFAD)
                .eventTime(t3).latitude(1.0).longitude(1.0).magnitude(3.0).riskLevel(EarthquakeRiskLevel.LOW).externalId("a1").build();
        EarthquakeEvent k3 = EarthquakeEvent.builder().id(java.util.UUID.randomUUID()).source(EarthquakeSource.KANDILLI)
                .eventTime(t4).latitude(1.0).longitude(1.0).magnitude(3.0).riskLevel(EarthquakeRiskLevel.LOW).externalId("k3").build();

        // Gerçek Postgres'te ORDER BY eventTime DESC bu sırayı üretir — mock bunu simüle eder.
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(k1, k2, a1, k3)));

        var result = service.listEarthquakes(0, 20, new EarthquakeEventService.EarthquakeFilter(
                null, null, null, null, null, null));

        assertThat(result.getContent()).extracting(r -> r.getSource())
                .containsExactly(EarthquakeSource.KANDILLI, EarthquakeSource.KANDILLI, EarthquakeSource.AFAD, EarthquakeSource.KANDILLI);
        assertThat(result.getContent()).extracting(r -> r.getEventTime())
                .containsExactly(t1, t2, t3, t4);
        // Her iki source da listede var — biri diğerini "ele geçirmiyor".
        assertThat(result.getContent()).extracting(r -> r.getSource())
                .contains(EarthquakeSource.AFAD, EarthquakeSource.KANDILLI);
    }

    @Test
    void listEarthquakes_withProvinceFilter_normalizesBeforeQuerying() {
        Page<EarthquakeEvent> empty = new PageImpl<>(List.of());
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(empty);

        service.listEarthquakes(0, 20, new EarthquakeEventService.EarthquakeFilter(
                EarthquakeSource.KANDILLI, "istanbul", "Kartal", 3.0, null, null));

        // Filtreli çağrı artık Criteria API (JpaSpecificationExecutor.findAll) üzerinden gidiyor —
        // normalizasyonun gerçekten uygulandığı EarthquakeEventSpecificationTest'te ayrıca doğrulanır.
        // Burada yalnızca doğru sayfalama/sıralama ile findAll'ın çağrıldığı doğrulanır.
        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("eventTime").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }
}
