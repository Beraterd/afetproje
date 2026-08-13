import { create } from 'zustand';
import { UserSummaryResponse } from '@/types';

interface AuthState {
    accessToken: string | null;
    user: UserSummaryResponse | null;
    setAuth: (accessToken: string, user: UserSummaryResponse) => void;
    setAccessToken: (accessToken: string) => void;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    accessToken: null,
    user: null,

    setAuth: (accessToken: string, user: UserSummaryResponse) =>
        set({ accessToken, user }),

    setAccessToken: (accessToken: string) => set({ accessToken }),

    clearAuth: () => set({ accessToken: null, user: null }),
}));
