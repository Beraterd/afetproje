import axiosInstance from './axiosInstance';
import { EarthquakeEventResponse, EarthquakeSyncResponse } from '@/types';
import { PagedResponse } from '@/types';

export const getEarthquakes = async (params?: {
    page?: number;
    size?: number;
}): Promise<PagedResponse<EarthquakeEventResponse>> => {
    const res = await axiosInstance.get<PagedResponse<EarthquakeEventResponse>>('/earthquakes', { params });
    return res.data;
};

export const getLatestEarthquakes = async (): Promise<EarthquakeEventResponse[]> => {
    const res = await axiosInstance.get<EarthquakeEventResponse[]>('/earthquakes/latest');
    return res.data;
};

export const getEarthquakeById = async (id: string): Promise<EarthquakeEventResponse> => {
    const res = await axiosInstance.get<EarthquakeEventResponse>(`/earthquakes/${id}`);
    return res.data;
};

export const syncEarthquakes = async (hoursBefore = 24): Promise<EarthquakeSyncResponse> => {
    const res = await axiosInstance.post<EarthquakeSyncResponse>('/earthquakes/sync', null, {
        params: { hoursBefore },
    });
    return res.data;
};
