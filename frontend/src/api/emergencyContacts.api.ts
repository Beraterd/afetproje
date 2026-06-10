import axiosInstance from './axiosInstance';
import {
    EmergencyContactResponse,
    AddEmergencyContactRequest,
    UserSearchResponse,
    MyAssemblyArea,
} from '@/types';

export const getMyEmergencyContacts = async (): Promise<EmergencyContactResponse[]> => {
    const res = await axiosInstance.get<EmergencyContactResponse[]>('/users/me/emergency-contacts');
    return res.data;
};

export const addEmergencyContact = async (data: AddEmergencyContactRequest): Promise<EmergencyContactResponse> => {
    const res = await axiosInstance.post<EmergencyContactResponse>('/users/me/emergency-contacts', data);
    return res.data;
};

export const removeEmergencyContact = async (contactUserId: string): Promise<void> => {
    await axiosInstance.delete(`/users/me/emergency-contacts/${contactUserId}`);
};

export const searchUsers = async (query: string): Promise<UserSearchResponse[]> => {
    const res = await axiosInstance.get<UserSearchResponse[]>('/users/search', { params: { query } });
    return res.data;
};

export const getMyAssemblyAreas = async (): Promise<MyAssemblyArea[]> => {
    const res = await axiosInstance.get<MyAssemblyArea[]>('/users/me/assembly-areas');
    return res.data;
};
