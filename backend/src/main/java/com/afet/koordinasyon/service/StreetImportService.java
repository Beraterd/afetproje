package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.Street;
import com.afet.koordinasyon.dto.response.StreetImportStatusResponse;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.StreetRepository;
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
 * `backend/src/main/resources/data/streets/{slug}.ndjson`'daki OSM tabanlı sokak kayıtlarını
 * (bkz. scripts/fetch_district_streets.py) streets tablosuna idempotent şekilde import eder —
 * BuildingImportService ile aynı desen (bkz. o sınıfın yorumu). Python script her fiziksel
 * sokağı mahalle başına zaten TEK bir mantıksal satıra birleştirmiş olarak yazar (bkz. V91
 * migration doc) — bu servis yalnızca 1:1 okuma/upsert yapar, kendi merge mantığı YOKTUR.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreetImportService {

    private static final int FLUSH_BATCH_SIZE = 500;

    private final StreetRepository streetRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
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
            neighborhoodByNormalizedName.put(normalizeNeighborhoodName(n.getName()), n);
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
                String name = textOrNull(node, "name");
                String neighborhoodName = textOrNull(node, "neighborhoodName");
                if (externalId == null || name == null || neighborhoodName == null || !node.hasNonNull("geometry")) {
                    skippedInvalidRecord++;
                    continue;
                }

                Neighborhood neighborhood = neighborhoodByNormalizedName.get(normalizeNeighborhoodName(neighborhoodName));
                if (neighborhood == null) {
                    unmatched.add(neighborhoodName);
                    skippedNoNeighborhood++;
                    continue;
                }

                Street street = streetRepository
                        .findByDistrictIdAndNeighborhoodIdAndName(district.getId(), neighborhood.getId(), name)
                        .orElseGet(Street::new);
                boolean isNew = street.getId() == null;

                street.setSource("OPENSTREETMAP");
                street.setExternalId(externalId);
                street.setDistrict(district);
                street.setNeighborhood(neighborhood);
                street.setName(name);
                street.setNormalizedName(TurkishTextNormalizer.normalize(name));
                street.setRoadType(textOrNull(node, "roadType"));
                street.setGeometry(node.get("geometry").toString());
                street.setBboxMinLat(decimalOrNull(node, "bboxMinLat"));
                street.setBboxMaxLat(decimalOrNull(node, "bboxMaxLat"));
                street.setBboxMinLon(decimalOrNull(node, "bboxMinLon"));
                street.setBboxMaxLon(decimalOrNull(node, "bboxMaxLon"));
                street.setCenterLat(decimalOrNull(node, "centerLat"));
                street.setCenterLon(decimalOrNull(node, "centerLon"));

                streetRepository.save(street);
                if (isNew) imported++; else updated++;

                if ((imported + updated) % FLUSH_BATCH_SIZE == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        }

        log.info("Sokak import tamamlandı ({}) — eklendi: {}, güncellendi: {}, mahalle bulunamadı: {}, geçersiz kayıt: {}",
                districtName, imported, updated, skippedNoNeighborhood, skippedInvalidRecord);

        return new ImportResult(imported, updated, skippedNoNeighborhood, skippedInvalidRecord, new ArrayList<>(unmatched));
    }

    @Transactional(readOnly = true)
    public List<StreetImportStatusResponse> getImportStatus() {
        Map<UUID, StreetRepository.DistrictStreetStats> statsByDistrictId = streetRepository.aggregateByDistrict().stream()
                .collect(Collectors.toMap(StreetRepository.DistrictStreetStats::getDistrictId, s -> s, (a, b) -> a));

        return districtRepository.findAll().stream()
                .map(d -> {
                    StreetRepository.DistrictStreetStats stats = statsByDistrictId.get(d.getId());
                    long count = stats != null ? stats.getStreetCount() : 0;
                    return StreetImportStatusResponse.builder()
                            .districtId(d.getId())
                            .districtName(d.getName())
                            .streetCount(count)
                            .neighborhoodCount(stats != null ? stats.getNeighborhoodCount() : 0)
                            .lastImportAt(stats != null ? stats.getLastImportAt() : null)
                            .status(count > 0 ? "HAS_DATA" : "NO_DATA")
                            .build();
                })
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

    /** fetch_district_buildings.py'nin norm() fonksiyonuyla aynı normalizasyon (yalnızca mahalle
     *  adı suffix stripping — BuildingImportService.normalize() ile birebir aynı, kasıtlı olarak
     *  kopyalanmıştır: iki servis farklı entity'ler import eder, paylaşılan bir yardımcıya
     *  taşımak bu ikisi arasında gereksiz bir bağımlılık kurar). */
    private static String normalizeNeighborhoodName(String name) {
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
