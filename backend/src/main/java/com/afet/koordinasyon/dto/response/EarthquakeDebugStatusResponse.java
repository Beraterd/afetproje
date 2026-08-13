package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class EarthquakeDebugStatusResponse {
    private EarthquakeEventResponse newestDbEvent;
    private long pollingIntervalMs;
    private int startupHours;
    private int pollingHours;
    private boolean emailNotificationsEnabled;
    private double emailMinMagnitude;
    private OffsetDateTime lastSyncStartedAt;
    private OffsetDateTime lastSyncCompletedAt;
    private int lastSyncFetchedCount;
    private int lastSyncSavedCount;
    private String serverTimezone;

    // ── Kandilli (AFAD'dan bağımsız) ─────────────────────────────────────────
    private long kandilliPollingIntervalMs;
    private OffsetDateTime kandilliLastSyncStartedAt;
    private OffsetDateTime kandilliLastSyncCompletedAt;
    private int kandilliLastSyncFetchedCount;
    private int kandilliLastSyncSavedCount;
    private String kandilliLastError;
}
