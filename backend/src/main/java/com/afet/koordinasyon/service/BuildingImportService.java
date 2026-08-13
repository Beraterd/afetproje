package com.afet.koordinasyon.service;

import com.afet.koordinasyon.config.Building3dProperties;
import com.afet.koordinasyon.domain.entity.Building;
import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.enums.BuildingSource;
import com.afet.koordinasyon.dto.response.BuildingImportStatusResponse;
import com.afet.koordinasyon.dto.response.NeighborhoodCoverageResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.BuildingRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.util.TurkishTextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * `backend/src/main/resources/data/buildings/{slug}.ndjson`'daki OSM tabanlı bina kayıtlarını
 * (bkz. scripts/fetch_district_buildings.py) buildings tablosuna idempotent şekilde import eder.
 * Her çağrı tek bir ilçeye yönelik dosya + district adı alır — bir ilçenin importu diğer
 * ilçelerin verisini etkilemez/bozmaz. Bu servis/API'ler district hardcode ETMEZ, bkz.
 * Building3dProperties.enabledDistricts (tek doğruluk kaynağı hangi ilçelerin aktif olduğu).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingImportService {

    /** Bu kadar kayıtta bir entityManager.flush()+clear() yapılır — Hibernate'in dirty-checking
     *  maliyeti persistence context boyutuyla büyür; periyodik clear olmadan 20k+ kayıtlık bir
     *  import loop'u pratikte O(n²)'ye yaklaşıp dakikalarca sürebilir (gözlemlendi). District/
     *  Neighborhood referansları clear() sonrası detached kalır ama sorun değil — Building'in
     *  bunlarla ManyToOne ilişkisi cascade taşımaz, Hibernate FK yazımı için yalnızca getId()'ye
     *  ihtiyaç duyar. */
    private static final int FLUSH_BATCH_SIZE = 500;

    private final BuildingRepository buildingRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final Building3dProperties building3dProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    public record ImportResult(
            int imported,
            int updated,
            int skippedNoNeighborhood,
            int skippedInvalidRecord,
            List<String> unmatchedNeighborhoods
    ) {}

    @Transactional
    public ImportResult importFromClasspath(String resourcePath, String districtName) throws IOException {
        District district = districtRepository.findByName(districtName)
                .orElseThrow(() -> new IllegalStateException(
                        "'" + districtName + "' ilçesi DB'de bulunamadı — önce district/neighborhood boundary migration'ları uygulanmalı"));

        Map<String, Neighborhood> neighborhoodByNormalizedName = new HashMap<>();
        for (Neighborhood n : neighborhoodRepository.findByDistrictIdOrderByNameAsc(district.getId())) {
            neighborhoodByNormalizedName.put(normalize(n.getName()), n);
        }
        if (neighborhoodByNormalizedName.isEmpty()) {
            throw new IllegalStateException("'" + districtName + "' için hiç mahalle bulunamadı — boundary migration'ları kontrol edin");
        }

        int imported = 0;
        int updated = 0;
        int skippedNoNeighborhood = 0;
        int skippedInvalidRecord = 0;
        Set<String> unmatched = new LinkedHashSet<>();

        try (var is = new ClassPathResource(resourcePath).getInputStream();
             var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                JsonNode node;
                try {
                    node = objectMapper.readTree(line);
                } catch (Exception e) {
                    log.warn("Satır {} parse edilemedi, atlanıyor: {}", lineNo, e.getMessage());
                    skippedInvalidRecord++;
                    continue;
                }

                String externalId = textOrNull(node, "externalId");
                String neighborhoodName = textOrNull(node, "neighborhoodName");
                if (externalId == null || neighborhoodName == null || !node.hasNonNull("geometry")) {
                    skippedInvalidRecord++;
                    continue;
                }

                Neighborhood neighborhood = neighborhoodByNormalizedName.get(normalize(neighborhoodName));
                if (neighborhood == null) {
                    unmatched.add(neighborhoodName);
                    skippedNoNeighborhood++;
                    continue;
                }

                BigDecimal heightM = decimalOrNull(node, "heightM");
                Integer levels = node.hasNonNull("levels") ? node.get("levels").asInt() : null;
                BigDecimal estimatedHeightM;
                String heightSource;
                if (heightM != null) {
                    estimatedHeightM = heightM;
                    heightSource = "osm_height";
                } else if (levels != null) {
                    estimatedHeightM = BigDecimal.valueOf(levels * building3dProperties.getLevelHeightM());
                    heightSource = "levels_x3";
                } else {
                    estimatedHeightM = BigDecimal.valueOf(building3dProperties.getFallbackHeightM());
                    heightSource = "fallback";
                }

                Optional<Building> existingOpt = buildingRepository.findBySourceAndExternalId(BuildingSource.OPENSTREETMAP, externalId);
                Building building = existingOpt.orElseGet(Building::new);
                building.setSource(BuildingSource.OPENSTREETMAP);
                building.setExternalId(externalId);
                building.setDistrict(district);
                building.setNeighborhood(neighborhood);
                building.setStreetName(textOrNull(node, "streetName"));
                building.setBuildingNumber(textOrNull(node, "buildingNumber"));
                building.setDisplayAddress(textOrNull(node, "displayAddress"));
                building.setLatitude(decimalOrNull(node, "latitude"));
                building.setLongitude(decimalOrNull(node, "longitude"));
                building.setGeometry(node.get("geometry").toString());
                building.setHeightM(heightM);
                building.setLevels(levels);
                building.setEstimatedHeightM(estimatedHeightM);
                building.setHeightSource(heightSource);
                building.setBuildingType(textOrNull(node, "buildingType"));
                building.setName(textOrNull(node, "name"));
                building.setSearchText(TurkishTextNormalizer.normalize(String.join(" ",
                        nullToEmpty(building.getStreetName()),
                        nullToEmpty(building.getBuildingNumber()),
                        nullToEmpty(building.getDisplayAddress()),
                        nullToEmpty(building.getName()))));
                building.setBboxMinLat(decimalOrNull(node, "bboxMinLat"));
                building.setBboxMaxLat(decimalOrNull(node, "bboxMaxLat"));
                building.setBboxMinLon(decimalOrNull(node, "bboxMinLon"));
                building.setBboxMaxLon(decimalOrNull(node, "bboxMaxLon"));

                buildingRepository.save(building);
                if (existingOpt.isPresent()) updated++; else imported++;

                if ((imported + updated) % FLUSH_BATCH_SIZE == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
                if ((imported + updated) % 2000 == 0) {
                    log.info("Bina import: {} işlendi ({} yeni, {} güncellendi)...", imported + updated, imported, updated);
                }
            }
        }

        log.info("Bina import tamamlandı — eklendi: {}, güncellendi: {}, mahalle bulunamadı: {}, geçersiz kayıt: {}",
                imported, updated, skippedNoNeighborhood, skippedInvalidRecord);

        return new ImportResult(imported, updated, skippedNoNeighborhood, skippedInvalidRecord, new ArrayList<>(unmatched));
    }

    /** Admin/maintenance import-status raporu (item 5) — Building3dProperties.enabledDistricts
     *  içindeki her ilçe (veri olsun olmasın, NO_DATA olarak görünür) + ayrıca gerçekten veri
     *  içeren ama henüz enabled olmayan ilçeler (varsa) birleştirilir. */
    @Transactional(readOnly = true)
    public List<BuildingImportStatusResponse> getImportStatus() {
        Map<UUID, BuildingRepository.DistrictBuildingStats> statsByDistrictId = buildingRepository.aggregateByDistrict().stream()
                .collect(Collectors.toMap(BuildingRepository.DistrictBuildingStats::getDistrictId, s -> s, (a, b) -> a));

        List<BuildingImportStatusResponse> result = new ArrayList<>();
        Set<UUID> reported = new LinkedHashSet<>();

        for (String districtName : building3dProperties.getEnabledDistricts()) {
            District d = districtRepository.findByName(districtName).orElse(null);
            if (d == null) continue;
            reported.add(d.getId());
            result.add(toStatusResponse(d.getId(), districtName, statsByDistrictId.get(d.getId())));
        }
        for (BuildingRepository.DistrictBuildingStats stats : statsByDistrictId.values()) {
            if (reported.contains(stats.getDistrictId())) continue;
            result.add(toStatusResponse(stats.getDistrictId(), stats.getDistrictName(), stats));
        }
        return result;
    }

    private BuildingImportStatusResponse toStatusResponse(UUID districtId, String districtName,
                                                            BuildingRepository.DistrictBuildingStats stats) {
        long count = stats != null ? stats.getBuildingCount() : 0;
        return BuildingImportStatusResponse.builder()
                .districtId(districtId)
                .districtName(districtName)
                .source(stats != null ? stats.getSource().name() : null)
                .buildingCount(count)
                .lastImportAt(stats != null ? stats.getLastImportAt() : null)
                .status(count > 0 ? "HAS_DATA" : "NO_DATA")
                .build();
    }

    /** Mahalle bazlı kapsama raporu (item 7) — bina bulunmayan mahalleler de buildingCount=0
     *  olarak dahil edilir, sonuçtan çıkarılmaz. */
    @Transactional(readOnly = true)
    public List<NeighborhoodCoverageResponse> getNeighborhoodCoverage(UUID districtId) {
        if (!districtRepository.existsById(districtId)) {
            throw new ResourceNotFoundException("District", "id", districtId);
        }
        Map<UUID, Long> countByNeighborhoodId = buildingRepository.countByNeighborhoodForDistrict(districtId).stream()
                .collect(Collectors.toMap(BuildingRepository.NeighborhoodBuildingCount::getNeighborhoodId,
                        BuildingRepository.NeighborhoodBuildingCount::getBuildingCount));

        return neighborhoodRepository.findByDistrictIdOrderByNameAsc(districtId).stream()
                .map(n -> NeighborhoodCoverageResponse.builder()
                        .neighborhoodId(n.getId())
                        .neighborhoodName(n.getName())
                        .buildingCount(countByNeighborhoodId.getOrDefault(n.getId(), 0L))
                        .build())
                .toList();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : BigDecimal.valueOf(v.asDouble());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** fetch_district_buildings.py'nin norm() fonksiyonuyla aynı normalizasyon (suffix stripping). */
    private static String normalize(String name) {
        String n = name.trim();
        String lower = n.toLowerCase(new Locale("tr"));
        for (String sfx : new String[]{" mahallesi", " mah.", " mh."}) {
            if (lower.endsWith(sfx)) {
                return n.substring(0, n.length() - sfx.length()).trim().toLowerCase(new Locale("tr"));
            }
        }
        return lower;
    }
}
