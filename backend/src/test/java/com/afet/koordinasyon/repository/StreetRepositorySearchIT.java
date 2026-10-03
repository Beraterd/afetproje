package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.Street;
import com.afet.koordinasyon.util.TurkishTextNormalizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gerçek Postgres'e karşı: StreetRepository.search'ün native SQL ranking'ini ve mahalle
 * izolasyonunu doğrular (item 47.1-47.3, 47.6-47.8, 47.11, 47.13-47.14). Kartal/Atalar +
 * Kartal/Cevizli (gerçek migration'larda zaten var) kullanılır — "it-test-" öneki taşıyan
 * geçici satırlar test sonunda silinir.
 */
@SpringBootTest
class StreetRepositorySearchIT {

    private static final String PREFIX = "IT Test ";

    @Autowired
    private StreetRepository streetRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private NeighborhoodRepository neighborhoodRepository;

    private District kartal;
    private Neighborhood atalar;
    private Neighborhood cevizli;

    @BeforeEach
    void setUp() {
        kartal = districtRepository.findByName("Kartal").orElseThrow();
        List<Neighborhood> nbs = neighborhoodRepository.findByDistrictIdOrderByNameAsc(kartal.getId());
        atalar = nbs.stream().filter(n -> n.getName().equals("Atalar")).findFirst().orElseThrow();
        cevizli = nbs.stream().filter(n -> n.getName().equals("Cevizli")).findFirst().orElseThrow();
    }

    @AfterEach
    void cleanUp() {
        streetRepository.findAll().stream()
                .filter(s -> s.getName().contains(PREFIX.trim()))
                .forEach(streetRepository::delete);
    }

    private Street street(Neighborhood n, String name) {
        return streetRepository.save(Street.builder()
                .source("OPENSTREETMAP")
                .externalId("way/it-test-" + UUID.randomUUID())
                .district(kartal)
                .neighborhood(n)
                .name(name)
                .normalizedName(TurkishTextNormalizer.normalize(name))
                .roadType("residential")
                .geometry("{\"type\":\"MultiLineString\",\"coordinates\":[[[29.18,40.9],[29.181,40.901]]]}")
                .bboxMinLat(BigDecimal.valueOf(40.9)).bboxMaxLat(BigDecimal.valueOf(40.901))
                .bboxMinLon(BigDecimal.valueOf(29.18)).bboxMaxLon(BigDecimal.valueOf(29.181))
                .centerLat(BigDecimal.valueOf(40.9005)).centerLon(BigDecimal.valueOf(29.1805))
                .build());
    }

    private List<Street> search(Neighborhood n, String rawQuery) {
        return streetRepository.search(kartal.getId(), n.getId(), TurkishTextNormalizer.normalize(rawQuery), PageRequest.of(0, 10));
    }

    @Test
    void search_isScopedToSelectedNeighborhood_sameNameInAnotherNeighborhoodIsIsolated() {
        street(atalar, PREFIX + "Selvi Sokak");
        street(cevizli, PREFIX + "Selvi Sokak");

        List<Street> inAtalar = search(atalar, PREFIX + "Selvi");

        assertThat(inAtalar).hasSize(1);
        assertThat(inAtalar.get(0).getNeighborhood().getId()).isEqualTo(atalar.getId());
    }

    @Test
    void search_exactMatchRanksBeforePrefixAndContains() {
        // Sorgu her zaman PREFIX+"Selvi" (== "IT Test Selvi") olduğundan, contains-eşleşmesi
        // için bu TAM DİZİYİ bir yerinde bitişik olarak içermeleri gerekir — üçüncü satır bunu
        // "Xyz " önekiyle sağlar (bkz. rank 2: kelime-başı eşleşmesi, "xyz" ile "it" arasında boşluk).
        street(atalar, PREFIX + "Selvi");
        street(atalar, PREFIX + "Selvi Boylum Sokak");
        street(atalar, "Xyz " + PREFIX + "Selvi Yani Sokak");

        List<Street> results = search(atalar, PREFIX + "Selvi");

        assertThat(results).hasSize(3);
        // Tam eşleşme ("Selvi") her zaman ilk sırada.
        assertThat(results.get(0).getName()).isEqualTo(PREFIX + "Selvi");
        // normalized_name'in BAŞINDA sorgu olan (rank 1) bir KELİMENİN başında olan (rank 2)
        // eşleşmeden önce gelmeli.
        int prefixIdx = indexOfName(results, PREFIX + "Selvi Boylum Sokak");
        int wordPrefixIdx = indexOfName(results, "Xyz " + PREFIX + "Selvi Yani Sokak");
        assertThat(prefixIdx).isLessThan(wordPrefixIdx);
    }

    @Test
    void search_wordPrefixRanksBeforePlainContains() {
        // "boylum" normalized_name'in BAŞINDA değil ama bir KELİMENİN başında -> rank 2
        street(atalar, PREFIX + "Selvi Boylum Sokak");
        // "boylum" burada bir kelimenin ortasında geçiyor -> rank 3 (contains-only)
        street(atalar, PREFIX + "Abcboylumcba Sokak");

        List<Street> results = search(atalar, "boylum");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo(PREFIX + "Selvi Boylum Sokak");
        assertThat(results.get(1).getName()).isEqualTo(PREFIX + "Abcboylumcba Sokak");
    }

    @Test
    void search_deterministicOrdering_shorterNameFirstOnTie() {
        street(atalar, PREFIX + "Gul Sokak");
        street(atalar, PREFIX + "Gul Bahce Sokak");

        List<Street> results = search(atalar, PREFIX + "Gul");

        assertThat(results.get(0).getName()).isEqualTo(PREFIX + "Gul Sokak");
        assertThat(results.get(1).getName()).isEqualTo(PREFIX + "Gul Bahce Sokak");
    }

    @Test
    void search_unknownQuery_returnsEmpty() {
        street(atalar, PREFIX + "Selvi Sokak");

        List<Street> results = search(atalar, PREFIX + "zzzzznomatch");

        assertThat(results).isEmpty();
    }

    @Test
    void search_respectsLimit() {
        for (int i = 0; i < 5; i++) {
            street(atalar, PREFIX + "Limit Test Sokak " + i);
        }

        List<Street> results = streetRepository.search(kartal.getId(), atalar.getId(),
                TurkishTextNormalizer.normalize(PREFIX + "Limit"), PageRequest.of(0, 3));

        assertThat(results).hasSize(3);
    }

    private static int indexOfName(List<Street> streets, String name) {
        for (int i = 0; i < streets.size(); i++) {
            if (streets.get(i).getName().equals(name)) return i;
        }
        return -1;
    }
}
