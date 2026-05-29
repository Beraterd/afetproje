package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.AddEmergencyContactRequest;
import com.afet.koordinasyon.dto.response.EmergencyContactResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.EmergencyContactService;
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
@RequestMapping("/api/users/me/emergency-contacts")
@RequiredArgsConstructor
@Tag(name = "Emergency Contacts", description = "Manage emergency contacts (yakınlarım)")
@SecurityRequirement(name = "bearerAuth")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    @GetMapping
    @Operation(summary = "List my emergency contacts")
    public ResponseEntity<List<EmergencyContactResponse>> listContacts(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(emergencyContactService.listContacts(principal));
    }

    @PostMapping
    @Operation(summary = "Add an emergency contact (max 5)")
    public ResponseEntity<EmergencyContactResponse> addContact(
            @Valid @RequestBody AddEmergencyContactRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emergencyContactService.addContact(request.getContactUserId(), principal));
    }

    @DeleteMapping("/{contactUserId}")
    @Operation(summary = "Remove an emergency contact")
    public ResponseEntity<Void> removeContact(
            @PathVariable UUID contactUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        emergencyContactService.removeContact(contactUserId, principal);
        return ResponseEntity.noContent().build();
    }
}
