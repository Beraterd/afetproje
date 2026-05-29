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
}
