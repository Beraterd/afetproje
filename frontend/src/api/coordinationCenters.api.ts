import axiosInstance from './axiosInstance';
import {
    CoordinationCenterResponse,
    CoordinationCenterStatusResponse,
    UpsertCoordinationCenterRequest,
} from '@/types/coordinationCenter';

export const getMyCoordinationStatus = async (): Promise<CoordinationCenterStatusResponse> => {
    const res = await axiosInstance.get<CoordinationCenterStatusResponse>('/coordination-centers/my-status');
    return res.data;
};

export const getDistrictCenter = async (districtId: string): Promise<CoordinationCenterResponse> => {
    const res = await axiosInstance.get<CoordinationCenterResponse>(`/coordination-centers/districts/${districtId}`);
    return res.data;
};

export const upsertDistrictCenter = async (
    districtId: string,
    data: UpsertCoordinationCenterRequest
): Promise<CoordinationCenterResponse> => {
    const res = await axiosInstance.put<CoordinationCenterResponse>(
        `/coordination-centers/districts/${districtId}`,
        data
    );
    return res.data;
};

export const getNeighborhoodCenter = async (neighborhoodId: string): Promise<CoordinationCenterResponse> => {
    const res = await axiosInstance.get<CoordinationCenterResponse>(
        `/coordination-centers/neighborhoods/${neighborhoodId}`
    );
    return res.data;
};

export const upsertNeighborhoodCenter = async (
    neighborhoodId: string,
    data: UpsertCoordinationCenterRequest
): Promise<CoordinationCenterResponse> => {
    const res = await axiosInstance.put<CoordinationCenterResponse>(
        `/coordination-centers/neighborhoods/${neighborhoodId}`,
        data
    );
    return res.data;
};
