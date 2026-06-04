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
    emailSentCount?: number;
    whatsappSentCount?: number;
    createdAt: string;
}

export interface SendStatusMessageRequest {
    simulationId?: string;
    templateKey: string;
    clientGeneratedId?: string;
    latitude?: number;
    longitude?: number;
    locationAccuracy?: number;
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

/** Toplanma alanı — GET /api/users/me/assembly-areas yanıtı */
export interface MyAssemblyArea {
    id: string;
    name: string;
    address: string | null;
    latitude: number | null;
    longitude: number | null;
    googleMapsUrl: string | null;
}
