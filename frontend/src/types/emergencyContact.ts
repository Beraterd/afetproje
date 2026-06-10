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

/** Toplanma alanı — GET /api/users/me/assembly-areas yanıtı */
export interface MyAssemblyArea {
    id: string;
    name: string;
    address: string | null;
    latitude: number | null;
    longitude: number | null;
    googleMapsUrl: string | null;
}
