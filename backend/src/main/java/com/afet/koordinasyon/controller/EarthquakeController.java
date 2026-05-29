package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.EarthquakeDebugStatusResponse;
import com.afet.koordinasyon.dto.response.EarthquakeEventResponse;
import com.afet.koordinasyon.dto.response.EarthquakeSyncResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.service.AfadEarthquakePollingService;
import com.afet.koordinasyon.service.EarthquakeEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/earthquakes")
@RequiredArgsConstructor
@Tag(name = "Earthquakes", description = "AFAD gerçek deprem verileri")
@SecurityRequirement(name = "bearerAuth")
public class EarthquakeController {

    private final EarthquakeEventService earthquakeEventService;
    private final AfadEarthquakePollingService pollingService;

    @GetMapping
    @Operation(summary = "Tüm depremleri sayfalı listele")
    public ResponseEntity<PagedResponse<EarthquakeEventResponse>> listEarthquakes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(earthquakeEventService.listEarthquakes(page, size));
    }

    @GetMapping("/latest")
    @Operation(summary = "Son 10 depremi getir")
    public ResponseEntity<List<EarthquakeEventResponse>> getLatest() {
        return ResponseEntity.ok(earthquakeEventService.getLatest());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Deprem detayını getir")
    public ResponseEntity<EarthquakeEventResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(earthquakeEventService.getById(id));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "AFAD'dan manuel veri senkronizasyonu (Sadece Admin)")
    public ResponseEntity<EarthquakeSyncResponse> sync(
            @RequestParam(defaultValue = "24") int hoursBefore) {
        return ResponseEntity.ok(earthquakeEventService.syncFromAfad(hoursBefore));
    }

    @GetMapping("/debug/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "AFAD sistem durum raporu (Sadece Admin)")
    public ResponseEntity<EarthquakeDebugStatusResponse> debugStatus() {
        EarthquakeDebugStatusResponse status = EarthquakeDebugStatusResponse.builder()
                .newestDbEvent(earthquakeEventService.getNewestEvent().orElse(null))
                .pollingIntervalMs(pollingService.getPollingIntervalMs())
                .startupHours(pollingService.getStartupHours())
                .pollingHours(pollingService.getPollingHours())
                .emailNotificationsEnabled(pollingService.isEmailNotificationsEnabled())
                .emailMinMagnitude(pollingService.getEmailMinMagnitude())
                .lastSyncStartedAt(pollingService.getLastSyncStartedAt())
                .lastSyncCompletedAt(pollingService.getLastSyncCompletedAt())
                .lastSyncFetchedCount(pollingService.getLastSyncFetchedCount())
                .lastSyncSavedCount(pollingService.getLastSyncSavedCount())
                .serverTimezone(ZoneId.systemDefault().getId())
                .build();
        return ResponseEntity.ok(status);
    }
}
