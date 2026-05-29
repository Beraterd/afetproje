package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.PurgeOperationalDataRequest;
import com.afet.koordinasyon.dto.response.PurgeOperationalDataResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.AdminMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/maintenance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Maintenance", description = "Sistem bakım ve temizlik işlemleri (yalnızca admin)")
@SecurityRequirement(name = "bearerAuth")
public class AdminMaintenanceController {

    private final AdminMaintenanceService adminMaintenanceService;

    @PostMapping("/purge-operational-data")
    @Operation(summary = "Operasyonel veriyi toplu sil (Admin only)",
               description = "Seçilen kategorilerdeki tüm kayıtları siler. " +
                             "confirmationText alanı tam olarak 'PURGE' olmalıdır. " +
                             "Protected e-posta listesindeki adminler asla silinmez.")
    public ResponseEntity<PurgeOperationalDataResponse> purgeOperationalData(
            @RequestBody PurgeOperationalDataRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminMaintenanceService.purgeOperationalData(req, principal.getId()));
    }
}
