package com.afet.koordinasyon.service;

import com.afet.koordinasyon.config.Building3dProperties;
import com.afet.koordinasyon.domain.entity.Building;
import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.enums.BuildingSource;
import com.afet.koordinasyon.repository.BuildingRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * fixture: src/test/resources/data/buildings/testdistrict_fixture.ndjson — 5 satır:
 * way/1 (osm_height), way/2 (levels_x3), way/3 (fallback), way/4 (bilinmeyen mahalle,
 * skippedNoNeighborhood), way/5 (geometry eksik, skippedInvalidRecord).
 */
@ExtendWith(MockitoExtension.class)
class BuildingImportServiceTest {

    private static final String FIXTURE = "data/buildings/testdistrict_fixture.ndjson";

    @Mock private BuildingRepository buildingRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;
    @Mock private EntityManager entityManager;

    private Building3dProperties building3dProperties;
    private BuildingImportService service;

    private District testDistrict;
    private Neighborhood merkez;

    @BeforeEach
    void setUp() {
        building3dProperties = new Building3dProperties();
        building3dProperties.setLevelHeightM(3.0);
        building3dProperties.setFallbackHeightM(6.0);

        service = new BuildingImportService(buildingRepository, districtRepository, neighborhoodRepository, building3dProperties);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        testDistrict = District.builder().id(UUID.randomUUID()).name("TestDistrict").build();
        merkez = Neighborhood.builder().id(UUID.randomUUID()).name("Merkez").district(testDistrict).build();

        org.mockito.Mockito.lenient().when(districtRepository.findByName("TestDistrict")).thenReturn(Optional.of(testDistrict));
        org.mockito.Mockito.lenient().when(neighborhoodRepository.findByDistrictId(testDistrict.getId())).thenReturn(List.of(merkez));
        org.mockito.Mockito.lenient().when(buildingRepository.save(any(Building.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Bilinmeyen ilçe adıyla import başlatılamaz")
    void importFromClasspath_unknownDistrict_throws() {
        when(districtRepository.findByName("Nowhere")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importFromClasspath(FIXTURE, "Nowhere"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("İlçenin hiç mahallesi yoksa import başlatılamaz")
    void importFromClasspath_noNeighborhoods_throws() {
        when(neighborhoodRepository.findByDistrictId(testDistrict.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.importFromClasspath(FIXTURE, "TestDistrict"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Geçerli kayıtlar import edilir, mahalle bulunamayan/geçersiz kayıtlar sayılır")
    void importFromClasspath_countsMatchFixture() throws IOException {
        when(buildingRepository.findBySourceAndExternalId(eq(BuildingSource.OPENSTREETMAP), any()))
                .thenReturn(Optional.empty());

        BuildingImportService.ImportResult result = service.importFromClasspath(FIXTURE, "TestDistrict");

        assertThat(result.imported()).isEqualTo(3);
        assertThat(result.updated()).isZero();
        assertThat(result.skippedNoNeighborhood()).isEqualTo(1);
        assertThat(result.skippedInvalidRecord()).isEqualTo(1);
        assertThat(result.unmatchedNeighborhoods()).containsExactly("BilinmeyenMahalle");
    }

    @Test
    @DisplayName("Yükseklik zinciri: osm_height > levels_x3 > fallback, hiçbiri 0 değil")
    void importFromClasspath_heightFallbackChain() throws IOException {
        when(buildingRepository.findBySourceAndExternalId(eq(BuildingSource.OPENSTREETMAP), any()))
                .thenReturn(Optional.empty());
        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);

        service.importFromClasspath(FIXTURE, "TestDistrict");

        org.mockito.Mockito.verify(buildingRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        var byExternalId = captor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(Building::getExternalId, b -> b));

        Building way1 = byExternalId.get("way/1");
        assertThat(way1.getHeightSource()).isEqualTo("osm_height");
        assertThat(way1.getEstimatedHeightM()).isEqualByComparingTo(BigDecimal.valueOf(12.5));

        Building way2 = byExternalId.get("way/2");
        assertThat(way2.getHeightSource()).isEqualTo("levels_x3");
        assertThat(way2.getEstimatedHeightM()).isEqualByComparingTo(BigDecimal.valueOf(12.0));

        Building way3 = byExternalId.get("way/3");
        assertThat(way3.getHeightSource()).isEqualTo("fallback");
        assertThat(way3.getEstimatedHeightM()).isEqualByComparingTo(BigDecimal.valueOf(6.0));

        assertThat(byExternalId.values()).allSatisfy(b ->
                assertThat(b.getEstimatedHeightM()).isGreaterThan(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("İkinci import aynı kayıtları duplike etmez — var olanlar update edilir (idempotency)")
    void importFromClasspath_reimport_updatesInsteadOfDuplicating() throws IOException {
        when(buildingRepository.findBySourceAndExternalId(eq(BuildingSource.OPENSTREETMAP), any()))
                .thenReturn(Optional.empty());
        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);

        BuildingImportService.ImportResult first = service.importFromClasspath(FIXTURE, "TestDistrict");
        assertThat(first.imported()).isEqualTo(3);

        org.mockito.Mockito.verify(buildingRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        var savedByExternalId = captor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(Building::getExternalId, b -> b));

        org.mockito.Mockito.reset(buildingRepository);
        when(buildingRepository.save(any(Building.class))).thenAnswer(inv -> inv.getArgument(0));
        when(buildingRepository.findBySourceAndExternalId(BuildingSource.OPENSTREETMAP, "way/1"))
                .thenReturn(Optional.of(savedByExternalId.get("way/1")));
        when(buildingRepository.findBySourceAndExternalId(BuildingSource.OPENSTREETMAP, "way/2"))
                .thenReturn(Optional.of(savedByExternalId.get("way/2")));
        when(buildingRepository.findBySourceAndExternalId(BuildingSource.OPENSTREETMAP, "way/3"))
                .thenReturn(Optional.of(savedByExternalId.get("way/3")));

        BuildingImportService.ImportResult second = service.importFromClasspath(FIXTURE, "TestDistrict");

        assertThat(second.imported()).isZero();
        assertThat(second.updated()).isEqualTo(3);
    }
}
