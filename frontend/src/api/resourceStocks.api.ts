import axiosInstance from './axiosInstance';
import {
    ResourceStockResponse,
    ResourceStockSummaryResponse,
    ResourceStockMovementResponse,
    StockLookupResponse,
    CreateResourceStockRequest,
    UpdateStockQuantityRequest,
} from '@/types';

export interface StockFilters {
    districtId?: string;
    neighborhoodId?: string;
    category?: string;
    status?: string;
    search?: string;
    criticalOnly?: boolean;
}

export const getResourceStocks = async (filters: StockFilters = {}): Promise<ResourceStockResponse[]> => {
    const res = await axiosInstance.get<ResourceStockResponse[]>('/resource-stocks', { params: filters });
    return res.data;
};

export const getStockSummary = async (params?: { districtId?: string; neighborhoodId?: string }):
    Promise<ResourceStockSummaryResponse> => {
    const res = await axiosInstance.get<ResourceStockSummaryResponse>('/resource-stocks/summary', { params });
    return res.data;
};

export const lookupStock = async (params: {
    districtId: string;
    neighborhoodId?: string;
    resourceType: string;
    quantity?: number;
}): Promise<StockLookupResponse> => {
    const res = await axiosInstance.get<StockLookupResponse>('/resource-stocks/lookup', { params });
    return res.data;
};

export const getStockMovements = async (id: string): Promise<ResourceStockMovementResponse[]> => {
    const res = await axiosInstance.get<ResourceStockMovementResponse[]>(`/resource-stocks/${id}/movements`);
    return res.data;
};

export const createResourceStock = async (data: CreateResourceStockRequest): Promise<ResourceStockResponse> => {
    const res = await axiosInstance.post<ResourceStockResponse>('/resource-stocks', data);
    return res.data;
};

export const updateStockQuantity = async (id: string, data: UpdateStockQuantityRequest):
    Promise<ResourceStockResponse> => {
    const res = await axiosInstance.patch<ResourceStockResponse>(`/resource-stocks/${id}/quantity`, data);
    return res.data;
};

export const deactivateStock = async (id: string): Promise<void> => {
    await axiosInstance.patch(`/resource-stocks/${id}/deactivate`);
};
