import axiosInstance from './axiosInstance';
import {
    PagedResponse,
    EventSummaryResponse,
    EventResponse,
    EventCloseResponse,
    EventJoinResponse,
    EventVolunteerResponse,
    EventParticipantResponse,
    UserEventResponse,
    MessageResponse,
} from '@/types';

export const getEvents = async (params: any): Promise<PagedResponse<EventSummaryResponse>> => {
    const res = await axiosInstance.get<PagedResponse<EventSummaryResponse>>('/events', { params });
    return res.data;
};

export const getEventById = async (id: string): Promise<EventResponse> => {
    const res = await axiosInstance.get<EventResponse>(`/events/${id}`);
    return res.data;
};

export const createEvent = async (data: any): Promise<EventResponse> => {
    const res = await axiosInstance.post<EventResponse>('/events', data);
    return res.data;
};

export const updateEvent = async (id: string, data: any): Promise<EventResponse> => {
    const res = await axiosInstance.put<EventResponse>(`/events/${id}`, data);
    return res.data;
};

export const closeEvent = async (id: string): Promise<EventCloseResponse> => {
    const res = await axiosInstance.post<EventCloseResponse>(`/events/${id}/close`);
    return res.data;
};

export const completeEvent = async (id: string): Promise<EventCloseResponse> => {
    const res = await axiosInstance.post<EventCloseResponse>(`/events/${id}/complete`);
    return res.data;
};

export const joinEvent = async (id: string): Promise<EventJoinResponse> => {
    const res = await axiosInstance.post<EventJoinResponse>(`/events/${id}/join`);
    return res.data;
};

export const leaveEvent = async (id: string): Promise<void> => {
    await axiosInstance.delete(`/events/${id}/leave`);
};

export const getEventVolunteers = async (id: string, params?: any): Promise<PagedResponse<EventVolunteerResponse>> => {
    const res = await axiosInstance.get<PagedResponse<EventVolunteerResponse>>(`/events/${id}/volunteers`, { params });
    return res.data;
};

export const getMyEvents = async (params?: { page?: number; size?: number; volunteerStatus?: string }): Promise<PagedResponse<UserEventResponse>> => {
    const res = await axiosInstance.get<PagedResponse<UserEventResponse>>('/events/my', { params });
    return res.data;
};

export const getEventParticipants = async (id: string): Promise<EventParticipantResponse[]> => {
    const res = await axiosInstance.get<EventParticipantResponse[]>(`/events/${id}/participants`);
    return res.data;
};
