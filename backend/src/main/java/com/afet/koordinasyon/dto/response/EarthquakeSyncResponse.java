package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EarthquakeSyncResponse {
    private int newEventsCount;          // backward compat alias for savedCount
    private int fetchedCount;
    private int savedCount;
    private int skippedDuplicateCount;
    private String newestAfadEventTime;  // Ham AFAD tarihi (TSI string)
    private String newestDbEventTime;    // DB'deki en yeni event_time (ISO)
    private long durationMs;
    private String message;
    private List<String> latestAfadExternalIds;  // AFAD'dan gelen ilk 10 EventID
    private List<String> latestDbExternalIds;    // DB'deki ilk 10 EventID
}
