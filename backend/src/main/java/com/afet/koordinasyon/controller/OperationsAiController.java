package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.OperationsAiRequest;
import com.afet.koordinasyon.dto.response.OperationsAiResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.OperationsAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "Operations AI", description = "AI-powered operations decision support assistant")
@SecurityRequirement(name = "bearerAuth")
public class OperationsAiController {

    private final OperationsAiService operationsAiService;

    @PostMapping("/operations-assistant")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Analyze operational data with AI decision support")
    public ResponseEntity<OperationsAiResponse> analyze(
            @Valid @RequestBody OperationsAiRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(operationsAiService.analyze(request, principal));
    }
}
