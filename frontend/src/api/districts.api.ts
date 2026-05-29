import axiosInstance from './axiosInstance';
import { DistrictResponse, NeighborhoodSummaryResponse } from '@/types';

const ensureArrayResponse = <T>(data: unknown, endpoint: string): T[] => {
    if (!Array.isArray(data)) {
        throw new Error(`Unexpected API response from ${endpoint}`);
    }
    return data as T[];
};

export const getDistricts = async (): Promise<DistrictResponse[]> => {
    const res = await axiosInstance.get<unknown>('/districts');
    return ensureArrayResponse<DistrictResponse>(res.data, '/districts');
};

export const getDistrictById = async (id: string): Promise<DistrictResponse> => {
    const res = await axiosInstance.get<DistrictResponse>(`/districts/${id}`);
    return res.data;
};

export const getNeighborhoodsByDistrict = async (districtId: string): Promise<NeighborhoodSummaryResponse[]> => {
    const endpoint = `/districts/${districtId}/neighborhoods`;
    const res = await axiosInstance.get<unknown>(endpoint);
    return ensureArrayResponse<NeighborhoodSummaryResponse>(res.data, endpoint);
};
