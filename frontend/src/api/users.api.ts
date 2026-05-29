import axiosInstance from './axiosInstance';
import { PagedResponse, UserResponse } from '@/types';

export const getMe = async (): Promise<UserResponse> => {
    const res = await axiosInstance.get<UserResponse>('/auth/me');
    return res.data;
};

export const updateMe = async (data: any): Promise<UserResponse> => {
    const res = await axiosInstance.put<UserResponse>('/users/me', data);
    return res.data;
};

export const changeEmail = async (data: any): Promise<void> => {
    await axiosInstance.post('/users/me/email', data);
};

export const changePassword = async (data: any): Promise<void> => {
    await axiosInstance.post('/users/me/password', data);
};

export const getUsers = async (params?: any): Promise<PagedResponse<UserResponse>> => {
    const res = await axiosInstance.get<PagedResponse<UserResponse>>('/admin/users', { params });
    return res.data;
};

export const assignRole = async (userId: string, data: any): Promise<UserResponse> => {
    const res = await axiosInstance.post<UserResponse>(`/admin/users/${userId}/role`, data);
    return res.data;
};

export const deleteUser = async (userId: string): Promise<void> => {
    await axiosInstance.delete(`/admin/users/${userId}`);
};

export interface AdminUpdateUserData {
    firstName?: string;
    lastName?: string;
    email?: string;
    districtId?: string;
    neighborhoodId?: string;
}

export const adminUpdateUser = async (userId: string, data: AdminUpdateUserData): Promise<UserResponse> => {
    const res = await axiosInstance.put<UserResponse>(`/admin/users/${userId}`, data);
    return res.data;
};

export interface PurgeRequest {
    purgeEvents: boolean;
    purgeDamageAssessments: boolean;
    purgeResourceRequests: boolean;
    purgeUsers: boolean;
    protectedEmails: string[];
    confirmationText: string;
}

export interface PurgeResponse {
    deletedEventVolunteers: number;
    deletedEvents: number;
    deletedDamagePhotos: number;
    deletedDamageAssessments: number;
    deletedResourceRequests: number;
    deletedUsers: number;
    warnings: string[];
}

export const purgeOperationalData = async (data: PurgeRequest): Promise<PurgeResponse> => {
    const res = await axiosInstance.post<PurgeResponse>('/admin/maintenance/purge-operational-data', data);
    return res.data;
};
