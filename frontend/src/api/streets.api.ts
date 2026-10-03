import axiosInstance from './axiosInstance';
import { StreetDetailResponse, StreetSearchResultResponse } from '@/types/street';

export const searchStreets = async (
    q: string,
    districtId: string,
    neighborhoodId: string,
    limit?: number,
    signal?: AbortSignal
): Promise<StreetSearchResultResponse[]> => {
    const res = await axiosInstance.get<StreetSearchResultResponse[]>('/streets/search', {
        params: { q, districtId, neighborhoodId, limit },
        signal,
    });
    return res.data;
};

export const getStreetById = async (id: string): Promise<StreetDetailResponse> => {
    const res = await axiosInstance.get<StreetDetailResponse>(`/streets/${id}`);
    return res.data;
};
