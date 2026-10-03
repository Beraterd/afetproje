package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.StreetRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gerçek Postgres'e karşı: bir ilçenin (Kartal — BuildingImportServiceIT ile aynı ilçe/mahalle,
 * gerçek migration'larda zaten var) idempotent sokak import'unu doğrular (item 45/47.14).
 * Kullanılan externalId'ler ("way/it-test-street-*") gerçek OSM way ID'leriyle asla çakışmaz.
 */
@SpringBootTest
class StreetImportServiceIT {

    private static final String FIXTURE = "data/streets/kartal_it_fixture.ndjson";
    private static final String[] TEST_NAMES = {"IT Test Sokak Bir", "IT Test Sokak İki"};

    @Autowired
    private StreetImportService streetImportService;

    @Autowired
    private StreetRepository streetRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private NeighborhoodRepository neighborhoodRepository;

    @AfterEach
    void cleanUp() {
        District kartal = districtRepository.findByName("Kartal").orElseThrow();
        List<Neighborhood> orhantepeCandidates = neighborhoodRepository.findByDistrictIdOrderByNameAsc(kartal.getId())
                .stream().filter(n -> n.getName().equalsIgnoreCase("Orhantepe")).toList();
        if (orhantepeCandidates.isEmpty()) return;
        Neighborhood orhantepe = orhantepeCandidates.get(0);
        for (String name : TEST_NAMES) {
            streetRepository.findByDistrictIdAndNeighborhoodIdAndName(kartal.getId(), orhantepe.getId(), name)
                    .ifPresent(streetRepository::delete);
        }
    }

    @Test
    void reimport_doesNotDuplicate_andUpdatesExisting() throws IOException {
        District kartal = districtRepository.findByName("Kartal").orElseThrow();
        long countBefore = streetRepository.countByDistrictId(kartal.getId());

        StreetImportService.ImportResult first = streetImportService.importFromClasspath(FIXTURE, "Kartal");
        assertThat(first.imported()).isEqualTo(2);
        assertThat(first.updated()).isZero();
        assertThat(first.skippedNoNeighborhood()).isZero();

        long countAfterFirst = streetRepository.countByDistrictId(kartal.getId());
        assertThat(countAfterFirst).isEqualTo(countBefore + 2);

        StreetImportService.ImportResult second = streetImportService.importFromClasspath(FIXTURE, "Kartal");
        assertThat(second.imported()).isZero();
        assertThat(second.updated()).isEqualTo(2);

        long countAfterSecond = streetRepository.countByDistrictId(kartal.getId());
        assertThat(countAfterSecond).isEqualTo(countAfterFirst);
    }
}
