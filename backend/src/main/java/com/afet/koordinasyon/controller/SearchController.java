package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.GlobalSearchResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Uygulama çapında global arama (rol/kapsam bazlı)")
@SecurityRequirement(name = "bearerAuth")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Olay, hasar tespiti, ekip, kullanıcı, kaynak talebi ve ilçe/mahalle üzerinde arama yapar")
    public ResponseEntity<GlobalSearchResponse> search(
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(searchService.search(q, principal));
    }
}
