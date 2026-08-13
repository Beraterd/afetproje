package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.Building3dConfigResponse;
import com.afet.koordinasyon.dto.response.BuildingDetailResponse;
import com.afet.koordinasyon.dto.response.BuildingSearchResultResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.service.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
@Tag(name = "Buildings", description = "Gerçek bina footprint'leri ve 3B bina katmanı (ilçe bazlı, bkz. /config enabledDistricts)")
@SecurityRequirement(name = "bearerAuth")
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping
    @Operation(summary = "Viewport (bbox) kapsamındaki binaları GeoJSON FeatureCollection olarak döndürür",
               description = "bbox formatı: minLon,minLat,maxLon,maxLat. Tüm ilçe binaları tek seferde DÖNMEZ.")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @RequestParam(required = false) String bbox,
            @RequestParam(required = false) Integer zoom) {

        Double minLat = null, maxLat = null, minLon = null, maxLon = null;
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = bbox.split(",");
            if (parts.length != 4) {
                throw new BusinessRuleException("bbox formatı geçersiz — beklenen: minLon,minLat,maxLon,maxLat");
            }
            try {
                minLon = Double.parseDouble(parts[0]);
                minLat = Double.parseDouble(parts[1]);
                maxLon = Double.parseDouble(parts[2]);
                maxLat = Double.parseDouble(parts[3]);
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("bbox sayısal değerler içermelidir");
            }
        }

        return ResponseEntity.ok(buildingService.listBuildings(districtId, neighborhoodId, minLat, maxLat, minLon, maxLon, zoom));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Bina detayı — hasar durumu, rapor sayısı, en güncel değerlendirme (PII içermez)")
    public ResponseEntity<BuildingDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(buildingService.getById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Adres/sokak/bina no arama (min 3 karakter, districtId zorunlu, sonuç limitli)")
    public ResponseEntity<List<BuildingSearchResultResponse>> search(
            @RequestParam String q,
            @RequestParam UUID districtId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(buildingService.search(q, districtId, limit));
    }

    @GetMapping("/config")
    @Operation(summary = "3B bina pilotu yapılandırması (aktif ilçeler, zoom eşikleri)")
    public ResponseEntity<Building3dConfigResponse> getConfig() {
        return ResponseEntity.ok(buildingService.getConfig());
    }
}
