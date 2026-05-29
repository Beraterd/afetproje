package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.SendStatusMessageRequest;
import com.afet.koordinasyon.dto.response.StatusMessageResponse;
import com.afet.koordinasyon.dto.response.StatusTemplateResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.EmergencyStatusMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/emergency-status-messages")
@RequiredArgsConstructor
@Tag(name = "Emergency Status Messages", description = "Send emergency status messages to contacts")
@SecurityRequirement(name = "bearerAuth")
public class EmergencyStatusMessageController {

    private final EmergencyStatusMessageService emergencyStatusMessageService;

    @GetMapping("/templates")
    @Operation(summary = "Get available status message templates (frontend must use these keys)")
    public ResponseEntity<List<StatusTemplateResponse>> getTemplates() {
        return ResponseEntity.ok(emergencyStatusMessageService.getTemplates());
    }

    @PostMapping("/send")
    @Operation(summary = "Send a status message to all emergency contacts")
    public ResponseEntity<StatusMessageResponse> sendStatusMessage(
            @Valid @RequestBody SendStatusMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emergencyStatusMessageService.sendStatusMessage(request, principal));
    }

    @GetMapping
    @Operation(summary = "Get my sent status messages")
    public ResponseEntity<List<StatusMessageResponse>> getMyMessages(
            @RequestParam(required = false) UUID simulationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(emergencyStatusMessageService.getMyMessages(simulationId, principal));
    }
}
