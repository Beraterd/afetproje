import { BloodType, Role } from './common';
import { DistrictSummaryResponse, NeighborhoodSummaryResponse } from '@/types/district';

export interface UserResponse {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    emailVerified: boolean;
    pendingEmail?: string;
    phone?: string;
    bloodType?: BloodType;
    district?: DistrictSummaryResponse;
    neighborhood?: NeighborhoodSummaryResponse;
    /** Top-level IDs for quick access (mirrors district.id / neighborhood.id) */
    districtId?: string;
    neighborhoodId?: string;
    address?: string;
    profession?: string;
    role: Role;
    /** Backend serializes boolean field "active" (not "isActive") */
    active: boolean;
    createdAt: string;
    updatedAt: string;
}
