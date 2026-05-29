import { create } from 'zustand';
import { UserSummaryResponse } from '@/types';

interface AuthState {
    accessToken: string | null;
    user: UserSummaryResponse | null;
    setAuth: (accessToken: string, user: UserSummaryResponse) => void;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    accessToken: null,
    user: null,

    setAuth: (accessToken: string, user: UserSummaryResponse) =>
        set({ accessToken, user }),

    clearAuth: () => set({ accessToken: null, user: null }),
}));
