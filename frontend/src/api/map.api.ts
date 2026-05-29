import axiosInstance from './axiosInstance';
import { MapDistrictResponse, MapNeighborhoodResponse, DistrictCoordinatorMapResponse, NeighborhoodCoordinatorMapResponse } from '@/types';
import { DamagePointResponse } from '@/types/damage';

export const getMapDistricts = async (): Promise<MapDistrictResponse[]> => {
    const res = await axiosInstance.get<MapDistrictResponse[]>('/map/districts');
    return res.data;
};

export const getMapNeighborhoods = async (districtId: string): Promise<MapNeighborhoodResponse[]> => {
    const res = await axiosInstance.get<MapNeighborhoodResponse[]>(`/map/districts/${districtId}/neighborhoods`);
    return res.data;
};

export const getMapDamagePoints = async (
    districtId?: string,
    neighborhoodId?: string
): Promise<DamagePointResponse[]> => {
    const params: Record<string, string> = {};
    if (neighborhoodId) params.neighborhoodId = neighborhoodId;
    else if (districtId) params.districtId = districtId;
    const res = await axiosInstance.get<DamagePointResponse[]>('/map/damage-points', {
        params: Object.keys(params).length > 0 ? params : undefined,
    });
    return res.data;
};

export const getDistrictCentersForMap = async (): Promise<DistrictCoordinatorMapResponse[]> => {
    const res = await axiosInstance.get<DistrictCoordinatorMapResponse[]>('/map/district-coordinators');
    return res.data;
};

export const getNeighborhoodCentersForMap = async (districtId?: string): Promise<NeighborhoodCoordinatorMapResponse[]> => {
    const res = await axiosInstance.get<NeighborhoodCoordinatorMapResponse[]>('/map/neighborhood-coordinators', {
        params: districtId ? { districtId } : undefined,
    });
    return res.data;
};
