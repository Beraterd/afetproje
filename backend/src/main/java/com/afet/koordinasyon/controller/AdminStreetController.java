package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.dto.response.StreetImportStatusResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.service.StreetImportService;
import com.afet.koordinasyon.util.TurkishTextNormalizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/streets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Streets", description = "İlçe bazlı sokak veri import + import durumu (yalnızca admin)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AdminStreetController {

    private static final String RESOURCE_DIR = "data/streets/";

    private final StreetImportService streetImportService;
    private final DistrictRepository districtRepository;

    @PostMapping("/import/{districtId}")
    @Operation(
        summary = "Repo'ya gömülü bir ilçenin OSM sokak veri setini içe aktar (idempotent, admin only)",
        description = """
            Kaynak: scripts/fetch_district_streets.py tarafından üretilen
            backend/src/main/resources/data/streets/{ilçe-slug}.ndjson (OpenStreetMap, ODbL).
            (districtId, neighborhoodId, name) üçlüsüne göre idempotent: aynı sokak ikinci
            çalıştırmada güncellenir, çoğaltılmaz.
            """)
    public ResponseEntity<Map<String, Object>> importDistrictStreets(@PathVariable UUID districtId) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));
        String resourcePath = RESOURCE_DIR + TurkishTextNormalizer.normalize(district.getName()) + ".ndjson";
        try {
            StreetImportService.ImportResult result = streetImportService.importFromClasspath(resourcePath, district.getName());
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
            log.error("'{}' sokak import başarısız", district.getName(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Import başarısız: " + e.getMessage()));
        }
    }

    @GetMapping("/import-status")
    @Operation(summary = "İlçe başına sokak import durumu (sokak sayısı, kapsanan mahalle sayısı, son import zamanı)")
    public ResponseEntity<List<StreetImportStatusResponse>> importStatus() {
        return ResponseEntity.ok(streetImportService.getImportStatus());
    }
}
