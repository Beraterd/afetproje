package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.enums.ResourceType;
import com.afet.koordinasyon.dto.request.CreateResourceStockRequest;
import com.afet.koordinasyon.dto.request.UpdateResourceStockRequest;
import com.afet.koordinasyon.dto.request.UpdateStockQuantityRequest;
import com.afet.koordinasyon.dto.response.ResourceStockMovementResponse;
import com.afet.koordinasyon.dto.response.ResourceStockResponse;
import com.afet.koordinasyon.dto.response.ResourceStockSummaryResponse;
import com.afet.koordinasyon.dto.response.StockLookupResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.ResourceStockService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resource-stocks")
@RequiredArgsConstructor
@Tag(name = "Resource Stocks", description = "Bölgesel kaynak/ekipman stok yönetimi")
@SecurityRequirement(name = "bearerAuth")
public class ResourceStockController {

    private final ResourceStockService stockService;

    @GetMapping
    @Operation(summary = "Stok listesi (rol bazlı kapsam + filtreler)")
    public ResponseEntity<List<ResourceStockResponse>> list(
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @RequestParam(required = false) ResourceType category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean criticalOnly,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(stockService.list(
                districtId, neighborhoodId, category, status, search, criticalOnly, principal));
    }

    @GetMapping("/summary")
    @Operation(summary = "Stok özet kartları (rol bazlı kapsam)")
    public ResponseEntity<ResourceStockSummaryResponse> summary(
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(stockService.summary(districtId, neighborhoodId, principal));
    }

    @GetMapping("/lookup")
    @Operation(summary = "Talep uyarısı için bölge + kaynak türü stok özeti")
    public ResponseEntity<StockLookupResponse> lookup(
            @RequestParam UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @RequestParam ResourceType resourceType,
            @RequestParam(required = false) Integer quantity) {
        return ResponseEntity.ok(stockService.lookup(districtId, neighborhoodId, resourceType, quantity));
    }

    @GetMapping("/{id}/movements")
    @Operation(summary = "Stok hareket geçmişi")
    public ResponseEntity<List<ResourceStockMovementResponse>> movements(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(stockService.getMovements(id, principal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Yeni stok kaydı oluştur")
    public ResponseEntity<ResourceStockResponse> create(
            @Valid @RequestBody CreateResourceStockRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.create(request, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Stok meta verisini güncelle")
    public ResponseEntity<ResourceStockResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResourceStockRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(stockService.update(id, request, principal));
    }

    @PatchMapping("/{id}/quantity")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Stok miktarını güncelle (hareket kaydı oluşur)")
    public ResponseEntity<ResourceStockResponse> updateQuantity(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStockQuantityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(stockService.updateQuantity(id, request, principal));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Stok kaydını pasife al")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        stockService.deactivate(id, principal);
        return ResponseEntity.noContent().build();
    }
}
