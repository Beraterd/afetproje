import axiosInstance from './axiosInstance';
import { EarthquakeEventResponse, EarthquakeSyncResponse, EarthquakeFilterParams, EarthquakeDebugStatusResponse } from '@/types';
import { PagedResponse } from '@/types';

export const getEarthquakes = async (
    params?: EarthquakeFilterParams
): Promise<PagedResponse<EarthquakeEventResponse>> => {
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

/** Kanonik 81 il listesi — İl filtresi dropdown'u için tek kaynak (backend TurkishProvinces). */
export const getEarthquakeProvinces = async (): Promise<string[]> => {
    const res = await axiosInstance.get<string[]>('/earthquakes/provinces');
    return res.data;
};

/** Seçilen ilde GERÇEKTEN kayıtlı deprem ilçeleri — statik/uydurma liste değildir. */
export const getEarthquakeDistricts = async (province: string): Promise<string[]> => {
    const res = await axiosInstance.get<string[]>('/earthquakes/districts', { params: { province } });
    return res.data;
};

/** Yalnızca ADMIN — mevcut sync durumu (AFAD + Kandilli, ikisi bağımsız). */
export const getEarthquakeDebugStatus = async (): Promise<EarthquakeDebugStatusResponse> => {
    const res = await axiosInstance.get<EarthquakeDebugStatusResponse>('/earthquakes/debug/status');
    return res.data;
};

export const syncEarthquakes = async (hoursBefore = 24): Promise<EarthquakeSyncResponse> => {
    const res = await axiosInstance.post<EarthquakeSyncResponse>('/earthquakes/sync', null, {
        params: { hoursBefore },
    });
    return res.data;
};

export const syncKandilliEarthquakes = async (hoursBefore = 24): Promise<EarthquakeSyncResponse> => {
    const res = await axiosInstance.post<EarthquakeSyncResponse>('/earthquakes/sync/kandilli', null, {
        params: { hoursBefore },
    });
    return res.data;
};
