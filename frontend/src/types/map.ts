import { RiskColor } from './common';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface MapDistrictResponse {
    id: string;
    name: string;
    riskScore: number;
    riskColor: RiskColor;
    riskLevel: RiskLevel;
    openEventCount: number;
    openResourceRequestCount: number;
    damageCount: number;
    polygon: any; // GeoJSON Feature
}

export interface MapNeighborhoodResponse {
    id: string;
    name: string;
    riskScore: number;
    riskColor: RiskColor;
    riskLevel: RiskLevel;
    openEventCount: number;
    openResourceRequestCount: number;
    damageCount: number;
    polygon: any; // GeoJSON Feature
}

export interface DistrictCoordinatorMapResponse {
    districtId: string;
    districtName: string;
    latitude: number;
    longitude: number;
    address?: string;
    locationVerified?: boolean;
    openEventCount: number;
    openResourceRequestCount: number;
    totalDamageCount: number;
    centerUpdatedAt?: string;
}

export interface NeighborhoodCoordinatorMapResponse {
    neighborhoodId: string;
    neighborhoodName: string;
    districtId?: string;
    districtName?: string;
    latitude: number;
    longitude: number;
    address?: string;
    locationVerified?: boolean;
    openEventCount: number;
    openResourceRequestCount: number;
    totalDamageCount: number;
    centerUpdatedAt?: string;
}
