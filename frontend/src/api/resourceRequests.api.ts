import axiosInstance from './axiosInstance';
import {
    ResourceRequestResponse,
    CreateResourceRequestRequest,
    UpdateResourceStatusRequest,
    ResourceSummaryResponse,
    PagedResponse,
} from '@/types';

export const getResourceRequests = async (params?: {
    page?: number;
    size?: number;
    districtId?: string;
    neighborhoodId?: string;
    status?: string;
}): Promise<PagedResponse<ResourceRequestResponse>> => {
    const res = await axiosInstance.get<PagedResponse<ResourceRequestResponse>>('/resource-requests', { params });
    return res.data;
};

export const getResourceRequestById = async (id: string): Promise<ResourceRequestResponse> => {
    const res = await axiosInstance.get<ResourceRequestResponse>(`/resource-requests/${id}`);
    return res.data;
};

export const createResourceRequest = async (data: CreateResourceRequestRequest): Promise<ResourceRequestResponse> => {
    const res = await axiosInstance.post<ResourceRequestResponse>('/resource-requests', data);
    return res.data;
};

export const updateResourceRequestStatus = async (id: string, data: UpdateResourceStatusRequest): Promise<ResourceRequestResponse> => {
    const res = await axiosInstance.patch<ResourceRequestResponse>(`/resource-requests/${id}/status`, data);
    return res.data;
};

export const getMapResourceSummary = async (): Promise<ResourceSummaryResponse[]> => {
    const res = await axiosInstance.get<ResourceSummaryResponse[]>('/map/resource-summary');
    return res.data;
};
