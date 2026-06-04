import { UserSummaryResponse } from './auth';

export interface TeamSummaryResponse {
    id: string;
    name: string;
    teamCode?: string;
    coefficient: number;
}

/**
 * Static team type descriptor from GET /teams/types.
 * No DB enum mapping, safe to use when the teams table might have legacy data.
 */
export interface TeamTypeResponse {
    /** Enum name: "SEARCH_RESCUE", "HASAR_TESPIT_EKIBI", etc. */
    value: string;
    /** Turkish label: "Arama Kurtarma Ekibi", "Hasar Tespit Ekibi", etc. */
    label: string;
}

export interface TeamResponse {
    id: string;
    name: string;
    teamCode?: string;
    districtName?: string;
    coefficient: number;
    requiresDocument?: string;
    leader?: UserSummaryResponse;
    activeMemberCount: number;
}

export interface TeamMemberResponse {
    userId: string;
    firstName: string;
    lastName: string;
    joinedAt: string;
    district: string;
    neighborhood: string;
}

export interface TeamJoinResponse {
    teamMembershipId: string;
    teamId: string;
    teamName: string;
    joinedAt: string;
}
