/** 3B bina altyapısı — backend `BuildingController`/`BuildingService`/`AdminBuildingController`
 *  DTO karşılıkları. Desteklenen ilçeler `Building3dConfigResponse.enabledDistricts`'ten gelir. */

export interface Building3dConfigResponse {
    enabledDistricts: string[];
    buildingMinZoom: number;
    extrusionMinZoom: number;
    /** 0 ise veri seti hiç import edilmemiş demektir — "bu viewport'ta bina yok" ile ayrı bir durum. */
    totalBuildingCount: number;
}

/** GET /api/buildings FeatureCollection'ındaki her Feature'ın `properties` gövdesi. */
export interface BuildingSummaryResponse {
    id: string;
    source: string;
    buildingType: string | null;
    name: string | null;
    streetName: string | null;
    buildingNumber: string | null;
    heightM: number | null;
    levels: number | null;
    estimatedHeightM: number;
    isEstimatedHeight: boolean;
    heightSource: 'osm_height' | 'levels_x3' | 'fallback';
    /** Binaya bağlı hiç hasar kaydı yoksa null (rapor yok ≠ UNASSESSED). */
    damageStatus: string | null;
    damageStatusLabel: string | null;
    damageAssessmentCount: number;
}

export interface BuildingFeature {
    type: 'Feature';
    id: string;
    geometry: GeoJSON.Geometry;
    properties: BuildingSummaryResponse;
}

export interface BuildingFeatureCollection {
    type: 'FeatureCollection';
    features: BuildingFeature[];
}

export interface BuildingLatestAssessmentSummary {
    id: string;
    damageLevel: string;
    damageLevelLabel: string;
    verificationStatus: string;
    verificationStatusLabel: string;
    createdAt: string;
    photoUrls: string[];
}

export interface BuildingDetailResponse {
    id: string;
    source: string;
    districtId: string;
    districtName: string;
    neighborhoodId: string;
    neighborhoodName: string;
    streetName: string | null;
    buildingNumber: string | null;
    displayAddress: string | null;
    latitude: number;
    longitude: number;
    geometry: GeoJSON.Geometry;
    heightM: number | null;
    levels: number | null;
    estimatedHeightM: number;
    isEstimatedHeight: boolean;
    heightSource: string;
    buildingType: string | null;
    name: string | null;
    damageStatus: string | null;
    damageStatusLabel: string | null;
    damageAssessmentCount: number;
    verifiedAssessmentCount: number;
    latestAssessment: BuildingLatestAssessmentSummary | null;
}

export interface BuildingSearchResultResponse {
    id: string;
    displayAddress: string | null;
    streetName: string | null;
    buildingNumber: string | null;
    neighborhoodName: string;
    latitude: number;
    longitude: number;
}

export interface BuildingImportResult {
    districtId: string;
    districtName: string;
    imported: number;
    updated: number;
    skippedNoNeighborhood: number;
    skippedInvalidRecord: number;
    unmatchedNeighborhoods: string[];
}

/** GET /api/admin/buildings/import-status — item 5. */
export interface BuildingImportStatusResponse {
    districtId: string;
    districtName: string;
    source: string | null;
    buildingCount: number;
    lastImportAt: string | null;
    status: 'HAS_DATA' | 'NO_DATA';
}

/** GET /api/admin/buildings/neighborhood-coverage?districtId= — item 7. */
export interface NeighborhoodCoverageResponse {
    neighborhoodId: string;
    neighborhoodName: string;
    buildingCount: number;
}
