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

@Service
@RequiredArgsConstructor
@Slf4j
public class AfadEarthquakePollingService {

    private final EarthquakeEventService earthquakeEventService;

    @Value("${app.afad.startup-hours:48}")
    private int startupHours;

    @Value("${app.afad.polling-hours:24}")
    private int pollingHours;

    @Value("${app.afad.polling-interval-ms:30000}")
    private long pollingIntervalMs;

    @Value("${app.afad.email-notifications-enabled:true}")
    private boolean emailNotificationsEnabled;

    @Value("${app.afad.email-min-magnitude:0.0}")
    private double emailMinMagnitude;

    // Durum bilgisi — debug endpoint için
    @Getter private volatile OffsetDateTime lastSyncStartedAt;
    @Getter private volatile OffsetDateTime lastSyncCompletedAt;
    @Getter private volatile int lastSyncFetchedCount;
    @Getter private volatile int lastSyncSavedCount;

    public long getPollingIntervalMs()        { return pollingIntervalMs; }
    public int  getStartupHours()             { return startupHours; }
    public int  getPollingHours()             { return pollingHours; }
    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public double getEmailMinMagnitude()      { return emailMinMagnitude; }

    @Scheduled(fixedRateString = "${app.afad.polling-interval-ms:30000}")
    public void pollAfadData() {
        log.info("=== AFAD sync başladı (polling, son {}s) ===", pollingHours);
        lastSyncStartedAt = OffsetDateTime.now();
        try {
            EarthquakeSyncResponse result = earthquakeEventService.syncFromAfad(pollingHours);
            lastSyncCompletedAt = OffsetDateTime.now();
            lastSyncFetchedCount = result.getFetchedCount();
            lastSyncSavedCount   = result.getSavedCount();
            log.info("=== AFAD sync bitti: {} ===", result.getMessage());
        } catch (Exception e) {
            log.error("AFAD polling sırasında beklenmedik hata: {}", e.getMessage());
        }
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initialLoad() {
        log.info("=== AFAD ilk yükleme başladı (son {}s) ===", startupHours);
        lastSyncStartedAt = OffsetDateTime.now();
        try {
            EarthquakeSyncResponse result = earthquakeEventService.syncFromAfad(startupHours);
            lastSyncCompletedAt = OffsetDateTime.now();
            lastSyncFetchedCount = result.getFetchedCount();
            lastSyncSavedCount   = result.getSavedCount();
            log.info("=== AFAD ilk yükleme bitti: {} ===", result.getMessage());
        } catch (Exception e) {
            log.error("AFAD ilk yükleme sırasında hata: {}", e.getMessage());
        }
    }
}
