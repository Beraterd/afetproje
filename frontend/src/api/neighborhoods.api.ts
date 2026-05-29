import axiosInstance from './axiosInstance';
import { NeighborhoodResponse, NeighborhoodSummaryResponse } from '@/types';

const ensureArrayResponse = <T>(data: unknown, endpoint: string): T[] => {
    if (!Array.isArray(data)) {
        throw new Error(`Unexpected API response from ${endpoint}`);
    }
    return data as T[];
};

export const getNeighborhoods = async (districtId: string): Promise<NeighborhoodSummaryResponse[]> => {
    const endpoint = `/districts/${districtId}/neighborhoods`;
    const res = await axiosInstance.get<unknown>(endpoint);
    return ensureArrayResponse<NeighborhoodSummaryResponse>(res.data, endpoint);
};

export const getNeighborhoodById = async (districtId: string, id: string): Promise<NeighborhoodResponse> => {
    const res = await axiosInstance.get<NeighborhoodResponse>(`/districts/${districtId}/neighborhoods/${id}`);
    return res.data;
};
