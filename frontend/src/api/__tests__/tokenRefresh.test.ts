import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { refreshAccessToken } from '@/api/tokenRefresh';

describe('refreshAccessToken (singleflight)', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it('coalesces concurrent refresh calls into a single network request', async () => {
        let resolvePost!: (v: unknown) => void;
        const postPromise = new Promise((resolve) => { resolvePost = resolve; });
        const postSpy = vi.spyOn(axios, 'post').mockReturnValue(postPromise as any);

        const p1 = refreshAccessToken();
        const p2 = refreshAccessToken();
        const p3 = refreshAccessToken();

        expect(postSpy).toHaveBeenCalledTimes(1);

        resolvePost({ data: { accessToken: 'new-access-token', tokenType: 'Bearer', expiresIn: 3600000 } });

        const [t1, t2, t3] = await Promise.all([p1, p2, p3]);
        expect(t1).toBe('new-access-token');
        expect(t2).toBe('new-access-token');
        expect(t3).toBe('new-access-token');
    });

    it('starts a fresh request after the previous refresh has settled', async () => {
        const postSpy = vi.spyOn(axios, 'post')
            .mockResolvedValueOnce({ data: { accessToken: 'token-1' } } as any)
            .mockResolvedValueOnce({ data: { accessToken: 'token-2' } } as any);

        const first = await refreshAccessToken();
        const second = await refreshAccessToken();

        expect(first).toBe('token-1');
        expect(second).toBe('token-2');
        expect(postSpy).toHaveBeenCalledTimes(2);
    });

    it('propagates rejection to every waiting caller when refresh fails', async () => {
        vi.spyOn(axios, 'post').mockRejectedValue(new Error('invalid refresh token'));

        const p1 = refreshAccessToken();
        const p2 = refreshAccessToken();

        await expect(p1).rejects.toThrow('invalid refresh token');
        await expect(p2).rejects.toThrow('invalid refresh token');
    });
});
