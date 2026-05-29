package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.DistrictCoordinationCenter;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.NeighborhoodCoordinationCenter;
import com.afet.koordinasyon.domain.enums.EventStatus;
import com.afet.koordinasyon.domain.enums.ResourceRequestStatus;
import com.afet.koordinasyon.dto.response.DistrictCoordinatorMapResponse;
import com.afet.koordinasyon.dto.response.MapDistrictResponse;
import com.afet.koordinasyon.dto.response.MapNeighborhoodResponse;
import com.afet.koordinasyon.dto.response.NeighborhoodCoordinatorMapResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.repository.DistrictCoordinationCenterRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.EventRepository;
import com.afet.koordinasyon.repository.NeighborhoodCoordinationCenterRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.ResourceRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MapService {

    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final EventRepository eventRepository;
    private final ResourceRequestRepository resourceRequestRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final RiskCalculationService riskCalculationService;
    private final ObjectMapper objectMapper;
    private final DistrictCoordinationCenterRepository districtCenterRepo;
    private final NeighborhoodCoordinationCenterRepository neighborhoodCenterRepo;

    @Transactional(readOnly = true)
    public List<MapDistrictResponse> getDistrictsForMap() {
        return districtRepository.findByActiveTrue().stream()
                .map(this::toMapDistrictResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MapNeighborhoodResponse> getNeighborhoodsForMap(UUID districtId) {
        districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));
        return neighborhoodRepository.findByDistrictId(districtId).stream()
                .map(this::toMapNeighborhoodResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DistrictCoordinatorMapResponse> getDistrictCoordinatorsForMap() {
        return districtCenterRepo.findAll().stream()
                .map(c -> {
                    District d = c.getDistrict();
                    long openEvents = eventRepository.countByDistrictIdAndStatusIn(d.getId(), ACTIVE_EVENT_STATUSES);
                    long openResources = resourceRequestRepository
                            .findByDistrictIdAndStatus(d.getId(), ResourceRequestStatus.OPEN).size();
                    long totalDamage = damageAssessmentRepository.countByDistrictId(d.getId());
                    return DistrictCoordinatorMapResponse.builder()
                            .districtId(d.getId())
                            .districtName(d.getName())
                            .latitude(c.getLatitude().doubleValue())
                            .longitude(c.getLongitude().doubleValue())
                            .address(c.getAddress())
                            .locationVerified(c.isLocationVerified())
                            .openEventCount(openEvents)
                            .openResourceRequestCount(openResources)
                            .totalDamageCount(totalDamage)
                            .centerUpdatedAt(c.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NeighborhoodCoordinatorMapResponse> getNeighborhoodCoordinatorsForMap(UUID districtId) {
        List<NeighborhoodCoordinationCenter> centers = districtId != null
                ? neighborhoodCenterRepo.findByNeighborhoodDistrictId(districtId)
                : neighborhoodCenterRepo.findAll();
        return centers.stream()
                .map(c -> {
                    Neighborhood n = c.getNeighborhood();
                    District d = n.getDistrict();
                    long openEvents = eventRepository.countByNeighborhoodIdAndStatusIn(n.getId(), ACTIVE_EVENT_STATUSES);
                    long openResources = resourceRequestRepository
                            .findByNeighborhoodIdAndStatus(n.getId(), ResourceRequestStatus.OPEN).size();
                    long totalDamage = damageAssessmentRepository.countByNeighborhoodId(n.getId());
                    return NeighborhoodCoordinatorMapResponse.builder()
                            .neighborhoodId(n.getId())
                            .neighborhoodName(n.getName())
                            .districtId(d != null ? d.getId() : null)
                            .districtName(d != null ? d.getName() : null)
                            .latitude(c.getLatitude().doubleValue())
                            .longitude(c.getLongitude().doubleValue())
                            .address(c.getAddress())
                            .locationVerified(c.isLocationVerified())
                            .openEventCount(openEvents)
                            .openResourceRequestCount(openResources)
                            .totalDamageCount(totalDamage)
                            .centerUpdatedAt(c.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    private static final List<EventStatus> ACTIVE_EVENT_STATUSES =
            List.of(EventStatus.OPEN, EventStatus.IN_PROGRESS);

    private MapDistrictResponse toMapDistrictResponse(District d) {
        long activeEvents = eventRepository.countByDistrictIdAndStatusIn(d.getId(), ACTIVE_EVENT_STATUSES);
        long openResources = resourceRequestRepository.findByDistrictIdAndStatus(d.getId(), ResourceRequestStatus.OPEN).size();
        long damageCount = damageAssessmentRepository.countByDistrictId(d.getId());
        BigDecimal riskScore = d.getRiskScore() != null ? d.getRiskScore() : BigDecimal.ZERO;
        return MapDistrictResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .riskScore(riskScore)
                .riskColor(riskCalculationService.getRiskColor(riskScore))
                .riskLevel(riskCalculationService.getRiskLevel(riskScore))
                .openEventCount(activeEvents)
                .openResourceRequestCount(openResources)
                .damageCount(damageCount)
                .polygon(parseGeojson(d.getGeojsonPolygon(), d.getName()))
                .build();
    }

    private MapNeighborhoodResponse toMapNeighborhoodResponse(Neighborhood n) {
        long activeEvents = eventRepository.countByNeighborhoodIdAndStatusIn(n.getId(), ACTIVE_EVENT_STATUSES);
        long openResources = resourceRequestRepository.findByNeighborhoodIdAndStatus(n.getId(), ResourceRequestStatus.OPEN).size();
        long damageCount = damageAssessmentRepository.countByNeighborhoodId(n.getId());
        BigDecimal riskScore = n.getRiskScore() != null ? n.getRiskScore() : BigDecimal.ZERO;
        return MapNeighborhoodResponse.builder()
                .id(n.getId())
                .name(n.getName())
                .riskScore(riskScore)
                .riskColor(riskCalculationService.getRiskColor(riskScore))
                .riskLevel(riskCalculationService.getRiskLevel(riskScore))
                .openEventCount(activeEvents)
                .openResourceRequestCount(openResources)
                .damageCount(damageCount)
                .polygon(parseGeojson(n.getGeojsonPolygon(), n.getName()))
                .build();
    }

    private JsonNode parseGeojson(String geojson, String name) {
        if (geojson == null || geojson.isBlank()) {
            log.debug("No GeoJSON polygon stored for '{}'", name);
            return null;
        }
        try {
            return objectMapper.readTree(geojson);
        } catch (Exception e) {
            log.warn("Failed to parse GeoJSON polygon for '{}': {}", name, e.getMessage());
            return null;
        }
    }
}
