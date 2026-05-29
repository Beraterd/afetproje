package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.ActiveSimulationResponse;
import com.afet.koordinasyon.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
@Tag(name = "Simulations Public", description = "Public simulation status endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SimulationPublicController {

    private final SimulationService simulationService;

    @GetMapping("/active")
    @Operation(summary = "Get the most recent active simulation (if any)")
    public ResponseEntity<ActiveSimulationResponse> getActiveSimulation() {
        ActiveSimulationResponse response = simulationService.getActiveSimulation();
        return ResponseEntity.ok(response);
    }
}
