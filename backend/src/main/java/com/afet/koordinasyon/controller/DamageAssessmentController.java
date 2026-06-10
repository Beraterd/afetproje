package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.AssignDamageAssessmentRequest;
import com.afet.koordinasyon.dto.request.CreateDamageAssessmentRequest;
import com.afet.koordinasyon.dto.request.VerifyDamageAssessmentRequest;
import com.afet.koordinasyon.dto.response.DamageAssessmentAiTriggerResponse;
import com.afet.koordinasyon.dto.response.DamageAssessmentAssignmentResponse;
import com.afet.koordinasyon.dto.response.EligibleAssigneeResponse;
import com.afet.koordinasyon.dto.response.DamageAssessmentResponse;
import com.afet.koordinasyon.dto.response.DamagePointResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.DamageAssessmentService;
import com.afet.koordinasyon.service.ai.DamageAiBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/damage-assessments")
@RequiredArgsConstructor
@Tag(name = "Damage Assessments", description = "Building damage assessment management")
@SecurityRequirement(name = "bearerAuth")
public class DamageAssessmentController {

    private final DamageAssessmentService damageAssessmentService;
    private final DamageAiBatchService damageAiBatchService;

    @GetMapping
    @Operation(summary = "List damage assessments (role-filtered)")
    public ResponseEntity<PagedResponse<DamageAssessmentResponse>> listAssessments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @RequestParam(required = false) String damageLevel,
            @RequestParam(required = false) String verificationStatus,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(damageAssessmentService.listAssessments(
                page, size, districtId, neighborhoodId, damageLevel, verificationStatus, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get damage assessment by ID")
    public ResponseEntity<DamageAssessmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(damageAssessmentService.getById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR','VOLUNTEER')")
    @Operation(summary = "Create a new damage assessment (requires at least 1 photo)")
    public ResponseEntity<DamageAssessmentResponse> create(
            @RequestPart("data") @Valid CreateDamageAssessmentRequest request,
            @RequestPart("photos") List<MultipartFile> photos,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(damageAssessmentService.create(request, photos, principal));
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Update verification status of a damage assessment")
    public ResponseEntity<DamageAssessmentResponse> verify(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyDamageAssessmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(damageAssessmentService.verify(id, request, principal));
    }

    @GetMapping("/my")
    @Operation(summary = "Get damage assessments reported by the current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<DamageAssessmentResponse>> getMyAssessments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(damageAssessmentService.listMyAssessments(page, size, principal));
    }

    @GetMapping("/by-neighborhood/{neighborhoodId}")
    @Operation(summary = "Get damage assessment pins for a specific neighborhood (map use)")
    public ResponseEntity<List<DamagePointResponse>> getByNeighborhood(@PathVariable UUID neighborhoodId) {
        return ResponseEntity.ok(damageAssessmentService.getDamagePoints(null, neighborhoodId));
    }

    @GetMapping("/photos/{token}")
    @Operation(summary = "Download a damage assessment photo by token")
    public void servePhoto(@PathVariable UUID token, HttpServletResponse response) {
        damageAssessmentService.servePhoto(token, response);
    }

    // ── Assignment endpoints ──────────────────────────────────────────────────

    @GetMapping("/{id}/eligible-assignees")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "List eligible assignees — active HASAR_TESPIT_EKIBI volunteers in the same district")
    public ResponseEntity<List<EligibleAssigneeResponse>> getEligibleAssignees(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(damageAssessmentService.getEligibleAssignees(id, principal));
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "List active assignments for a damage assessment")
    public ResponseEntity<List<DamageAssessmentAssignmentResponse>> listAssignments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(damageAssessmentService.listAssignments(id, principal));
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Assign a user to a damage assessment (auto-sets status to ASSIGNED)")
    public ResponseEntity<DamageAssessmentAssignmentResponse> assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDamageAssessmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(damageAssessmentService.assign(id, request, principal));
    }

    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Remove an assignment from a damage assessment")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable UUID id,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        damageAssessmentService.removeAssignment(id, assignmentId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ai-analysis")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Trigger AI pre-assessment for a damage assessment (background queue, returns 202)")
    public ResponseEntity<DamageAssessmentAiTriggerResponse> triggerAiAnalysis(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.accepted().body(damageAssessmentService.triggerAiAnalysis(id, principal));
    }

    @PostMapping("/ai/enqueue-missing")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Enqueue all records missing AI analysis into the background queue (idempotent)")
    public ResponseEntity<java.util.Map<String, Object>> enqueueMissing() {
        int enqueued = damageAiBatchService.enqueueAllMissing();
        return ResponseEntity.accepted().body(java.util.Map.of("enqueued", enqueued));
    }
}
