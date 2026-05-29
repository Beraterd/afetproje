package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.CreateSimulationRequest;
import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/simulations")
@RequiredArgsConstructor
@Tag(name = "Simulations", description = "Earthquake simulation management (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping
    @Operation(summary = "Trigger an earthquake simulation and notify all users in the district")
    public ResponseEntity<SimulationCreatedResponse> triggerSimulation(
            @Valid @RequestBody CreateSimulationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(simulationService.triggerSimulation(request, principal));
    }

    @GetMapping
    @Operation(summary = "List all simulations with pagination")
    public ResponseEntity<PagedResponse<SimulationDetailResponse>> listSimulations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(simulationService.listSimulations(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get simulation details by ID")
    public ResponseEntity<SimulationDetailResponse> getSimulation(@PathVariable UUID id) {
        return ResponseEntity.ok(simulationService.getSimulation(id));
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "Get notification logs for a simulation")
    public ResponseEntity<PagedResponse<SimulationLogResponse>> getSimulationLogs(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(simulationService.getSimulationLogs(id, status, page, size));
    }
}
