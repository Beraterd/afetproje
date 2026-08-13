import axios from 'axios';

const _apiBase = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '');
const baseURL = _apiBase ? `${_apiBase}/api` : '/api';

interface RefreshResponse {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
}

// Module-level singleflight: if several requests 401 at the same moment, they all
// await the SAME in-flight refresh call instead of each triggering their own —
// avoids hammering /auth/refresh and avoids the resulting token-rotation race.
let refreshPromise: Promise<string> | null = null;

/**
 * Exchanges the HttpOnly refresh-token cookie for a new access token. Uses a bare
 * axios call (not the shared instance with interceptors) so this request can never
 * recursively trigger the 401 → refresh flow itself.
 */
export function refreshAccessToken(): Promise<string> {
    if (!refreshPromise) {
        refreshPromise = axios
            .post<RefreshResponse>(`${baseURL}/auth/refresh`, {}, { withCredentials: true })
            .then((res) => res.data.accessToken)
            .finally(() => {
                refreshPromise = null;
            });
    }
    return refreshPromise;
}
