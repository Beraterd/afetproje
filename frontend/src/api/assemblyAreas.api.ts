import axiosInstance from './axiosInstance';
import {
    AdminAssemblyAreaResponse,
    AssemblyAreaStatsResponse,
    PagedResponse,
} from '@/types';

export interface AssemblyAreaFilters {
    districtId?: string;
    neighborhoodId?: string;
    sourceName?: string;
    needsReview?: boolean;
    active?: boolean;
    search?: string;
    page?: number;
    size?: number;
}

export interface UpdateAssemblyAreaRequest {
    name?: string;
    address?: string;
    latitude?: number | null;
    longitude?: number | null;
    googleMapsUrl?: string | null;
    needsReview?: boolean;
    active?: boolean;
}

export interface BulkUpdateRequest {
    ids: string[];
    needsReview?: boolean;
    active?: boolean;
}

export const getAssemblyAreasForReview = async (
    filters: AssemblyAreaFilters = {}
): Promise<PagedResponse<AdminAssemblyAreaResponse>> => {
    const res = await axiosInstance.get<PagedResponse<AdminAssemblyAreaResponse>>(
        '/admin/assembly-areas',
        { params: filters }
    );
    return res.data;
};

export const getAssemblyAreaStats = async (): Promise<AssemblyAreaStatsResponse> => {
    const res = await axiosInstance.get<AssemblyAreaStatsResponse>('/admin/assembly-areas/stats');
    return res.data;
};

export const getAssemblyAreaById = async (id: string): Promise<AdminAssemblyAreaResponse> => {
    const res = await axiosInstance.get<AdminAssemblyAreaResponse>(`/admin/assembly-areas/${id}`);
    return res.data;
};

export const updateAssemblyArea = async (
    id: string,
    data: UpdateAssemblyAreaRequest
): Promise<AdminAssemblyAreaResponse> => {
    const res = await axiosInstance.patch<AdminAssemblyAreaResponse>(
        `/admin/assembly-areas/${id}`,
        data
    );
    return res.data;
};

export const bulkUpdateAssemblyAreas = async (
    data: BulkUpdateRequest
): Promise<{ updated: number }> => {
    const res = await axiosInstance.post<{ updated: number }>(
        '/admin/assembly-areas/bulk',
        data
    );
    return res.data;
};
