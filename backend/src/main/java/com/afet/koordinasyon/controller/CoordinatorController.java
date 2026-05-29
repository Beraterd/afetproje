package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.AssignCoordinatorRequest;
import com.afet.koordinasyon.dto.request.UpdateCoordinatorLocationRequest;
import com.afet.koordinasyon.dto.response.CoordinatorDistrictResponse;
import com.afet.koordinasyon.dto.response.CoordinatorNeighborhoodResponse;
import com.afet.koordinasyon.dto.response.UserResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.CoordinatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/coordinators")
@RequiredArgsConstructor
@Tag(name = "Coordinators", description = "Coordinator assignment management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'DISTRICT_COORDINATOR')")
public class CoordinatorController {

    private final CoordinatorService coordinatorService;

    @GetMapping("/eligible-users")
    @Operation(summary = "List users eligible for coordinator assignment")
    public ResponseEntity<List<UserResponse>> listEligibleUsers(
            @RequestParam(required = false) String assignmentType,
            @RequestParam(required = false) UUID districtId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(coordinatorService.listEligibleUsers(principal, assignmentType, districtId));
    }

    @GetMapping("/districts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all districts with their coordinators")
    public ResponseEntity<List<CoordinatorDistrictResponse>> listDistrictCoordinators() {
        return ResponseEntity.ok(coordinatorService.listDistrictCoordinators());
    }

    @PutMapping("/districts/{districtId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a district coordinator (Admin only)")
    public ResponseEntity<CoordinatorDistrictResponse> assignDistrictCoordinator(
            @PathVariable UUID districtId,
            @Valid @RequestBody AssignCoordinatorRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                coordinatorService.assignDistrictCoordinator(districtId, request.getUserId(), principal));
    }

    @GetMapping("/neighborhoods")
    @Operation(summary = "List neighborhoods with coordinators (Admin: all, Coordinator: own district)")
    public ResponseEntity<List<CoordinatorNeighborhoodResponse>> listNeighborhoodCoordinators(
            @RequestParam(required = false) UUID districtId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(coordinatorService.listNeighborhoodCoordinators(districtId, principal));
    }

    @PutMapping("/neighborhoods/{neighborhoodId}")
    @Operation(summary = "Assign a neighborhood coordinator (Admin or district coordinator for own district)")
    public ResponseEntity<CoordinatorNeighborhoodResponse> assignNeighborhoodCoordinator(
            @PathVariable UUID neighborhoodId,
            @Valid @RequestBody AssignCoordinatorRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                coordinatorService.assignNeighborhoodCoordinator(neighborhoodId, request.getUserId(), principal));
    }

    @PatchMapping("/districts/{districtId}/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRICT_COORDINATOR')")
    @Operation(summary = "Update district coordinator location (Admin: any district, Coordinator: own district)")
    public ResponseEntity<CoordinatorDistrictResponse> updateDistrictCoordinatorLocation(
            @PathVariable UUID districtId,
            @Valid @RequestBody UpdateCoordinatorLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                coordinatorService.updateDistrictCoordinatorLocation(districtId, request, principal));
    }

    @PatchMapping("/neighborhoods/{neighborhoodId}/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Update neighborhood coordinator location")
    public ResponseEntity<CoordinatorNeighborhoodResponse> updateNeighborhoodCoordinatorLocation(
            @PathVariable UUID neighborhoodId,
            @Valid @RequestBody UpdateCoordinatorLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                coordinatorService.updateNeighborhoodCoordinatorLocation(neighborhoodId, request, principal));
    }
}
