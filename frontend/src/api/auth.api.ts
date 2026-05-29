import axiosInstance from './axiosInstance';
import { AuthResponse, TokenRefreshResponse, MessageResponse, ResetTokenValidationResponse } from '@/types';

export const login = async (data: any): Promise<AuthResponse> => {
    const res = await axiosInstance.post<AuthResponse>('/auth/login', data);
    return res.data;
};

export const register = async (data: any): Promise<MessageResponse> => {
    const res = await axiosInstance.post<MessageResponse>('/auth/register', data);
    return res.data;
};

export const verifyEmail = async (token: string): Promise<MessageResponse> => {
    const res = await axiosInstance.post<MessageResponse>(`/auth/verify-email?token=${token}`);
    return res.data;
};

export const refresh = async (refreshToken: string): Promise<TokenRefreshResponse> => {
    const res = await axiosInstance.post<TokenRefreshResponse>('/auth/refresh', { refreshToken });
    return res.data;
};

export const logout = async (refreshToken: string): Promise<void> => {
    await axiosInstance.post('/auth/logout', { refreshToken });
};

export const forgotPassword = async (email: string): Promise<MessageResponse> => {
    const res = await axiosInstance.post<MessageResponse>('/auth/forgot-password', { email });
    return res.data;
};

export const validateResetToken = async (token: string): Promise<ResetTokenValidationResponse> => {
    const res = await axiosInstance.get<ResetTokenValidationResponse>('/auth/reset-password/validate', {
        params: { token },
    });
    return res.data;
};

export const resetPassword = async (
    token: string,
    newPassword: string,
    confirmPassword: string
): Promise<MessageResponse> => {
    const res = await axiosInstance.post<MessageResponse>('/auth/reset-password', {
        token,
        newPassword,
        confirmPassword,
    });
    return res.data;
};
