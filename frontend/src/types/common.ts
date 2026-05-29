export type Role = 'ADMIN' | 'DISTRICT_COORDINATOR' | 'NEIGHBORHOOD_COORDINATOR' | 'VOLUNTEER';

export type BloodType = 'A_POSITIVE' | 'A_NEGATIVE' | 'B_POSITIVE' | 'B_NEGATIVE' | 'AB_POSITIVE' | 'AB_NEGATIVE' | 'O_POSITIVE' | 'O_NEGATIVE';

export type EventStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED';

export type VolunteerStatus = 'ASSIGNED' | 'COMPLETED' | 'WITHDRAWN';

export type DocumentType =
    | 'SEARCH_RESCUE_CERTIFICATE'
    | 'PSYCHOSOCIAL_GRADUATION_DOCUMENT'
    | 'OTHER';

export type DocumentStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type SimulationStatus = 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'PARTIAL_FAILURE';

export type RiskColor = 'GREEN' | 'YELLOW' | 'RED' | 'PURPLE';

export interface PagedResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    last: boolean;
    first: boolean;
    empty: boolean;
}

export interface ErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
    details?: FieldError[];
}

export interface FieldError {
    field: string;
    message: string;
}

export interface MessageResponse {
    message: string;
}
