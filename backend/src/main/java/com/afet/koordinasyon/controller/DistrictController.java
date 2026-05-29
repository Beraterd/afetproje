package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.DistrictResponse;
import com.afet.koordinasyon.dto.response.NeighborhoodSummaryResponse;
import com.afet.koordinasyon.service.DistrictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
@Tag(name = "Districts", description = "District and neighborhood listing endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DistrictController {

    private final DistrictService districtService;

    @GetMapping
    @Operation(summary = "List all active districts")
    public ResponseEntity<List<DistrictResponse>> listDistricts() {
        return ResponseEntity.ok(districtService.listActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a district by ID")
    public ResponseEntity<DistrictResponse> getDistrict(@PathVariable UUID id) {
        return ResponseEntity.ok(districtService.getById(id));
    }

    @GetMapping("/{id}/neighborhoods")
    @Operation(summary = "List neighborhoods in a district")
    public ResponseEntity<List<NeighborhoodSummaryResponse>> listNeighborhoods(@PathVariable UUID id) {
        return ResponseEntity.ok(districtService.listNeighborhoods(id));
    }
}
