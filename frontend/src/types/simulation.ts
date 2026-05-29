import { DistrictSummaryResponse } from './district';
import { UserSummaryResponse } from './auth';
import { SimulationStatus } from './common';

export interface SimulationCreatedResponse {
    simulationId: string;
    districtId: string;
    districtName: string;
    magnitude: number;
    emailStatus: SimulationStatus;
    totalUsersToNotify: number;
    triggeredAt: string;
}

export interface SimulationDetailResponse {
    id: string;
    district: DistrictSummaryResponse;
    magnitude: number;
    notes?: string;
    emailStatus: SimulationStatus;
    totalQueued: number;
    totalSent: number;
    totalFailed: number;
    triggeredAt: string;
    createdBy: UserSummaryResponse;
}

export interface SimulationLogResponse {
    id: string;
    userId: string;
    emailAddress: string;
    status: string;
    retryCount: number;
    lastError?: string;
    sentAt?: string;
}
