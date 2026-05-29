package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.enums.DocumentType;
import com.afet.koordinasyon.dto.response.DocumentDownloadResponse;
import com.afet.koordinasyon.dto.response.DocumentResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "User document management")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "List all documents uploaded by the current user")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getMyDocuments(principal.getId()));
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Upload a document")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(principal.getId(), documentType, file));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Get a download URL for a specific document")
    public ResponseEntity<DocumentDownloadResponse> getDownloadUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getDownloadUrl(principal.getId(), id));
    }
}
