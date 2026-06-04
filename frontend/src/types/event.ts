import { UserSummaryResponse } from './auth';
import { EventStatus, VolunteerStatus } from './common';
import { NeighborhoodSummaryResponse } from './district';
import { TeamSummaryResponse } from './team';

export interface EventResponse {
    id: string;
    title: string;
    description: string;
    status: EventStatus;
    requiredPeople: number;
    riskScore: number;
    team: TeamSummaryResponse;
    neighborhood: NeighborhoodSummaryResponse;
    createdBy: UserSummaryResponse;
    latitude?: number;
    longitude?: number;
    startsAt: string;
    endsAt: string;
    closedAt?: string;
    assignedVolunteers: number;
    acceptedCount: number;
    isParticipating: boolean;
    currentUserJoined: boolean;
    createdAt: string;
    updatedAt: string;
    eventTeamCode?: string;
}

export interface EventSummaryResponse {
    id: string;
    title: string;
    status: EventStatus;
    requiredPeople: number;
    riskScore: number;
    team: TeamSummaryResponse;
    neighborhood: NeighborhoodSummaryResponse;
    latitude?: number;
    longitude?: number;
    startsAt: string;
    createdAt: string;
    eventTeamCode?: string;
}

export interface EventCloseResponse {
    id: string;
    status: EventStatus;
    closedAt: string;
    riskScore: number;
}

export interface EventVolunteerResponse {
    userId: string;
    firstName: string;
    lastName: string;
    status: VolunteerStatus;
    joinedAt: string;
}

export interface EventJoinResponse {
    eventVolunteerId: string;
    eventId: string;
    status: VolunteerStatus;
    joinedAt: string;
}

export interface EventParticipantResponse {
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    source: 'DIRECT_JOIN' | 'AI_INVITATION';
    status: 'JOINED' | 'INVITED' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED';
    joinedAt: string;
}

export interface UserEventResponse {
    id: string;
    title: string;
    status: EventStatus;
    team: TeamSummaryResponse;
    neighborhood: NeighborhoodSummaryResponse;
    joinedAt: string;
    volunteerStatus: VolunteerStatus;
}
