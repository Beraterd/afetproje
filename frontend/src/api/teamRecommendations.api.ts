import axiosInstance from './axiosInstance';
import axios from 'axios';
import {
    TeamRecommendationRequest,
    TeamRecommendationResponse,
    ApproveRecommendationRequest,
    ApproveRecommendationResponse,
    EventAssignmentResponse,
} from '@/types';
import { PagedResponse } from '@/types';

export const createTeamRecommendation = async (
    data: TeamRecommendationRequest
): Promise<TeamRecommendationResponse> => {
    const res = await axiosInstance.post<TeamRecommendationResponse>('/team-recommendations', data);
    return res.data;
};

export const createEventTeamRecommendation = async (
    eventId: string
): Promise<TeamRecommendationResponse> => {
    const res = await axiosInstance.post<TeamRecommendationResponse>(
        `/events/${eventId}/team-recommendations`
    );
    return res.data;
};

/**
 * §3 — Etkinlik için arka planda otomatik üretilen en güncel öneriyi getirir.
 * Henüz öneri yoksa backend 204 döner; bu durumda null döneriz (frontend poll edebilir).
 */
export const getLatestEventTeamRecommendation = async (
    eventId: string
): Promise<TeamRecommendationResponse | null> => {
    const res = await axiosInstance.get<TeamRecommendationResponse | ''>(
        `/events/${eventId}/team-recommendations/latest`
    );
    return res.status === 204 || !res.data ? null : (res.data as TeamRecommendationResponse);
};

export const listTeamRecommendations = async (params?: {
    status?: string;
    page?: number;
    size?: number;
}): Promise<PagedResponse<TeamRecommendationResponse>> => {
    const res = await axiosInstance.get<PagedResponse<TeamRecommendationResponse>>(
        '/team-recommendations',
        { params }
    );
    return res.data;
};

export const getTeamRecommendationById = async (id: string): Promise<TeamRecommendationResponse> => {
    const res = await axiosInstance.get<TeamRecommendationResponse>(`/team-recommendations/${id}`);
    return res.data;
};

export const approveTeamRecommendation = async (
    id: string,
    data: ApproveRecommendationRequest
): Promise<ApproveRecommendationResponse> => {
    const res = await axiosInstance.post<ApproveRecommendationResponse>(
        `/team-recommendations/${id}/approve`,
        data
    );
    return res.data;
};

export const rejectTeamRecommendation = async (id: string): Promise<TeamRecommendationResponse> => {
    const res = await axiosInstance.post<TeamRecommendationResponse>(`/team-recommendations/${id}/reject`);
    return res.data;
};

export const getEventAssignments = async (eventId: string): Promise<EventAssignmentResponse[]> => {
    const res = await axiosInstance.get<EventAssignmentResponse[]>(`/events/${eventId}/assignments`);
    return res.data;
};

const publicApiBase = (() => {
    const raw = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '');
    return raw ? `${raw}/api` : '/api';
})();

export const acceptAssignmentToken = async (token: string): Promise<Record<string, string>> => {
    const res = await axios.get<Record<string, string>>(`${publicApiBase}/event-assignments/accept`, {
        params: { token },
    });
    return res.data;
};

export const declineAssignmentToken = async (token: string): Promise<Record<string, string>> => {
    const res = await axios.get<Record<string, string>>(`${publicApiBase}/event-assignments/decline`, {
        params: { token },
    });
    return res.data;
};

export const getActiveEventCount = async (): Promise<number> => {
    const res = await axiosInstance.get<{ count: number }>('/events/active-count');
    return res.data.count;
};
