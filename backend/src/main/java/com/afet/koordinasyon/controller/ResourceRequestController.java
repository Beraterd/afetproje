package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.CreateResourceRequestRequest;
import com.afet.koordinasyon.dto.request.UpdateResourceStatusRequest;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.dto.response.ResourceRequestResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.ResourceRequestService;
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
@RequestMapping("/api/resource-requests")
@RequiredArgsConstructor
@Tag(name = "Resource Requests", description = "Resource and equipment request management")
@SecurityRequirement(name = "bearerAuth")
public class ResourceRequestController {

    private final ResourceRequestService resourceRequestService;

    @GetMapping
    @Operation(summary = "List resource requests (role-filtered)")
    public ResponseEntity<PagedResponse<ResourceRequestResponse>> listRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @RequestParam(required = false) com.afet.koordinasyon.domain.enums.ResourceType resourceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(resourceRequestService.listRequests(
                page, size, districtId, neighborhoodId, resourceType, status, priority, search, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource request by ID")
    public ResponseEntity<ResourceRequestResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(resourceRequestService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Create a new resource request")
    public ResponseEntity<ResourceRequestResponse> create(
            @Valid @RequestBody CreateResourceRequestRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceRequestService.create(request, principal));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Update resource request status")
    public ResponseEntity<ResourceRequestResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResourceStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(resourceRequestService.updateStatus(id, request, principal));
    }
}
