package com.afet.koordinasyon.service;

import com.afet.koordinasyon.dto.response.EarthquakeSyncResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Kandilli için AFAD'dan TAMAMEN bağımsız scheduled sync (item 15/16).
 * <p>
 * Ayrı bir Spring {@code @Scheduled} metodu olduğu için Kandilli'de oluşan bir exception
 * AFAD'ın kendi scheduled task'ını ETKİLEMEZ (Spring her {@code @Scheduled} metodunu
 * bağımsız bir görev olarak zamanlar) — burada try/catch ayrıca AFAD'daki gibi temiz log
 * ve durum takibi (lastSync*) için kullanılır, izolasyon için zorunlu değildir ama
 * tutarlılık ve gözlemlenebilirlik için AYNI desen izlenmiştir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KandilliEarthquakePollingService {

    private final EarthquakeEventService earthquakeEventService;

    @Value("${app.kandilli.startup-hours:48}")
    private int startupHours;

    @Value("${app.kandilli.polling-hours:24}")
    private int pollingHours;

    @Value("${app.kandilli.polling-interval-ms:60000}")
    private long pollingIntervalMs;

    @Getter private volatile OffsetDateTime lastSyncStartedAt;
    @Getter private volatile OffsetDateTime lastSyncCompletedAt;
    @Getter private volatile int lastSyncFetchedCount;
    @Getter private volatile int lastSyncSavedCount;
    @Getter private volatile String lastError;

    public long getPollingIntervalMs() { return pollingIntervalMs; }
    public int getStartupHours()       { return startupHours; }
    public int getPollingHours()       { return pollingHours; }

    @Scheduled(fixedRateString = "${app.kandilli.polling-interval-ms:60000}")
    public void pollKandilliData() {
        log.info("=== Kandilli sync başladı (polling, son {}s) ===", pollingHours);
        lastSyncStartedAt = OffsetDateTime.now();
        try {
            EarthquakeSyncResponse result = earthquakeEventService.syncFromKandilli(pollingHours);
            lastSyncCompletedAt = OffsetDateTime.now();
            lastSyncFetchedCount = result.getFetchedCount();
            lastSyncSavedCount   = result.getSavedCount();
            lastError = null;
            log.info("=== Kandilli sync bitti: {} ===", result.getMessage());
        } catch (Exception e) {
            // Kandilli erişilemese/hata verse bile AFAD scheduler'ı ETKİLENMEZ — bağımsız task.
            lastError = e.getMessage();
            log.error("Kandilli polling sırasında beklenmedik hata (AFAD sync etkilenmedi): {}", e.getMessage());
        }
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initialLoad() {
        log.info("=== Kandilli ilk yükleme başladı (son {}s) ===", startupHours);
        lastSyncStartedAt = OffsetDateTime.now();
        try {
            EarthquakeSyncResponse result = earthquakeEventService.syncFromKandilli(startupHours);
            lastSyncCompletedAt = OffsetDateTime.now();
            lastSyncFetchedCount = result.getFetchedCount();
            lastSyncSavedCount   = result.getSavedCount();
            lastError = null;
            log.info("=== Kandilli ilk yükleme bitti: {} ===", result.getMessage());
        } catch (Exception e) {
            lastError = e.getMessage();
            log.error("Kandilli ilk yükleme sırasında hata: {}", e.getMessage());
        }
    }
}
