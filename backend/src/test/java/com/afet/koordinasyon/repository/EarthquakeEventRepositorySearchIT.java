package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.enums.EarthquakeRiskLevel;
import com.afet.koordinasyon.domain.enums.EarthquakeSource;
import com.afet.koordinasyon.earthquake.LocationNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Item 20 (testler #6-9, #11-13) — canlı Postgres'e karşı province/district/source/
 * magnitude/tarih filtre kombinasyonlarının GERÇEK sonuç kümesini doğrular ("çalışır" değil,
 * "doğru sonucu döner"). Her test kendi verisini oluşturur, @Transactional ile rollback
 * edilir — kalıcı veri bırakmaz.
 * <p>
 * Hotfix regresyon testleri (search_sourceOnly_noDateFilter_doesNotThrow /
 * search_withDateRangeFilter_appliesCorrectly): GET /api/earthquakes?source=KANDILLI (tarih
 * filtresi olmadan) 500 ile patlıyordu çünkü eski JPQL "(:startDate IS NULL OR ...)" deseni
 * PostgreSQL'in timestamptz parametresinin tipini çıkaramamasına yol açıyordu
 * ("could not determine data type of parameter $9"). Artık Criteria API (EarthquakeEventSpecification)
 * kullanılıyor — null tarih filtreleri için hiçbir parametre bind edilmiyor.
 */
@SpringBootTest
@Transactional
class EarthquakeEventRepositorySearchIT {

    @Autowired
    private EarthquakeEventRepository repository;

    private static final PageRequest PAGE = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "eventTime"));

    private EarthquakeEvent save(EarthquakeSource source, String province, String district,
                                  double magnitude, OffsetDateTime time) {
        EarthquakeEvent e = EarthquakeEvent.builder()
                .externalId(source + "_" + UUID.randomUUID())
                .eventTime(time)
                .latitude(38.0).longitude(36.0)
                .magnitude(magnitude)
                .province(province)
                .provinceNormalized(LocationNormalizer.normalize(province))
                .district(district)
                .districtNormalized(LocationNormalizer.normalize(district))
                .source(source)
                .riskLevel(EarthquakeRiskLevel.LOW)
                .build();
        return repository.save(e);
    }

    private Page<EarthquakeEvent> search(EarthquakeSource source, String province, String district,
                                          Double minMagnitude, OffsetDateTime startDate, OffsetDateTime endDate) {
        Specification<EarthquakeEvent> spec = EarthquakeEventSpecification.withFilters(
                source, province, district, minMagnitude, startDate, endDate);
        return repository.findAll(spec, PAGE);
    }

    @Test
    void search_byProvince_returnsOnlyMatchingProvince() {
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, OffsetDateTime.now());
        save(EarthquakeSource.AFAD, "Ankara", "Çankaya", 3.0, OffsetDateTime.now());

        Page<EarthquakeEvent> result = search(null, LocationNormalizer.normalize("İstanbul"), null, null, null, null);

        assertThat(result.getContent()).allMatch(e -> "İstanbul".equals(e.getProvince()));
        assertThat(result.getContent()).extracting(EarthquakeEvent::getProvince).contains("İstanbul");
        assertThat(result.getContent()).noneMatch(e -> "Ankara".equals(e.getProvince()));
    }

    @Test
    void search_byDistrict_returnsOnlyMatchingDistrict() {
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, OffsetDateTime.now());
        save(EarthquakeSource.AFAD, "İstanbul", "Kadıköy", 3.0, OffsetDateTime.now());

        Page<EarthquakeEvent> result = search(null, null, LocationNormalizer.normalize("Kartal"), null, null, null);

        assertThat(result.getContent()).allMatch(e -> "Kartal".equals(e.getDistrict()));
    }

    @Test
    void search_bySource_returnsOnlyMatchingSource() {
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, OffsetDateTime.now());
        save(EarthquakeSource.KANDILLI, "İstanbul", "Kartal", 3.0, OffsetDateTime.now());

        Page<EarthquakeEvent> result = search(EarthquakeSource.KANDILLI, null, null, null, null, null);

        assertThat(result.getContent()).allMatch(e -> e.getSource() == EarthquakeSource.KANDILLI);
    }

    @Test
    void search_provinceAndDistrictTogether_bothApply() {
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, OffsetDateTime.now());
        save(EarthquakeSource.AFAD, "İstanbul", "Kadıköy", 3.0, OffsetDateTime.now());
        save(EarthquakeSource.AFAD, "Ankara", "Kartal", 3.0, OffsetDateTime.now()); // farklı il, aynı isimli ilçe yok ama senaryo netliği için

        Page<EarthquakeEvent> result = search(
                null, LocationNormalizer.normalize("İstanbul"), LocationNormalizer.normalize("Kartal"),
                null, null, null);

        assertThat(result.getContent()).allMatch(e -> "İstanbul".equals(e.getProvince()) && "Kartal".equals(e.getDistrict()));
    }

    @Test
    void search_districtBelongingToOtherProvince_doesNotLeakIntoWrongProvinceFilter() {
        // "Merkez" gibi aynı isimli ilçe farklı illerde olabilir — il+ilçe birlikte
        // filtrelendiğinde YANLIŞ ilin kaydı asla dönmemeli.
        save(EarthquakeSource.AFAD, "İstanbul", "Merkez", 3.0, OffsetDateTime.now());
        save(EarthquakeSource.AFAD, "Ankara", "Merkez", 3.0, OffsetDateTime.now());

        Page<EarthquakeEvent> result = search(
                null, LocationNormalizer.normalize("Ankara"), LocationNormalizer.normalize("Merkez"),
                null, null, null);

        assertThat(result.getContent()).allMatch(e -> "Ankara".equals(e.getProvince()));
        assertThat(result.getContent()).noneMatch(e -> "İstanbul".equals(e.getProvince()));
    }

    @Test
    void search_magnitudeAndProvinceTogether_bothApply() {
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 2.0, OffsetDateTime.now());
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 4.5, OffsetDateTime.now());

        Page<EarthquakeEvent> result = search(null, LocationNormalizer.normalize("İstanbul"), null, 4.0, null, null);

        assertThat(result.getContent()).allMatch(e -> e.getMagnitude() >= 4.0);
    }

    @Test
    void search_turkishCharacterProvince_matchesRegardlessOfSourceCasing() {
        // Kandilli ASCII/büyük harf verir ("KAHRAMANMARAS"), ama provinceNormalized aynı
        // anahtara denk gelir — kullanıcı dropdown'dan "Kahramanmaraş" seçer.
        save(EarthquakeSource.KANDILLI, "Kahramanmaraş", "Afsin", 3.0, OffsetDateTime.now());

        Page<EarthquakeEvent> result = search(null, LocationNormalizer.normalize("Kahramanmaraş"), null, null, null, null);

        assertThat(result.getContent()).isNotEmpty();
    }

    // ── Hotfix regresyon: tarih filtresi null iken 500 vermemeli ────────────────

    @Test
    void search_sourceOnlyNoDateFilter_doesNotThrow_regressionForOriginal500() {
        save(EarthquakeSource.KANDILLI, "İstanbul", "Kartal", 3.0, OffsetDateTime.now());

        // Orijinal hata: GET /api/earthquakes?page=1&size=20&source=KANDILLI — startDate/endDate
        // verilmeden yalnızca source ile filtrelemek "could not determine data type of
        // parameter $9" hatasıyla patlıyordu.
        assertThatCode(() -> search(EarthquakeSource.KANDILLI, null, null, null, null, null))
                .doesNotThrowAnyException();

        Page<EarthquakeEvent> result = search(EarthquakeSource.KANDILLI, null, null, null, null, null);
        assertThat(result.getContent()).allMatch(e -> e.getSource() == EarthquakeSource.KANDILLI);
    }

    @Test
    void search_withDateRangeFilter_appliesCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();
        save(EarthquakeSource.KANDILLI, "İstanbul", "Kartal", 3.0, now.minusDays(10));
        save(EarthquakeSource.KANDILLI, "İstanbul", "Kartal", 3.0, now);

        Page<EarthquakeEvent> result = search(
                EarthquakeSource.KANDILLI, null, null, null, now.minusDays(1), now.plusDays(1));

        assertThat(result.getContent()).allMatch(e -> !e.getEventTime().isBefore(now.minusDays(1))
                && !e.getEventTime().isAfter(now.plusDays(1)));
        assertThat(result.getContent()).extracting(EarthquakeEvent::getEventTime)
                .noneMatch(t -> t.isBefore(now.minusDays(2)));
    }

    @Test
    void search_startDateOnly_withoutEndDate_doesNotThrow() {
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, OffsetDateTime.now());

        assertThatCode(() -> search(null, null, null, null, OffsetDateTime.now().minusDays(1), null))
                .doesNotThrowAnyException();
    }

    @Test
    void search_endDateOnly_withoutStartDate_doesNotThrow() {
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, OffsetDateTime.now());

        assertThatCode(() -> search(null, null, null, null, null, OffsetDateTime.now().plusDays(1)))
                .doesNotThrowAnyException();
    }

    // ── Hotfix regresyon #2: "Tüm Kaynaklar" AFAD+Kandilli'yi eventTime'a göre BİRLEŞTİRMELİ ──

    /**
     * Kullanıcının bildirdiği tam senaryo: source=null ("Tüm Kaynaklar") iken sonuç, AFAD'ın
     * ham batch sırasına göre değil GLOBAL eventTime DESC'e göre sıralanmalı — Kandilli
     * kayıtları (afadOrderIndex her zaman NULL) listenin sonuna düşmemeli.
     * <p>
     * Bu test, ortamda daha önce oluşmuş başka gerçek kayıtlarla karışmaması için 2031 yılına
     * ait, gerçek AFAD/Kandilli verisiyle çakışması imkansız senkron zamanlar kullanır ve
     * sonucu bu dar tarih aralığına filtreler — filtrelenen alan yalnızca izolasyon amaçlıdır,
     * asıl doğrulanan davranış source=null'ın AFAD+Kandilli'yi birleştirmesidir.
     */
    @Test
    void search_noSourceFilter_mergesAfadAndKandilli_globalEventTimeOrder_notAfadFirst() {
        OffsetDateTime base = OffsetDateTime.parse("2031-03-01T00:00:00Z");
        OffsetDateTime kandilli1 = base.plusMinutes(35); // en yeni — KANDILLI
        OffsetDateTime kandilli2 = base.plusMinutes(34);
        OffsetDateTime afad1 = base.plusMinutes(21);     // AFAD, ikisinin arasında
        OffsetDateTime kandilli3 = base.plusMinutes(17); // en eski — KANDILLI

        // Kasıtlı olarak KANDILLI önce, AFAD sonra kaydedilir — findAllOrdered'daki eski bug
        // "hangi sırayla save edildiği" değil "afadOrderIndex NULL mı" ile ilgiliydi.
        save(EarthquakeSource.KANDILLI, "İstanbul", "Kartal", 3.0, kandilli1);
        save(EarthquakeSource.KANDILLI, "İstanbul", "Kartal", 3.0, kandilli2);
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, afad1);
        save(EarthquakeSource.KANDILLI, "İstanbul", "Kartal", 3.0, kandilli3);

        Page<EarthquakeEvent> result = search(
                null, null, null, null, base, base.plusHours(1)); // source=null -> Tüm Kaynaklar

        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent()).extracting(EarthquakeEvent::getEventTime)
                .containsExactly(kandilli1, kandilli2, afad1, kandilli3);
        assertThat(result.getContent()).extracting(EarthquakeEvent::getSource)
                .containsExactly(EarthquakeSource.KANDILLI, EarthquakeSource.KANDILLI,
                        EarthquakeSource.AFAD, EarthquakeSource.KANDILLI);
        // Her iki kaynak da mevcut — biri diğerini listeden dışlamıyor/arkaya itmiyor.
        assertThat(result.getContent()).extracting(EarthquakeEvent::getSource)
                .contains(EarthquakeSource.AFAD, EarthquakeSource.KANDILLI);
    }

    /** Pagination'ın bu birleşik global sıralama üzerinden uygulandığını doğrular (item 4). */
    @Test
    void search_noSourceFilter_paginationAppliesOverMergedGlobalOrder() {
        OffsetDateTime base = OffsetDateTime.parse("2031-04-01T00:00:00Z");
        save(EarthquakeSource.KANDILLI, "İstanbul", "Kartal", 3.0, base.plusMinutes(4));
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, base.plusMinutes(3));
        save(EarthquakeSource.KANDILLI, "İstanbul", "Kartal", 3.0, base.plusMinutes(2));
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, base.plusMinutes(1));

        Specification<EarthquakeEvent> spec = EarthquakeEventSpecification.withFilters(
                null, null, null, null, base, base.plusHours(1));

        Page<EarthquakeEvent> page0 = repository.findAll(spec, PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "eventTime")));
        Page<EarthquakeEvent> page1 = repository.findAll(spec, PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "eventTime")));

        assertThat(page0.getTotalElements()).isEqualTo(4);
        assertThat(page0.getContent()).extracting(EarthquakeEvent::getEventTime)
                .containsExactly(base.plusMinutes(4), base.plusMinutes(3));
        assertThat(page1.getContent()).extracting(EarthquakeEvent::getEventTime)
                .containsExactly(base.plusMinutes(2), base.plusMinutes(1));
    }

    @Test
    void existsBySourceAndExternalId_sameIdDifferentSource_areIndependent() {
        String sharedId = "shared-external-id-" + UUID.randomUUID();
        EarthquakeEvent afad = EarthquakeEvent.builder()
                .externalId(sharedId).eventTime(OffsetDateTime.now())
                .latitude(38.0).longitude(36.0).magnitude(3.0)
                .source(EarthquakeSource.AFAD).riskLevel(EarthquakeRiskLevel.LOW).build();
        repository.save(afad);

        assertThat(repository.existsBySourceAndExternalId(EarthquakeSource.AFAD, sharedId)).isTrue();
        assertThat(repository.existsBySourceAndExternalId(EarthquakeSource.KANDILLI, sharedId)).isFalse();
    }

    @Test
    void findDistinctDistrictsByProvinceNormalized_onlyReturnsRealObservedDistricts() {
        save(EarthquakeSource.AFAD, "İstanbul", "Kartal", 3.0, OffsetDateTime.now());
        save(EarthquakeSource.AFAD, "İstanbul", "Kadıköy", 3.0, OffsetDateTime.now());
        save(EarthquakeSource.AFAD, "Ankara", "Çankaya", 3.0, OffsetDateTime.now());

        List<String> districts = repository.findDistinctDistrictsByProvinceNormalized(
                LocationNormalizer.normalize("İstanbul"));

        assertThat(districts).contains("Kartal", "Kadıköy").doesNotContain("Çankaya");
    }
}
