import axiosInstance from './axiosInstance';

export interface CoordinatorDistrictResponse {
    districtId: string;
    districtName: string;
    coordinatorId?: string;
    coordinatorFirstName?: string;
    coordinatorLastName?: string;
    coordinatorEmail?: string;
}

export interface CoordinatorNeighborhoodResponse {
    neighborhoodId: string;
    neighborhoodName: string;
    districtId?: string;
    districtName?: string;
    coordinatorId?: string;
    coordinatorFirstName?: string;
    coordinatorLastName?: string;
    coordinatorEmail?: string;
}

export const listDistrictCoordinators = async (): Promise<CoordinatorDistrictResponse[]> => {
    const res = await axiosInstance.get<CoordinatorDistrictResponse[]>('/admin/coordinators/districts');
    return res.data;
};

export const assignDistrictCoordinator = async (districtId: string, userId: string): Promise<CoordinatorDistrictResponse> => {
    const res = await axiosInstance.put<CoordinatorDistrictResponse>(`/admin/coordinators/districts/${districtId}`, { userId });
    return res.data;
};

export const listNeighborhoodCoordinators = async (districtId?: string): Promise<CoordinatorNeighborhoodResponse[]> => {
    const res = await axiosInstance.get<CoordinatorNeighborhoodResponse[]>('/admin/coordinators/neighborhoods', {
        params: districtId ? { districtId } : undefined,
    });
    return res.data;
};

export const assignNeighborhoodCoordinator = async (neighborhoodId: string, userId: string): Promise<CoordinatorNeighborhoodResponse> => {
    const res = await axiosInstance.put<CoordinatorNeighborhoodResponse>(`/admin/coordinators/neighborhoods/${neighborhoodId}`, { userId });
    return res.data;
};

export const listEligibleUsers = async (assignmentType?: string, districtId?: string): Promise<any[]> => {
    const res = await axiosInstance.get<any[]>('/admin/coordinators/eligible-users', {
        params: { assignmentType, districtId },
    });
    return res.data;
};

export const updateDistrictCoordinatorLocation = async (
    districtId: string,
    latitude: number,
    longitude: number
): Promise<CoordinatorDistrictResponse> => {
    const res = await axiosInstance.patch<CoordinatorDistrictResponse>(
        `/admin/coordinators/districts/${districtId}/location`,
        { latitude, longitude }
    );
    return res.data;
};

export const updateNeighborhoodCoordinatorLocation = async (
    neighborhoodId: string,
    latitude: number,
    longitude: number
): Promise<CoordinatorNeighborhoodResponse> => {
    const res = await axiosInstance.patch<CoordinatorNeighborhoodResponse>(
        `/admin/coordinators/neighborhoods/${neighborhoodId}/location`,
        { latitude, longitude }
    );
    return res.data;
};
