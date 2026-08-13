package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.dto.response.BuildingImportStatusResponse;
import com.afet.koordinasyon.dto.response.NeighborhoodCoverageResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.service.BuildingImportService;
import com.afet.koordinasyon.util.TurkishTextNormalizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/buildings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Buildings", description = "İlçe bazlı bina verisi import + import durumu (yalnızca admin)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AdminBuildingController {

    private static final String RESOURCE_DIR = "data/buildings/";

    private final BuildingImportService buildingImportService;
    private final DistrictRepository districtRepository;

    @PostMapping("/import/{districtId}")
    @Operation(
        summary = "Repo'ya gömülü bir ilçenin OSM bina veri setini içe aktar (idempotent, admin only)",
        description = """
            Kaynak: scripts/fetch_district_buildings.py tarafından üretilen
            backend/src/main/resources/data/buildings/{ilçe-slug}.ndjson (OpenStreetMap, ODbL).
            Kullanıcıdan dosya alınmaz — repo'ya gömülü, kod incelemesinden geçmiş sabit veri seti okunur.
            (source, externalId) çiftine göre idempotent: aynı bina ikinci çalıştırmada güncellenir, çoğaltılmaz.
            Bir ilçenin importu başarısız olsa da diğer ilçelerin verisi etkilenmez.
            """)
    public ResponseEntity<Map<String, Object>> importDistrictBuildings(@PathVariable UUID districtId) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));
        String resourcePath = RESOURCE_DIR + TurkishTextNormalizer.normalize(district.getName()) + ".ndjson";
        try {
            BuildingImportService.ImportResult result = buildingImportService.importFromClasspath(resourcePath, district.getName());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("districtId", district.getId());
            body.put("districtName", district.getName());
            body.put("imported", result.imported());
            body.put("updated", result.updated());
            body.put("skippedNoNeighborhood", result.skippedNoNeighborhood());
            body.put("skippedInvalidRecord", result.skippedInvalidRecord());
            body.put("unmatchedNeighborhoods", result.unmatchedNeighborhoods());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("'{}' bina import başarısız", district.getName(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Import başarısız: " + e.getMessage()));
        }
    }

    @GetMapping("/import-status")
    @Operation(summary = "İlçe başına bina import durumu (bina sayısı, son import zamanı, kaynak) — item 5")
    public ResponseEntity<List<BuildingImportStatusResponse>> importStatus() {
        return ResponseEntity.ok(buildingImportService.getImportStatus());
    }

    @GetMapping("/neighborhood-coverage")
    @Operation(summary = "Bir ilçenin mahalle bazlı bina kapsama raporu (0 bina çıkan mahalleler dahil) — item 7")
    public ResponseEntity<List<NeighborhoodCoverageResponse>> neighborhoodCoverage(@RequestParam UUID districtId) {
        return ResponseEntity.ok(buildingImportService.getNeighborhoodCoverage(districtId));
    }
}
