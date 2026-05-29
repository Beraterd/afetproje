export interface CoordinationCenterResponse {
    id: string;

    /** For district centers */
    districtId?: string;
    districtName?: string;

    /** For neighborhood centers */
    neighborhoodId?: string;
    neighborhoodName?: string;

    address: string;
    streetName?: string;
    buildingNo?: string;

    latitude: number;
    longitude: number;
    locationVerified: boolean;

    createdAt: string;
    updatedAt: string;
    updatedBy?: string;
}

export interface CoordinationCenterStatusResponse {
    userRole: string;

    assignedDistrictId?: string;
    assignedDistrictName?: string;

    assignedNeighborhoodId?: string;
    assignedNeighborhoodName?: string;

    /** District containing the assigned neighborhood (NC only) */
    assignedNeighborhoodDistrictId?: string;
    assignedNeighborhoodDistrictName?: string;

    districtCenterDefined: boolean;
    neighborhoodCenterDefined: boolean;
    mustSetCenter: boolean;
}

export interface UpsertCoordinationCenterRequest {
    address: string;
    streetName?: string;
    buildingNo?: string;
    latitude: number;
    longitude: number;
    locationVerified: boolean;
}
