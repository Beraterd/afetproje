package com.afet.koordinasyon.service;

import com.afet.koordinasyon.config.Building3dProperties;
import com.afet.koordinasyon.domain.entity.Building;
import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.enums.BuildingSource;
import com.afet.koordinasyon.dto.response.BuildingSearchResultResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.BuildingRepository;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock private BuildingRepository buildingRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;
    @Mock private DamageAssessmentRepository damageAssessmentRepository;
    @Mock private BuildingDamageStatusService damageStatusService;

    private Building3dProperties building3dProperties;
    private BuildingService service;

    private District pendik;
    private District otherDistrict;
    private Neighborhood kurtkoy;

    @BeforeEach
    void setUp() {
        building3dProperties = new Building3dProperties();
        building3dProperties.setEnabledDistricts(List.of("Pendik"));

        service = new BuildingService(buildingRepository, districtRepository, neighborhoodRepository,
                damageAssessmentRepository, damageStatusService, building3dProperties);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");

        pendik = District.builder().id(UUID.randomUUID()).name("Pendik").build();
        otherDistrict = District.builder().id(UUID.randomUUID()).name("Kadıköy").build();
        kurtkoy = Neighborhood.builder().id(UUID.randomUUID()).name("Kurtköy").district(pendik).build();
    }

    private Building building(District d, Neighborhood n, String street) {
        return Building.builder()
                .id(UUID.randomUUID())
                .source(BuildingSource.OPENSTREETMAP)
                .externalId("way/" + UUID.randomUUID())
                .district(d)
                .neighborhood(n)
                .streetName(street)
                .buildingNumber("1")
                .displayAddress(street + " 1, " + n.getName() + " Mah., " + d.getName() + "/İstanbul")
                .latitude(BigDecimal.valueOf(40.9))
                .longitude(BigDecimal.valueOf(29.3))
                .geometry("{\"type\":\"Polygon\",\"coordinates\":[[[29.3,40.9],[29.31,40.9],[29.31,40.91],[29.3,40.9]]]}")
                .estimatedHeightM(BigDecimal.valueOf(9))
                .heightSource("levels_x3")
                .bboxMinLat(BigDecimal.valueOf(40.9)).bboxMaxLat(BigDecimal.valueOf(40.91))
                .bboxMinLon(BigDecimal.valueOf(29.3)).bboxMaxLon(BigDecimal.valueOf(29.31))
                .build();
    }

    // ── bbox / district gating ──────────────────────────────────────────────

    @Test
    @DisplayName("Aktif olmayan ilçe için bina isteği reddedilir")
    void listBuildings_disabledDistrict_throws() {
        when(districtRepository.findById(otherDistrict.getId())).thenReturn(java.util.Optional.of(otherDistrict));

        assertThatThrownBy(() -> service.listBuildings(otherDistrict.getId(), null, 40.0, 41.0, 29.0, 30.0, 17))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Zoom eşiğinin altında sunucu tarafında boş FeatureCollection döner (defense-in-depth)")
    void listBuildings_belowMinZoom_returnsEmpty() {
        when(districtRepository.findById(pendik.getId())).thenReturn(java.util.Optional.of(pendik));

        Map<String, Object> result = service.listBuildings(pendik.getId(), null, 40.0, 41.0, 29.0, 30.0, 10);

        assertThat((List<?>) result.get("features")).isEmpty();
    }

    @Test
    @DisplayName("bbox eksikse (zoom eşiği geçildiyse) BusinessRuleException")
    void listBuildings_missingBbox_throws() {
        when(districtRepository.findById(pendik.getId())).thenReturn(java.util.Optional.of(pendik));

        assertThatThrownBy(() -> service.listBuildings(pendik.getId(), null, null, null, null, null, 17))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("bbox içindeki binalar döner, damage status batch olarak eklenir")
    void listBuildings_returnsFeaturesWithDamageStatus() {
        Building b1 = building(pendik, kurtkoy, "Hatboyu Sokak");
        when(districtRepository.findById(pendik.getId())).thenReturn(java.util.Optional.of(pendik));
        when(buildingRepository.findByDistrictAndBboxOverlap(eq(pendik.getId()), eq((UUID) null),
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(b1)));
        when(damageStatusService.summarizeForBuildings(any()))
                .thenReturn(Map.of(b1.getId(), new BuildingDamageStatusService.BuildingDamageSummary(
                        com.afet.koordinasyon.domain.enums.DamageLevel.HEAVY, 2, 1)));

        Map<String, Object> result = service.listBuildings(pendik.getId(), null, 40.0, 41.0, 29.0, 30.0, 17);

        List<?> features = (List<?>) result.get("features");
        assertThat(features).hasSize(1);
    }

    @Test
    @DisplayName("neighborhoodId verilirse repository'ye geçirilir (mahalle filtreleme)")
    void listBuildings_neighborhoodFilterPassedThrough() {
        when(neighborhoodRepository.findById(kurtkoy.getId())).thenReturn(java.util.Optional.of(kurtkoy));
        when(buildingRepository.findByDistrictAndBboxOverlap(eq(pendik.getId()), eq(kurtkoy.getId()),
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(damageStatusService.summarizeForBuildings(any())).thenReturn(Map.of());

        service.listBuildings(null, kurtkoy.getId(), 40.0, 41.0, 29.0, 30.0, 17);

        org.mockito.Mockito.verify(buildingRepository).findByDistrictAndBboxOverlap(
                eq(pendik.getId()), eq(kurtkoy.getId()), any(), any(), any(), any(), any());
    }

    // ── getById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Olmayan buildingId 404 (ResourceNotFoundException)")
    void getById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(buildingRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Bina detayı doğru alanlarla döner")
    void getById_returnsDetail() {
        Building b = building(pendik, kurtkoy, "Hatboyu Sokak");
        when(buildingRepository.findById(b.getId())).thenReturn(java.util.Optional.of(b));
        when(damageStatusService.summarizeForBuildings(List.of(b.getId())))
                .thenReturn(Map.of(b.getId(), new BuildingDamageStatusService.BuildingDamageSummary(null, 0, 0)));
        when(damageAssessmentRepository.findFirstByBuildingIdOrderByCreatedAtDesc(b.getId()))
                .thenReturn(java.util.Optional.empty());

        var detail = service.getById(b.getId());

        assertThat(detail.getId()).isEqualTo(b.getId());
        assertThat(detail.getDisplayAddress()).isEqualTo(b.getDisplayAddress());
        assertThat(detail.getDamageAssessmentCount()).isZero();
        assertThat(detail.getLatestAssessment()).isNull();
        assertThat(detail.isEstimatedHeight()).isTrue();
    }

    // ── search ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("2 karakterlik arama reddedilir (min 3 karakter)")
    void search_tooShort_throws() {
        assertThatThrownBy(() -> service.search("ab", pendik.getId(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Aktif olmayan ilçede arama reddedilir")
    void search_disabledDistrict_throws() {
        when(districtRepository.findById(otherDistrict.getId())).thenReturn(java.util.Optional.of(otherDistrict));

        assertThatThrownBy(() -> service.search("Hatboyu", otherDistrict.getId(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Geçerli arama Pendik sonuçlarını döner, başka ilçe repository'ye hiç sorulmaz")
    void search_returnsResults_scopedToDistrict() {
        Building b = building(pendik, kurtkoy, "Hatboyu Sokak");
        when(districtRepository.findById(pendik.getId())).thenReturn(java.util.Optional.of(pendik));
        when(buildingRepository.searchByDistrict(eq(pendik.getId()), eq("hatboyu"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(b)));

        List<BuildingSearchResultResponse> results = service.search("Hatboyu", pendik.getId(), null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDisplayAddress()).isEqualTo(b.getDisplayAddress());
        org.mockito.Mockito.verify(buildingRepository, org.mockito.Mockito.never())
                .searchByDistrict(eq(otherDistrict.getId()), any(), any());
    }

    @Test
    @DisplayName("limit MAX_SEARCH_LIMIT'i aşamaz")
    void search_limitIsCapped() {
        when(districtRepository.findById(pendik.getId())).thenReturn(java.util.Optional.of(pendik));
        when(buildingRepository.searchByDistrict(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.search("Hatboyu", pendik.getId(), 999);

        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(buildingRepository).searchByDistrict(eq(pendik.getId()), eq("hatboyu"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isLessThanOrEqualTo(20);
    }

    @Test
    @DisplayName("Türkçe katlama + sokak/sk kısaltması eşdeğerliği: arama sorgusu normalize edilerek repository'ye geçirilir")
    void search_normalizesTurkishAndStreetAbbreviations() {
        when(districtRepository.findById(pendik.getId())).thenReturn(java.util.Optional.of(pendik));
        when(buildingRepository.searchByDistrict(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.search("Selvi Boylum Sk", pendik.getId(), null);

        org.mockito.Mockito.verify(buildingRepository).searchByDistrict(eq(pendik.getId()), eq("selvi boylum"), any(Pageable.class));
    }

    @Test
    @DisplayName("Normalize sonrası 3 karakterin altına düşen sorgu (yalnızca kısaltma) reddedilir")
    void search_normalizedTooShort_throws() {
        assertThatThrownBy(() -> service.search("Sok", pendik.getId(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── config ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getConfig, Building3dProperties'ten aynen döner")
    void getConfig_reflectsProperties() {
        when(buildingRepository.count()).thenReturn(42L);

        var config = service.getConfig();
        assertThat(config.getEnabledDistricts()).containsExactly("Pendik");
        assertThat(config.getBuildingMinZoom()).isEqualTo(building3dProperties.getBuildingMinZoom());
        assertThat(config.getTotalBuildingCount()).isEqualTo(42L);
    }
}
