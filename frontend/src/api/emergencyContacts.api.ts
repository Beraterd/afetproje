import axiosInstance from './axiosInstance';
import {
    EmergencyContactResponse,
    AddEmergencyContactRequest,
    UserSearchResponse,
    StatusMessageResponse,
    StatusTemplateItem,
    SendStatusMessageRequest,
    ActiveSimulationResponse,
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

export const sendStatusMessage = async (data: SendStatusMessageRequest): Promise<StatusMessageResponse> => {
    const res = await axiosInstance.post<StatusMessageResponse>('/users/me/emergency-status-messages/send', data);
    return res.data;
};

export const getMyStatusMessages = async (simulationId?: string): Promise<StatusMessageResponse[]> => {
    const res = await axiosInstance.get<StatusMessageResponse[]>('/users/me/emergency-status-messages', {
        params: simulationId ? { simulationId } : undefined,
    });
    return res.data;
};

export const getStatusMessageTemplates = async (): Promise<StatusTemplateItem[]> => {
    const res = await axiosInstance.get<StatusTemplateItem[]>('/users/me/emergency-status-messages/templates');
    return res.data;
};

export const getActiveSimulation = async (): Promise<ActiveSimulationResponse | null> => {
    const res = await axiosInstance.get<ActiveSimulationResponse | null>('/simulations/active');
    return res.data;
};

export const getMyAssemblyAreas = async (): Promise<MyAssemblyArea[]> => {
    const res = await axiosInstance.get<MyAssemblyArea[]>('/users/me/assembly-areas');
    return res.data;
};
