package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.enums.BuildingSource;
import com.afet.koordinasyon.repository.BuildingRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gerçek Postgres'e karşı: bir ilçenin (Kartal — mevcut migration'larda zaten var, gerçek
 * mahalleleriyle) idempotent import'unu doğrular (item 45: count before -> reimport -> count
 * after). Kullanılan externalId'ler ("it-test-kartal/...") gerçek OSM way ID'leriyle asla
 * çakışmayacak sabit bir önekle işaretlenmiştir ve testin sonunda temizlenir.
 */
@SpringBootTest
class BuildingImportServiceIT {

    private static final String FIXTURE = "data/buildings/kartal_it_fixture.ndjson";
    private static final String[] TEST_EXTERNAL_IDS = {"it-test-kartal/1", "it-test-kartal/2"};

    @Autowired
    private BuildingImportService buildingImportService;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @AfterEach
    void cleanUp() {
        for (String externalId : TEST_EXTERNAL_IDS) {
            buildingRepository.findBySourceAndExternalId(BuildingSource.OPENSTREETMAP, externalId)
                    .ifPresent(buildingRepository::delete);
        }
    }

    @Test
    void reimport_doesNotDuplicate_andUpdatesExisting() throws IOException {
        District kartal = districtRepository.findByName("Kartal").orElseThrow();
        long countBefore = buildingRepository.countByDistrictId(kartal.getId());

        BuildingImportService.ImportResult first = buildingImportService.importFromClasspath(FIXTURE, "Kartal");
        assertThat(first.imported()).isEqualTo(2);
        assertThat(first.updated()).isZero();

        long countAfterFirst = buildingRepository.countByDistrictId(kartal.getId());
        assertThat(countAfterFirst).isEqualTo(countBefore + 2);

        BuildingImportService.ImportResult second = buildingImportService.importFromClasspath(FIXTURE, "Kartal");
        assertThat(second.imported()).isZero();
        assertThat(second.updated()).isEqualTo(2);

        long countAfterSecond = buildingRepository.countByDistrictId(kartal.getId());
        assertThat(countAfterSecond).isEqualTo(countAfterFirst);
    }

    @Test
    void aggregateAndCoverageQueries_reflectRealImport() throws IOException {
        District kartal = districtRepository.findByName("Kartal").orElseThrow();
        buildingImportService.importFromClasspath(FIXTURE, "Kartal");

        var districtStats = buildingRepository.aggregateByDistrict().stream()
                .filter(s -> s.getDistrictId().equals(kartal.getId()))
                .findFirst().orElseThrow();
        assertThat(districtStats.getBuildingCount()).isGreaterThanOrEqualTo(2);
        assertThat(districtStats.getSource()).isEqualTo(BuildingSource.OPENSTREETMAP);
        assertThat(districtStats.getLastImportAt()).isNotNull();

        var neighborhoodCounts = buildingRepository.countByNeighborhoodForDistrict(kartal.getId());
        long orhantepeCount = neighborhoodCounts.stream()
                .mapToLong(BuildingRepository.NeighborhoodBuildingCount::getBuildingCount)
                .sum();
        assertThat(orhantepeCount).isGreaterThanOrEqualTo(2);
    }
}
