/** Sokak/cadde arama altyapısı — backend `StreetController`/`StreetService` DTO karşılıkları. */

export interface StreetBounds {
    minLon: number;
    minLat: number;
    maxLon: number;
    maxLat: number;
}

export interface StreetCenter {
    lon: number;
    lat: number;
}

/** GET /api/streets/search sonuç satırı — autocomplete listesi, geometri İÇERMEZ. */
export interface StreetSearchResultResponse {
    id: string;
    name: string;
    districtId: string;
    districtName: string;
    neighborhoodId: string;
    neighborhoodName: string;
    roadType: string | null;
    bounds: StreetBounds;
    center: StreetCenter;
}

/** GET /api/streets/{id} — seçim sonrası harita highlight'ı için tam geometri döner. */
export interface StreetDetailResponse {
    id: string;
    name: string;
    districtId: string;
    districtName: string;
    neighborhoodId: string;
    neighborhoodName: string;
    roadType: string | null;
    geometry: GeoJSON.Geometry;
    bounds: StreetBounds;
    center: StreetCenter;
}
