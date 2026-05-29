package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.service.DamageAssessmentService;
import com.afet.koordinasyon.service.MapService;
import com.afet.koordinasyon.service.ResourceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
@Tag(name = "Map", description = "Risk map data endpoints with GeoJSON polygons")
@SecurityRequirement(name = "bearerAuth")
public class MapController {

    private final MapService mapService;
    private final DamageAssessmentService damageAssessmentService;
    private final ResourceRequestService resourceRequestService;

    @GetMapping("/districts")
    @Operation(summary = "Get all districts with risk scores and GeoJSON polygons")
    public ResponseEntity<List<MapDistrictResponse>> getMapDistricts() {
        return ResponseEntity.ok(mapService.getDistrictsForMap());
    }

    @GetMapping("/districts/{districtId}/neighborhoods")
    @Operation(summary = "Get neighborhoods in a district with risk scores and GeoJSON polygons")
    public ResponseEntity<List<MapNeighborhoodResponse>> getMapNeighborhoods(@PathVariable UUID districtId) {
        return ResponseEntity.ok(mapService.getNeighborhoodsForMap(districtId));
    }

    @GetMapping("/damage-summary")
    @Operation(summary = "Get damage assessment counts per neighborhood for map layer")
    public ResponseEntity<List<MapDamageSummaryResponse>> getDamageSummary() {
        return ResponseEntity.ok(damageAssessmentService.getDamageSummary());
    }

    @GetMapping("/damage-points")
    @Operation(summary = "Get individual damage assessment pins for map layer")
    public ResponseEntity<List<DamagePointResponse>> getDamagePoints(
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId) {
        return ResponseEntity.ok(damageAssessmentService.getDamagePoints(districtId, neighborhoodId));
    }

    @GetMapping("/resource-summary")
    @Operation(summary = "Get open resource request counts per neighborhood for map layer")
    public ResponseEntity<List<ResourceSummaryResponse>> getResourceSummary() {
        return ResponseEntity.ok(resourceRequestService.getResourceSummary());
    }

    @GetMapping("/district-coordinators")
    @Operation(summary = "Get district coordinator locations for map layer (only those with location set)")
    public ResponseEntity<List<DistrictCoordinatorMapResponse>> getDistrictCoordinatorsForMap() {
        return ResponseEntity.ok(mapService.getDistrictCoordinatorsForMap());
    }

    @GetMapping("/neighborhood-coordinators")
    @Operation(summary = "Get neighborhood coordinator locations for map layer")
    public ResponseEntity<List<NeighborhoodCoordinatorMapResponse>> getNeighborhoodCoordinatorsForMap(
            @RequestParam(required = false) UUID districtId) {
        return ResponseEntity.ok(mapService.getNeighborhoodCoordinatorsForMap(districtId));
    }
}
