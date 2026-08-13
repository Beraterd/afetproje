import axiosInstance from './axiosInstance';
import {
    Building3dConfigResponse,
    BuildingDetailResponse,
    BuildingFeatureCollection,
    BuildingImportResult,
    BuildingImportStatusResponse,
    BuildingSearchResultResponse,
    NeighborhoodCoverageResponse,
} from '@/types/building';

export const getBuilding3dConfig = async (): Promise<Building3dConfigResponse> => {
    const res = await axiosInstance.get<Building3dConfigResponse>('/buildings/config');
    return res.data;
};

export interface BuildingsBboxParams {
    districtId?: string;
    neighborhoodId?: string;
    /** minLon,minLat,maxLon,maxLat */
    bbox: string;
    zoom?: number;
}

export const getBuildings = async (params: BuildingsBboxParams, signal?: AbortSignal): Promise<BuildingFeatureCollection> => {
    const res = await axiosInstance.get<BuildingFeatureCollection>('/buildings', { params, signal });
    return res.data;
};

export const getBuildingById = async (id: string): Promise<BuildingDetailResponse> => {
    const res = await axiosInstance.get<BuildingDetailResponse>(`/buildings/${id}`);
    return res.data;
};

export const searchBuildings = async (
    q: string,
    districtId: string,
    limit?: number
): Promise<BuildingSearchResultResponse[]> => {
    const res = await axiosInstance.get<BuildingSearchResultResponse[]>('/buildings/search', {
        params: { q, districtId, limit },
    });
    return res.data;
};

/** Yalnızca ADMIN — repo'ya gömülü bir ilçenin OSM bina veri setini idempotent şekilde içe aktarır. */
export const importDistrictBuildings = async (districtId: string): Promise<BuildingImportResult> => {
    const res = await axiosInstance.post<BuildingImportResult>(`/admin/buildings/import/${districtId}`);
    return res.data;
};

/** Yalnızca ADMIN — ilçe başına bina import durumu (bina sayısı, son import zamanı, kaynak). */
export const getBuildingImportStatus = async (): Promise<BuildingImportStatusResponse[]> => {
    const res = await axiosInstance.get<BuildingImportStatusResponse[]>('/admin/buildings/import-status');
    return res.data;
};

/** Yalnızca ADMIN — bir ilçenin mahalle bazlı bina kapsama raporu (0 bina çıkan mahalleler dahil). */
export const getNeighborhoodBuildingCoverage = async (districtId: string): Promise<NeighborhoodCoverageResponse[]> => {
    const res = await axiosInstance.get<NeighborhoodCoverageResponse[]>('/admin/buildings/neighborhood-coverage', {
        params: { districtId },
    });
    return res.data;
};
