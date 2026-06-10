import axiosInstance from './axiosInstance';

export interface EmergencyTokenStatusResponse {
    valid: boolean;
    errorCode?: string;
    senderDisplayName?: string;
    lastKnownLatitude?: number;
    lastKnownLongitude?: number;
    lastKnownLocationAccuracy?: number;
    lastKnownLocationUpdatedAt?: string;
}

export interface EmergencyStatusSendRequest {
    token: string;
    type: string;
    latitude?: number;
    longitude?: number;
    locationAccuracy?: number;
    locationSource?: string;
    userAgent?: string;
}

export interface EmergencyStatusSendResponse {
    result: string;
    success: boolean;
    sentCount: number;
    messageType?: string;
}

export const validateEmergencyToken = async (token: string): Promise<EmergencyTokenStatusResponse> => {
    const res = await axiosInstance.get<EmergencyTokenStatusResponse>('/emergency/token-status', {
        params: { token },
    });
    return res.data;
};

export const sendEmergencyStatus = async (
    data: EmergencyStatusSendRequest,
): Promise<EmergencyStatusSendResponse> => {
    const res = await axiosInstance.post<EmergencyStatusSendResponse>('/emergency/send', data);
    return res.data;
};
