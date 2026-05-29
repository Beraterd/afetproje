package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.DistrictCoordinationCenter;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.NeighborhoodCoordinationCenter;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.dto.request.UpsertCoordinationCenterRequest;
import com.afet.koordinasyon.dto.response.CoordinationCenterResponse;
import com.afet.koordinasyon.dto.response.CoordinationCenterStatusResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DistrictCoordinationCenterRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodCoordinationCenterRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoordinationCenterService {

    private final DistrictCoordinationCenterRepository districtCenterRepo;
    private final NeighborhoodCoordinationCenterRepository neighborhoodCenterRepo;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ── District Centers ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CoordinationCenterResponse getDistrictCenter(UUID districtId, UserPrincipal principal) {
        District district = findDistrict(districtId);
        checkDistrictAccess(districtId, principal);

        return districtCenterRepo.findByDistrictId(districtId)
                .map(c -> toDistrictResponse(c, district))
                .orElseThrow(() -> new ResourceNotFoundException("DistrictCoordinationCenter", "districtId", districtId));
    }

    @Transactional
    public CoordinationCenterResponse upsertDistrictCenter(UUID districtId,
                                                            UpsertCoordinationCenterRequest req,
                                                            UserPrincipal principal) {
        District district = findDistrict(districtId);
        checkDistrictAccess(districtId, principal);
        validateDistrictPolygon(req.getLatitude(), req.getLongitude(), district);

        User editor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        DistrictCoordinationCenter center = districtCenterRepo.findByDistrictId(districtId)
                .orElseGet(() -> DistrictCoordinationCenter.builder().district(district).build());

        center.setAddress(req.getAddress());
        center.setStreetName(req.getStreetName());
        center.setBuildingNo(req.getBuildingNo());
        center.setLatitude(BigDecimal.valueOf(req.getLatitude()));
        center.setLongitude(BigDecimal.valueOf(req.getLongitude()));
        center.setLocationVerified(Boolean.TRUE.equals(req.getLocationVerified()));
        center.setUpdatedBy(editor);

        return toDistrictResponse(districtCenterRepo.save(center), district);
    }

    // ── Neighborhood Centers ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CoordinationCenterResponse getNeighborhoodCenter(UUID neighborhoodId, UserPrincipal principal) {
        Neighborhood neighborhood = findNeighborhood(neighborhoodId);
        checkNeighborhoodAccess(neighborhood, principal);

        return neighborhoodCenterRepo.findByNeighborhoodId(neighborhoodId)
                .map(c -> toNeighborhoodResponse(c, neighborhood))
                .orElseThrow(() -> new ResourceNotFoundException("NeighborhoodCoordinationCenter", "neighborhoodId", neighborhoodId));
    }

    @Transactional
    public CoordinationCenterResponse upsertNeighborhoodCenter(UUID neighborhoodId,
                                                                UpsertCoordinationCenterRequest req,
                                                                UserPrincipal principal) {
        Neighborhood neighborhood = findNeighborhood(neighborhoodId);
        checkNeighborhoodAccess(neighborhood, principal);
        validateNeighborhoodPolygon(req.getLatitude(), req.getLongitude(), neighborhood);

        User editor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        NeighborhoodCoordinationCenter center = neighborhoodCenterRepo.findByNeighborhoodId(neighborhoodId)
                .orElseGet(() -> NeighborhoodCoordinationCenter.builder().neighborhood(neighborhood).build());

        center.setAddress(req.getAddress());
        center.setStreetName(req.getStreetName());
        center.setBuildingNo(req.getBuildingNo());
        center.setLatitude(BigDecimal.valueOf(req.getLatitude()));
        center.setLongitude(BigDecimal.valueOf(req.getLongitude()));
        center.setLocationVerified(Boolean.TRUE.equals(req.getLocationVerified()));
        center.setUpdatedBy(editor);

        return toNeighborhoodResponse(neighborhoodCenterRepo.save(center), neighborhood);
    }

    // ── My Status ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CoordinationCenterStatusResponse getMyStatus(UserPrincipal principal) {
        String role = principal.getRole().name();
        CoordinationCenterStatusResponse.CoordinationCenterStatusResponseBuilder builder =
                CoordinationCenterStatusResponse.builder().userRole(role);

        if ("DISTRICT_COORDINATOR".equals(role)) {
            // Primary: FK lookup (district.coordinator_id = user.id)
            // Fallback: user's own district_id (handles manual DB assignments)
            District district = districtRepository.findByCoordinatorId(principal.getId()).orElse(null);
            if (district == null && principal.getDistrictId() != null) {
                district = districtRepository.findById(principal.getDistrictId()).orElse(null);
            }
            if (district != null) {
                UUID districtId = district.getId();
                boolean defined = districtCenterRepo.existsByDistrictId(districtId);
                builder.assignedDistrictId(districtId)
                        .assignedDistrictName(district.getName())
                        .districtCenterDefined(defined)
                        .mustSetCenter(!defined);
            } else {
                builder.mustSetCenter(false);
            }
        } else if ("NEIGHBORHOOD_COORDINATOR".equals(role)) {
            // Primary: FK lookup (neighborhood.coordinator_id = user.id)
            // Fallback: user's own neighborhood_id (handles manual DB assignments)
            Neighborhood neighborhood = neighborhoodRepository.findByCoordinatorId(principal.getId()).orElse(null);
            if (neighborhood == null && principal.getNeighborhoodId() != null) {
                neighborhood = neighborhoodRepository.findById(principal.getNeighborhoodId()).orElse(null);
            }
            if (neighborhood != null) {
                UUID neighborhoodId = neighborhood.getId();
                boolean defined = neighborhoodCenterRepo.existsByNeighborhoodId(neighborhoodId);
                District nbhDistrict = neighborhood.getDistrict();
                builder.assignedNeighborhoodId(neighborhoodId)
                        .assignedNeighborhoodName(neighborhood.getName())
                        .assignedNeighborhoodDistrictId(nbhDistrict != null ? nbhDistrict.getId() : null)
                        .assignedNeighborhoodDistrictName(nbhDistrict != null ? nbhDistrict.getName() : null)
                        .neighborhoodCenterDefined(defined)
                        .mustSetCenter(!defined);
            } else {
                builder.mustSetCenter(false);
            }
        } else {
            builder.mustSetCenter(false);
        }

        return builder.build();
    }

    // ── Access checks ─────────────────────────────────────────────────────────

    private void checkDistrictAccess(UUID districtId, UserPrincipal principal) {
        String role = principal.getRole().name();
        if ("ADMIN".equals(role)) return;
        if ("DISTRICT_COORDINATOR".equals(role)) {
            // Primary: FK lookup; fallback: user's own district_id (manual DB assignment)
            UUID assignedDistrictId = districtRepository.findByCoordinatorId(principal.getId())
                    .map(District::getId)
                    .orElse(principal.getDistrictId());
            if (districtId.equals(assignedDistrictId)) return;
        }
        throw new BusinessRuleException("Bu ilçenin koordinatörlük merkezi bilgilerine erişim yetkiniz bulunmamaktadır.");
    }

    private void checkNeighborhoodAccess(Neighborhood neighborhood, UserPrincipal principal) {
        String role = principal.getRole().name();
        if ("ADMIN".equals(role)) return;
        if ("NEIGHBORHOOD_COORDINATOR".equals(role)) {
            // Primary: FK lookup; fallback: user's own neighborhood_id (manual DB assignment)
            UUID assignedNeighborhoodId = neighborhoodRepository.findByCoordinatorId(principal.getId())
                    .map(Neighborhood::getId)
                    .orElse(principal.getNeighborhoodId());
            if (neighborhood.getId().equals(assignedNeighborhoodId)) return;
        }
        if ("DISTRICT_COORDINATOR".equals(role)) {
            // DC can manage any neighborhood center within their assigned district
            // Primary: FK lookup; fallback: user's own district_id
            UUID assignedDistrictId = districtRepository.findByCoordinatorId(principal.getId())
                    .map(District::getId)
                    .orElse(principal.getDistrictId());
            District nbhDistrict = neighborhood.getDistrict();
            if (assignedDistrictId != null && nbhDistrict != null && assignedDistrictId.equals(nbhDistrict.getId())) return;
        }
        throw new BusinessRuleException("Bu mahallenin koordinatörlük merkezi bilgilerine erişim yetkiniz bulunmamaktadır.");
    }

    // ── Polygon validation ────────────────────────────────────────────────────

    private void validateDistrictPolygon(double lat, double lng, District district) {
        if (district.getGeojsonPolygon() == null || district.getGeojsonPolygon().isBlank()) return;
        if (!isPointInGeojson(lat, lng, district.getGeojsonPolygon())) {
            throw new BusinessRuleException(
                    "Seçtiğiniz konum ilçe sınırları dışında kalıyor. Lütfen ilçe sınırları içinden bir konum seçin.");
        }
    }

    private void validateNeighborhoodPolygon(double lat, double lng, Neighborhood neighborhood) {
        if (neighborhood.getGeojsonPolygon() == null || neighborhood.getGeojsonPolygon().isBlank()) return;
        if (!isPointInGeojson(lat, lng, neighborhood.getGeojsonPolygon())) {
            throw new BusinessRuleException(
                    "Seçtiğiniz konum mahalle sınırları dışında kalıyor. Lütfen mahalle sınırları içinden bir konum seçin.");
        }
    }

    /**
     * Ray-casting point-in-polygon for GeoJSON Polygon or MultiPolygon.
     * GeoJSON uses [longitude, latitude] coordinate order.
     */
    private boolean isPointInGeojson(double lat, double lng, String geojsonPolygon) {
        try {
            JsonNode node = objectMapper.readTree(geojsonPolygon);
            String type = node.path("type").asText();
            JsonNode coordinates = node.path("coordinates");

            if ("Polygon".equals(type)) {
                return isPointInRing(lat, lng, coordinates.get(0));
            } else if ("MultiPolygon".equals(type)) {
                for (JsonNode polygon : coordinates) {
                    if (isPointInRing(lat, lng, polygon.get(0))) return true;
                }
                return false;
            }
            // Unknown type → no restriction
            return true;
        } catch (Exception e) {
            log.warn("GeoJSON polygon validation failed, allowing point: {}", e.getMessage());
            return true; // Fail open — don't block if parsing fails
        }
    }

    private boolean isPointInRing(double lat, double lng, JsonNode ring) {
        if (ring == null || ring.isEmpty()) return false;
        int n = ring.size();
        boolean inside = false;
        int j = n - 1;
        for (int i = 0; i < n; i++) {
            // GeoJSON: [longitude, latitude]
            double xi = ring.get(i).get(0).asDouble(); // longitude
            double yi = ring.get(i).get(1).asDouble(); // latitude
            double xj = ring.get(j).get(0).asDouble();
            double yj = ring.get(j).get(1).asDouble();
            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
            j = i;
        }
        return inside;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private District findDistrict(UUID districtId) {
        return districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));
    }

    private Neighborhood findNeighborhood(UUID neighborhoodId) {
        return neighborhoodRepository.findById(neighborhoodId)
                .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", neighborhoodId));
    }

    private CoordinationCenterResponse toDistrictResponse(DistrictCoordinationCenter c, District district) {
        return CoordinationCenterResponse.builder()
                .id(c.getId())
                .districtId(district.getId())
                .districtName(district.getName())
                .address(c.getAddress())
                .streetName(c.getStreetName())
                .buildingNo(c.getBuildingNo())
                .latitude(c.getLatitude().doubleValue())
                .longitude(c.getLongitude().doubleValue())
                .locationVerified(c.isLocationVerified())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .updatedBy(c.getUpdatedBy() != null
                        ? c.getUpdatedBy().getFirstName() + " " + c.getUpdatedBy().getLastName()
                        : null)
                .build();
    }

    private CoordinationCenterResponse toNeighborhoodResponse(NeighborhoodCoordinationCenter c, Neighborhood nb) {
        District d = nb.getDistrict();
        return CoordinationCenterResponse.builder()
                .id(c.getId())
                .neighborhoodId(nb.getId())
                .neighborhoodName(nb.getName())
                .districtId(d != null ? d.getId() : null)
                .districtName(d != null ? d.getName() : null)
                .address(c.getAddress())
                .streetName(c.getStreetName())
                .buildingNo(c.getBuildingNo())
                .latitude(c.getLatitude().doubleValue())
                .longitude(c.getLongitude().doubleValue())
                .locationVerified(c.isLocationVerified())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .updatedBy(c.getUpdatedBy() != null
                        ? c.getUpdatedBy().getFirstName() + " " + c.getUpdatedBy().getLastName()
                        : null)
                .build();
    }
}
