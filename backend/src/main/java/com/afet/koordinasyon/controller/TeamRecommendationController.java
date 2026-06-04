package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.enums.RecommendationStatus;
import com.afet.koordinasyon.dto.request.ApproveRecommendationRequest;
import com.afet.koordinasyon.dto.request.TeamRecommendationRequest;
import com.afet.koordinasyon.dto.response.ApproveRecommendationResponse;
import com.afet.koordinasyon.dto.response.TeamRecommendationResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.TeamRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/team-recommendations")
@RequiredArgsConstructor
@Tag(name = "TeamRecommendations", description = "AI destekli ekip öneri sistemi")
@SecurityRequirement(name = "bearerAuth")
public class TeamRecommendationController {

    private final TeamRecommendationService recommendationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Yeni ekip önerisi oluştur")
    public ResponseEntity<TeamRecommendationResponse> create(
            @Valid @RequestBody TeamRecommendationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recommendationService.createRecommendation(request, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Ekip önerilerini listele")
    public ResponseEntity<Page<TeamRecommendationResponse>> list(
            @RequestParam(required = false) RecommendationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(recommendationService.list(status, page, size, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Öneri detayını getir")
    public ResponseEntity<TeamRecommendationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(recommendationService.getById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR')")
    @Operation(summary = "Öneriyi seçilen kişilerle onayla, davet maili gönder")
    public ResponseEntity<ApproveRecommendationResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveRecommendationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(recommendationService.approveWithSelectedMembers(id, request, principal));
    }

    @PostMapping("/{id}/approve-all")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR')")
    @Operation(summary = "Öneriyi tüm üyelerle onayla (eski akış)")
    public ResponseEntity<TeamRecommendationResponse> approveAll(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(recommendationService.approve(id, principal));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR')")
    @Operation(summary = "Öneriyi reddet")
    public ResponseEntity<TeamRecommendationResponse> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(recommendationService.reject(id, principal));
    }
}
