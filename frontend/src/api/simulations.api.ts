import axiosInstance from './axiosInstance';
import {
    PagedResponse,
    SimulationCreatedResponse,
    SimulationDetailResponse,
    SimulationLogResponse,
} from '@/types';

export const createSimulation = async (data: any): Promise<SimulationCreatedResponse> => {
    const res = await axiosInstance.post<SimulationCreatedResponse>('/admin/simulations', data);
    return res.data;
};

export const getSimulations = async (params?: any): Promise<PagedResponse<SimulationDetailResponse>> => {
    const res = await axiosInstance.get<PagedResponse<SimulationDetailResponse>>('/admin/simulations', { params });
    return res.data;
};

export const getSimulationById = async (id: string): Promise<SimulationDetailResponse> => {
    const res = await axiosInstance.get<SimulationDetailResponse>(`/admin/simulations/${id}`);
    return res.data;
};

export const getSimulationLogs = async (id: string, params?: any): Promise<PagedResponse<SimulationLogResponse>> => {
    const res = await axiosInstance.get<PagedResponse<SimulationLogResponse>>(`/admin/simulations/${id}/logs`, { params });
    return res.data;
};
