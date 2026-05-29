export type EarthquakeRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface EarthquakeEventResponse {
    id: string;
    externalId: string;
    eventTime: string;
    latitude: number;
    longitude: number;
    depth?: number;
    magnitude: number;
    location?: string;
    province?: string;
    district?: string;
    source: string;
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
