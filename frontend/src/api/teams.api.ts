import axiosInstance from './axiosInstance';
import { TeamResponse, TeamMemberResponse, TeamTypeResponse, PagedResponse } from '@/types';

export const getTeams = async (): Promise<TeamResponse[]> => {
    const res = await axiosInstance.get<TeamResponse[]>('/teams');
    return res.data;
};

/**
 * Returns static team type enum values — no Hibernate enum mapping, no N+1 queries.
 * Safe to use even when the teams table contains legacy DB enum values.
 * Use this for form dropdowns; use getTeams() when DB metadata (coefficient, document) is needed.
 */
export const getTeamTypes = async (): Promise<TeamTypeResponse[]> => {
    const res = await axiosInstance.get<TeamTypeResponse[]>('/teams/types');
    return res.data;
};

export const getTeamMembers = async (teamId: string, params?: any): Promise<PagedResponse<TeamMemberResponse>> => {
    const res = await axiosInstance.get<PagedResponse<TeamMemberResponse>>(`/teams/${teamId}/members`, { params });
    return res.data;
};
