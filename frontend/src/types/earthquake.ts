export type EarthquakeRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/** Deprem verisinin geldiği kurum — backend'deki EarthquakeSource enum'uyla birebir. */
export type EarthquakeSource = 'AFAD' | 'KANDILLI';

export interface EarthquakeEventResponse {
    id: string;
    externalId: string;
    eventTime: string;
    latitude: number;
    longitude: number;
    depth?: number;
    magnitude: number;
    /** ML/Mw/MD vb. — sağlayıcı vermiyorsa yok. */
    magnitudeType?: string;
    location?: string;
    province?: string;
    district?: string;
    source: EarthquakeSource;
    riskLevel: EarthquakeRiskLevel;
    createdAt: string;
}

export interface EarthquakeSyncResponse {
    newEventsCount: number;
    fetchedCount: number;
    savedCount: number;
    skippedDuplicateCount: number;
    newestAfadEventTime?: string;
    newestDbEventTime?: string;
    durationMs: number;
    message: string;
    latestAfadExternalIds?: string[];
    latestDbExternalIds?: string[];
}

/** Yalnızca ADMIN — GET /earthquakes/debug/status. */
export interface EarthquakeDebugStatusResponse {
    newestDbEvent?: EarthquakeEventResponse;
    pollingIntervalMs: number;
    startupHours: number;
    pollingHours: number;
    emailNotificationsEnabled: boolean;
    emailMinMagnitude: number;
    lastSyncStartedAt?: string;
    lastSyncCompletedAt?: string;
    lastSyncFetchedCount: number;
    lastSyncSavedCount: number;
    serverTimezone: string;
    kandilliPollingIntervalMs: number;
    kandilliLastSyncStartedAt?: string;
    kandilliLastSyncCompletedAt?: string;
    kandilliLastSyncFetchedCount: number;
    kandilliLastSyncSavedCount: number;
    kandilliLastError?: string;
}

/** GET /earthquakes filtre parametreleri — hepsi opsiyonel, backend'de uygulanır. */
export interface EarthquakeFilterParams {
    page?: number;
    size?: number;
    source?: EarthquakeSource;
    province?: string;
    district?: string;
    minMagnitude?: number;
    startDate?: string;
    endDate?: string;
}
