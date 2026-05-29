package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.RejectDocumentRequest;
import com.afet.koordinasyon.dto.response.DocumentDownloadResponse;
import com.afet.koordinasyon.dto.response.DocumentResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.dto.response.PendingDocumentResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.DocumentService;
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
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
@Tag(name = "Admin Documents", description = "Document approval management for admins and coordinators")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR')")
public class AdminDocumentController {

    private final DocumentService documentService;

    @GetMapping("/pending")
    @Operation(summary = "List pending documents (filtered by district for coordinators)")
    public ResponseEntity<PagedResponse<PendingDocumentResponse>> getPendingDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getPendingDocuments(
                principal.getId(), principal.getRole(), page, size));
    }

    @GetMapping("/{id}/view-url")
    @Operation(summary = "Get a view URL for a pending document (admin/coordinator access)")
    public ResponseEntity<DocumentDownloadResponse> getViewUrl(
            @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getAdminDocumentViewUrl(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a document")
    public ResponseEntity<DocumentResponse> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.approveDocument(principal.getId(), id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a document with a reason")
    public ResponseEntity<DocumentResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.rejectDocument(principal.getId(), id, request.getReason()));
    }
}
