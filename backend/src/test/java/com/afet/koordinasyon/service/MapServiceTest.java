package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.repository.BuildingRepository;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.repository.DistrictCoordinationCenterRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.EventRepository;
import com.afet.koordinasyon.repository.NeighborhoodCoordinationCenterRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.ResourceRequestRepository;
import com.afet.koordinasyon.dto.response.MapNeighborhoodResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapServiceTest {

    @Mock private DistrictRepository districtRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;
    @Mock private EventRepository eventRepository;
    @Mock private ResourceRequestRepository resourceRequestRepository;
    @Mock private DamageAssessmentRepository damageAssessmentRepository;
    @Mock private BuildingRepository buildingRepository;
    @Mock private RiskCalculationService riskCalculationService;
    @Mock private ObjectMapper objectMapper;
    @Mock private DistrictCoordinationCenterRepository districtCenterRepo;
    @Mock private NeighborhoodCoordinationCenterRepository neighborhoodCenterRepo;

    @InjectMocks
    private MapService mapService;

    private District pendik;
    private Neighborhood kurtkoy;

    @BeforeEach
    void setUp() {
        pendik = District.builder().id(UUID.randomUUID()).name("Pendik").build();
        kurtkoy = Neighborhood.builder().id(UUID.randomUUID()).name("Kurtköy").district(pendik).build();
    }

    @Test
    void getNeighborhoodsForMap_populatesBuildingCountFromBuildingRepository() {
        when(districtRepository.findById(pendik.getId())).thenReturn(java.util.Optional.of(pendik));
        when(neighborhoodRepository.findByDistrictId(pendik.getId())).thenReturn(List.of(kurtkoy));
        when(buildingRepository.countByNeighborhoodId(kurtkoy.getId())).thenReturn(1842L);

        List<MapNeighborhoodResponse> result = mapService.getNeighborhoodsForMap(pendik.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBuildingCount()).isEqualTo(1842L);
    }

    @Test
    void getNeighborhoodsForMap_returnsZeroBuildingCountWhenNoneImported() {
        when(districtRepository.findById(pendik.getId())).thenReturn(java.util.Optional.of(pendik));
        when(neighborhoodRepository.findByDistrictId(pendik.getId())).thenReturn(List.of(kurtkoy));
        when(buildingRepository.countByNeighborhoodId(kurtkoy.getId())).thenReturn(0L);

        List<MapNeighborhoodResponse> result = mapService.getNeighborhoodsForMap(pendik.getId());

        assertThat(result.get(0).getBuildingCount()).isZero();
    }
}
