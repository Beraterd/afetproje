package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.UpsertCoordinationCenterRequest;
import com.afet.koordinasyon.dto.response.CoordinationCenterResponse;
import com.afet.koordinasyon.dto.response.CoordinationCenterStatusResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.CoordinationCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/coordination-centers")
@RequiredArgsConstructor
@Tag(name = "Coordination Centers", description = "District and neighborhood operational coordination center management")
@SecurityRequirement(name = "bearerAuth")
public class CoordinationCenterController {

    private final CoordinationCenterService coordinationCenterService;

    // ── My Status (for first-login popup) ────────────────────────────────────

    @GetMapping("/my-status")
    @Operation(summary = "Get coordination center setup status for the current user")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    public ResponseEntity<CoordinationCenterStatusResponse> getMyStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(coordinationCenterService.getMyStatus(principal));
    }

    // ── District Coordination Centers ─────────────────────────────────────────

    @GetMapping("/districts/{districtId}")
    @Operation(summary = "Get coordination center for a district")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR')")
    public ResponseEntity<CoordinationCenterResponse> getDistrictCenter(
            @PathVariable UUID districtId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(coordinationCenterService.getDistrictCenter(districtId, principal));
    }

    @PutMapping("/districts/{districtId}")
    @Operation(summary = "Create or update coordination center for a district")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR')")
    public ResponseEntity<CoordinationCenterResponse> upsertDistrictCenter(
            @PathVariable UUID districtId,
            @Valid @RequestBody UpsertCoordinationCenterRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(coordinationCenterService.upsertDistrictCenter(districtId, request, principal));
    }

    // ── Neighborhood Coordination Centers ────────────────────────────────────

    @GetMapping("/neighborhoods/{neighborhoodId}")
    @Operation(summary = "Get coordination center for a neighborhood")
    @PreAuthorize("hasAnyRole('ADMIN','NEIGHBORHOOD_COORDINATOR','DISTRICT_COORDINATOR')")
    public ResponseEntity<CoordinationCenterResponse> getNeighborhoodCenter(
            @PathVariable UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(coordinationCenterService.getNeighborhoodCenter(neighborhoodId, principal));
    }

    @PutMapping("/neighborhoods/{neighborhoodId}")
    @Operation(summary = "Create or update coordination center for a neighborhood")
    @PreAuthorize("hasAnyRole('ADMIN','NEIGHBORHOOD_COORDINATOR','DISTRICT_COORDINATOR')")
    public ResponseEntity<CoordinationCenterResponse> upsertNeighborhoodCenter(
            @PathVariable UUID neighborhoodId,
            @Valid @RequestBody UpsertCoordinationCenterRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(coordinationCenterService.upsertNeighborhoodCenter(neighborhoodId, request, principal));
    }
}
