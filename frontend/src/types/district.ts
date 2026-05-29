import { UserSummaryResponse } from './auth';
import { RiskColor } from './common';

export interface DistrictSummaryResponse {
    id: string;
    name: string;
}

export interface DistrictResponse {
    id: string;
    name: string;
    riskScore: number;
    riskColor: RiskColor;
    coordinator?: UserSummaryResponse;
    polygon?: any; // GeoJSON
    riskScoreUpdatedAt: string;
}

export interface NeighborhoodSummaryResponse {
    id: string;
    name: string;
    districtName: string;
}

export interface AssemblyAreaResponse {
    id: string;
    name: string;
    latitude: number;
    longitude: number;
    capacity: number;
    googleMapsUrl: string;
}

export interface AdminAssemblyAreaResponse {
    id: string;
    name: string;
    address: string | null;
    latitude: number | null;
    longitude: number | null;
    googleMapsUrl: string | null;
    capacity: number | null;
    description: string | null;
    sourceName: string | null;
    sourceReference: string | null;
    needsReview: boolean;
    active: boolean;
    createdAt: string;
    district: DistrictSummaryResponse | null;
    neighborhood: NeighborhoodSummaryResponse | null;
}

export interface AssemblyAreaStatsResponse {
    pendingReview: number;
    total: number;
}

export interface NeighborhoodResponse {
    id: string;
    name: string;
    districtId: string;
    riskScore: number;
    riskColor: RiskColor;
    coordinator?: UserSummaryResponse;
    polygon?: any; // GeoJSON
    assemblyAreas: AssemblyAreaResponse[];
}
