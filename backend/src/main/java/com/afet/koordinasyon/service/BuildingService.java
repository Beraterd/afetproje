package com.afet.koordinasyon.service;

import com.afet.koordinasyon.config.Building3dProperties;
import com.afet.koordinasyon.domain.entity.Building;
import com.afet.koordinasyon.domain.entity.DamageAssessment;
import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.enums.PhotoType;
import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.BuildingRepository;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.util.TurkishTextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildingService {

    /** Tek bir bbox isteğinde döndürülen maksimum bina sayısı — bir ilçenin tamamını tek seferde asla dökme. */
    private static final int MAX_BBOX_RESULTS = 2000;
    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int MAX_SEARCH_LIMIT = 20;
    private static final int MIN_SEARCH_QUERY_LENGTH = 3;

    @Value("${app.base-url}")
    private String baseUrl;

    private final BuildingRepository buildingRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final BuildingDamageStatusService damageStatusService;
    private final Building3dProperties building3dProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Building3dConfigResponse getConfig() {
        return Building3dConfigResponse.builder()
                .enabledDistricts(building3dProperties.getEnabledDistricts())
                .buildingMinZoom(building3dProperties.getBuildingMinZoom())
                .extrusionMinZoom(building3dProperties.getExtrusionMinZoom())
                .totalBuildingCount(buildingRepository.count())
                .build();
    }

    /** GET /api/buildings — bbox-scoped GeoJSON FeatureCollection. */
    @Transactional(readOnly = true)
    public Map<String, Object> listBuildings(UUID districtId, UUID neighborhoodId,
                                               Double minLat, Double maxLat, Double minLon, Double maxLon,
                                               Integer zoom) {
        District district = resolveEnabledDistrict(districtId, neighborhoodId);

        // Sunucu tarafı zoom eşiği — frontend bu eşiğin altında hiç istek atmamalı, ama savunma amaçlı
        // burada da uygulanır (client'a güvenme).
        if (zoom != null && zoom < building3dProperties.getBuildingMinZoom()) {
            return emptyFeatureCollection();
        }
        if (minLat == null || maxLat == null || minLon == null || maxLon == null) {
            throw new BusinessRuleException("bbox parametresi zorunludur (minLon,minLat,maxLon,maxLat)");
        }

        List<Building> buildings = buildingRepository.findByDistrictAndBboxOverlap(
                district.getId(), neighborhoodId,
                BigDecimal.valueOf(minLat), BigDecimal.valueOf(maxLat),
                BigDecimal.valueOf(minLon), BigDecimal.valueOf(maxLon),
                PageRequest.of(0, MAX_BBOX_RESULTS)).getContent();

        Map<UUID, BuildingDamageStatusService.BuildingDamageSummary> statusByBuilding =
                damageStatusService.summarizeForBuildings(buildings.stream().map(Building::getId).toList());

        List<Map<String, Object>> features = buildings.stream()
                .map(b -> toFeature(b, statusByBuilding.get(b.getId())))
                .toList();

        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", features);
        return collection;
    }

    @Transactional(readOnly = true)
    public BuildingDetailResponse getById(UUID id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building", "id", id));

        var statusMap = damageStatusService.summarizeForBuildings(List.of(id));
        var summary = statusMap.get(id);

        DamageAssessment latest = damageAssessmentRepository.findFirstByBuildingIdOrderByCreatedAtDesc(id).orElse(null);
        BuildingLatestAssessmentSummary latestSummary = null;
        if (latest != null) {
            List<String> photoUrls = latest.getPhotos().stream()
                    .filter(p -> p.getPhotoType() == PhotoType.REPORTER_PHOTO || p.getPhotoType() == PhotoType.ASSIGNEE_FIELD_PHOTO)
                    .map(p -> baseUrl + "/api/damage-assessments/photos/" + p.getDownloadToken())
                    .toList();
            latestSummary = BuildingLatestAssessmentSummary.builder()
                    .id(latest.getId())
                    .damageLevel(latest.getDamageLevel().name())
                    .damageLevelLabel(latest.getDamageLevel().getLabel())
                    .verificationStatus(latest.getVerificationStatus().name())
                    .verificationStatusLabel(latest.getVerificationStatus().getLabel())
                    .createdAt(latest.getCreatedAt())
                    .photoUrls(photoUrls)
                    .build();
        }

        return BuildingDetailResponse.builder()
                .id(building.getId())
                .source(building.getSource().name())
                .districtId(building.getDistrict().getId())
                .districtName(building.getDistrict().getName())
                .neighborhoodId(building.getNeighborhood().getId())
                .neighborhoodName(building.getNeighborhood().getName())
                .streetName(building.getStreetName())
                .buildingNumber(building.getBuildingNumber())
                .displayAddress(building.getDisplayAddress())
                .latitude(building.getLatitude().doubleValue())
                .longitude(building.getLongitude().doubleValue())
                .geometry(parseGeometry(building.getGeometry()))
                .heightM(building.getHeightM() != null ? building.getHeightM().doubleValue() : null)
                .levels(building.getLevels())
                .estimatedHeightM(building.getEstimatedHeightM().doubleValue())
                .isEstimatedHeight(!"osm_height".equals(building.getHeightSource()))
                .heightSource(building.getHeightSource())
                .buildingType(building.getBuildingType())
                .name(building.getName())
                .damageStatus(summary != null && summary.damageStatus() != null ? summary.damageStatus().name() : null)
                .damageStatusLabel(summary != null && summary.damageStatus() != null ? summary.damageStatus().getLabel() : null)
                .damageAssessmentCount(summary != null ? summary.totalCount() : 0)
                .verifiedAssessmentCount(summary != null ? summary.verifiedCount() : 0)
                .latestAssessment(latestSummary)
                .build();
    }

    /** GET /api/buildings/search — yalnızca verilen ilçe (districtId) kapsamında, o ilçenin 3B
     *  bina katmanı aktifse (bkz. Building3dProperties.enabledDistricts) adres araması.
     *  Karşılaştırma Türkçe-katlanmış, sokak/cadde/mahalle kısaltmalarından arındırılmış bir metin
     *  üzerinden yapılır (bkz. {@link TurkishTextNormalizer}) — "Selvi Boylum Sk" ve
     *  "Selvi Boylum Sokak" aynı sonucu döner. */
    @Transactional(readOnly = true)
    public List<BuildingSearchResultResponse> search(String q, UUID districtId, Integer limit) {
        if (q == null || q.trim().length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new BusinessRuleException("Arama metni en az " + MIN_SEARCH_QUERY_LENGTH + " karakter olmalıdır");
        }
        String normalizedQ = TurkishTextNormalizer.normalize(q);
        if (normalizedQ.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new BusinessRuleException("Arama metni en az " + MIN_SEARCH_QUERY_LENGTH + " karakter olmalıdır");
        }
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));
        if (!building3dProperties.isDistrictEnabled(district.getName())) {
            throw new BusinessRuleException("Bu ilçe için bina araması aktif değil");
        }

        int effectiveLimit = Math.min(limit != null ? limit : DEFAULT_SEARCH_LIMIT, MAX_SEARCH_LIMIT);
        return buildingRepository.searchByDistrict(districtId, normalizedQ, PageRequest.of(0, effectiveLimit))
                .getContent().stream()
                .map(b -> BuildingSearchResultResponse.builder()
                        .id(b.getId())
                        .displayAddress(b.getDisplayAddress())
                        .streetName(b.getStreetName())
                        .buildingNumber(b.getBuildingNumber())
                        .neighborhoodName(b.getNeighborhood().getName())
                        .latitude(b.getLatitude().doubleValue())
                        .longitude(b.getLongitude().doubleValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ── yardımcılar ──────────────────────────────────────────────────────

    private District resolveEnabledDistrict(UUID districtId, UUID neighborhoodId) {
        District district;
        if (districtId != null) {
            district = districtRepository.findById(districtId)
                    .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));
        } else if (neighborhoodId != null) {
            Neighborhood neighborhood = neighborhoodRepository.findById(neighborhoodId)
                    .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", neighborhoodId));
            district = neighborhood.getDistrict();
        } else {
            throw new BusinessRuleException("districtId veya neighborhoodId belirtilmelidir");
        }
        if (!building3dProperties.isDistrictEnabled(district.getName())) {
            throw new BusinessRuleException("Bu ilçe için 3B bina katmanı aktif değil");
        }
        return district;
    }

    private Map<String, Object> toFeature(Building b, BuildingDamageStatusService.BuildingDamageSummary summary) {
        boolean isEstimated = !"osm_height".equals(b.getHeightSource());
        BuildingSummaryResponse props = BuildingSummaryResponse.builder()
                .id(b.getId())
                .source(b.getSource().name())
                .buildingType(b.getBuildingType())
                .name(b.getName())
                .streetName(b.getStreetName())
                .buildingNumber(b.getBuildingNumber())
                .heightM(b.getHeightM() != null ? b.getHeightM().doubleValue() : null)
                .levels(b.getLevels())
                .estimatedHeightM(b.getEstimatedHeightM().doubleValue())
                .isEstimatedHeight(isEstimated)
                .heightSource(b.getHeightSource())
                .damageStatus(summary != null && summary.damageStatus() != null ? summary.damageStatus().name() : null)
                .damageStatusLabel(summary != null && summary.damageStatus() != null ? summary.damageStatus().getLabel() : null)
                .damageAssessmentCount(summary != null ? summary.totalCount() : 0)
                .build();

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("id", b.getId().toString());
        feature.put("geometry", parseGeometry(b.getGeometry()));
        feature.put("properties", props);
        return feature;
    }

    private JsonNode parseGeometry(String geometryJson) {
        try {
            return objectMapper.readTree(geometryJson);
        } catch (Exception e) {
            throw new IllegalStateException("Bozuk bina geometrisi (DB tutarsızlığı)", e);
        }
    }

    private Map<String, Object> emptyFeatureCollection() {
        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", List.of());
        return collection;
    }
}
