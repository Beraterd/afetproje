export interface EmergencyContactResponse {
    contactUserId: string;
    firstName: string;
    lastName: string;
    email: string;
    createdAt: string;
}

export interface AddEmergencyContactRequest {
    contactUserId: string;
}

export interface UserSearchResponse {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
}

export interface StatusMessageResponse {
    id: string;
    templateKey: string;
    messageText: string;
    recipientCount: number;
    sentCount: number;
    createdAt: string;
}

export interface SendStatusMessageRequest {
    simulationId?: string;
    templateKey: string;
}

export interface ActiveSimulationResponse {
    id: string;
    districtName: string;
    magnitude: number;
    triggeredAt: string;
}

/** Template item returned by GET /api/users/me/emergency-status-messages/templates */
export interface StatusTemplateItem {
    key: string;
    label: string;
}
