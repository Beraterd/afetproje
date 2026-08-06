import { Role } from './common';

export interface ForgotPasswordRequest {
    email: string;
}

export interface ResetPasswordRequest {
    token: string;
    newPassword: string;
    confirmPassword: string;
}

export interface ResetTokenValidationResponse {
    valid: boolean;
}

export interface UserSummaryResponse {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    role: Role;
    districtId?: string;
    neighborhoodId?: string;
    locationPermissionStatus?: string;
    /** True for the read-only "Admin Demo Modu" visitor account */
    demo?: boolean;
}

export interface AuthResponse {
    token: string;
    tokenType: string;
    expiresIn: number;
    user: UserSummaryResponse;
}

export interface TokenRefreshResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
}

export interface StartSmsLoginRequest {
    email: string;
    password: string;
}

export interface StartSmsLoginResponse {
    challengeId: string;
    maskedPhone: string;
    expiresInSeconds: number;
}

export interface VerifySmsLoginRequest {
    challengeId: string;
    code: string;
}

export interface ResendSmsLoginRequest {
    challengeId: string;
}
