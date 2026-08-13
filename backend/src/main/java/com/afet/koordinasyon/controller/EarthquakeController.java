package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.enums.EarthquakeSource;
import com.afet.koordinasyon.dto.response.EarthquakeDebugStatusResponse;
import com.afet.koordinasyon.dto.response.EarthquakeEventResponse;
import com.afet.koordinasyon.dto.response.EarthquakeSyncResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.service.AfadEarthquakePollingService;
import com.afet.koordinasyon.service.EarthquakeEventService;
import com.afet.koordinasyon.service.KandilliEarthquakePollingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/earthquakes")
@RequiredArgsConstructor
@Tag(name = "Earthquakes", description = "AFAD ve Kandilli Rasathanesi deprem verileri")
@SecurityRequirement(name = "bearerAuth")
public class EarthquakeController {

    private final EarthquakeEventService earthquakeEventService;
    private final AfadEarthquakePollingService afadPollingService;
    private final KandilliEarthquakePollingService kandilliPollingService;

    @GetMapping
    @Operation(summary = "Depremleri sayfalı listele — opsiyonel kaynak/il/ilçe/büyüklük/tarih filtreleriyle")
    public ResponseEntity<PagedResponse<EarthquakeEventResponse>> listEarthquakes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) EarthquakeSource source,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Double minMagnitude,
            @RequestParam(required = false) OffsetDateTime startDate,
            @RequestParam(required = false) OffsetDateTime endDate) {
        var filter = new EarthquakeEventService.EarthquakeFilter(
                source, province, district, minMagnitude, startDate, endDate);
        return ResponseEntity.ok(earthquakeEventService.listEarthquakes(page, size, filter));
    }

    @GetMapping("/provinces")
    @Operation(summary = "İl filtresi için kanonik 81 il listesi")
    public ResponseEntity<List<String>> getProvinces() {
        return ResponseEntity.ok(earthquakeEventService.getProvinces());
    }

    @GetMapping("/districts")
    @Operation(summary = "Seçilen ilde GERÇEKTEN kayıtlı deprem ilçeleri (statik/uydurma liste değil)")
    public ResponseEntity<List<String>> getDistricts(@RequestParam String province) {
        return ResponseEntity.ok(earthquakeEventService.getDistrictsForProvince(province));
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

    @PostMapping("/sync/kandilli")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kandilli'den manuel veri senkronizasyonu (Sadece Admin)")
    public ResponseEntity<EarthquakeSyncResponse> syncKandilli(
            @RequestParam(defaultValue = "24") int hoursBefore) {
        return ResponseEntity.ok(earthquakeEventService.syncFromKandilli(hoursBefore));
    }

    @GetMapping("/debug/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "AFAD + Kandilli sistem durum raporu (Sadece Admin)")
    public ResponseEntity<EarthquakeDebugStatusResponse> debugStatus() {
        EarthquakeDebugStatusResponse status = EarthquakeDebugStatusResponse.builder()
                .newestDbEvent(earthquakeEventService.getNewestEvent().orElse(null))
                .pollingIntervalMs(afadPollingService.getPollingIntervalMs())
                .startupHours(afadPollingService.getStartupHours())
                .pollingHours(afadPollingService.getPollingHours())
                .emailNotificationsEnabled(afadPollingService.isEmailNotificationsEnabled())
                .emailMinMagnitude(afadPollingService.getEmailMinMagnitude())
                .lastSyncStartedAt(afadPollingService.getLastSyncStartedAt())
                .lastSyncCompletedAt(afadPollingService.getLastSyncCompletedAt())
                .lastSyncFetchedCount(afadPollingService.getLastSyncFetchedCount())
                .lastSyncSavedCount(afadPollingService.getLastSyncSavedCount())
                .serverTimezone(ZoneId.systemDefault().getId())
                .kandilliPollingIntervalMs(kandilliPollingService.getPollingIntervalMs())
                .kandilliLastSyncStartedAt(kandilliPollingService.getLastSyncStartedAt())
                .kandilliLastSyncCompletedAt(kandilliPollingService.getLastSyncCompletedAt())
                .kandilliLastSyncFetchedCount(kandilliPollingService.getLastSyncFetchedCount())
                .kandilliLastSyncSavedCount(kandilliPollingService.getLastSyncSavedCount())
                .kandilliLastError(kandilliPollingService.getLastError())
                .build();
        return ResponseEntity.ok(status);
    }
}
