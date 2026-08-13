import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useAuthStore } from '@/store/authStore';

vi.mock('@/api/tokenRefresh', () => ({
    refreshAccessToken: vi.fn(),
}));

import { refreshAccessToken } from '@/api/tokenRefresh';
import axiosInstance from '@/api/axiosInstance';

/** Custom axios adapters are responsible for their own status validation (axios core only
 *  does this inside its built-in http/xhr adapters) — so a 401 "response" must be thrown as
 *  an AxiosError-shaped rejection here, exactly like a real transport would produce. */
function respond(status: number, data: unknown) {
    return async (config: any) => {
        const response = { data, status, statusText: status < 300 ? 'OK' : 'Error', headers: {}, config, request: {} };
        if (status >= 200 && status < 300) return response;
        const error: any = new Error(`Request failed with status code ${status}`);
        error.config = config;
        error.response = response;
        error.isAxiosError = true;
        throw error;
    };
}

describe('axiosInstance 401 handling', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        useAuthStore.setState({ accessToken: 'expired-token', user: null });
        localStorage.clear();
    });

    it('retries the original request once with a refreshed access token', async () => {
        (refreshAccessToken as any).mockResolvedValue('brand-new-token');

        let calls = 0;
        const adapter = async (config: any) => {
            calls += 1;
            if (calls === 1) return respond(401, { message: 'Unauthorized' })(config);
            expect(config.headers.Authorization).toBe('Bearer brand-new-token');
            return respond(200, { ok: true })(config);
        };

        const res = await axiosInstance.get('/protected', { adapter: adapter as any });

        expect(res.data).toEqual({ ok: true });
        expect(refreshAccessToken).toHaveBeenCalledTimes(1);
        expect(useAuthStore.getState().accessToken).toBe('brand-new-token');
    });

    it('clears the session and stops retrying when refresh itself fails', async () => {
        (refreshAccessToken as any).mockRejectedValue(new Error('refresh failed'));

        await expect(
            axiosInstance.get('/protected', { adapter: respond(401, { message: 'Unauthorized' }) as any })
        ).rejects.toBeTruthy();

        expect(useAuthStore.getState().accessToken).toBeNull();
        expect(localStorage.getItem('afet_token')).toBeNull();
    });

    it('does not attempt a refresh for the login endpoint itself', async () => {
        await expect(
            axiosInstance.post('/auth/login', {}, { adapter: respond(401, { message: 'Bad credentials' }) as any })
        ).rejects.toBeTruthy();

        expect(refreshAccessToken).not.toHaveBeenCalled();
    });
});
