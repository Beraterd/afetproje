package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.StreetDetailResponse;
import com.afet.koordinasyon.dto.response.StreetSearchResultResponse;
import com.afet.koordinasyon.service.StreetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/streets")
@RequiredArgsConstructor
@Tag(name = "Streets", description = "Mahalle-scoped gerçek sokak/cadde arama (Operasyon Haritası autocomplete)")
@SecurityRequirement(name = "bearerAuth")
public class StreetController {

    private final StreetService streetService;

    @GetMapping("/search")
    @Operation(summary = "Seçili mahalledeki sokak/cadde adlarında autocomplete arama (min 2 karakter, districtId+neighborhoodId zorunlu)")
    public ResponseEntity<List<StreetSearchResultResponse>> search(
            @RequestParam String q,
            @RequestParam UUID districtId,
            @RequestParam UUID neighborhoodId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(streetService.search(q, districtId, neighborhoodId, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sokak detayı — harita highlight'ı için tam geometri döner")
    public ResponseEntity<StreetDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(streetService.getById(id));
    }
}
