import axios from 'axios';
import { useAuthStore } from '@/store/authStore';
import { parseApiError } from '@/utils/errorParser';
import { refreshAccessToken } from './tokenRefresh';

const _apiBase = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '');
const baseURL = _apiBase ? `${_apiBase}/api` : '/api';

const instance = axios.create({
    baseURL,
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' },
    // Refresh token HttpOnly cookie olarak tutulur — her istekte otomatik gönderilmesi/
    // Set-Cookie ile güncellenmesi için credentials her zaman dahil edilir.
    withCredentials: true,
});

// Attach Bearer token to every request.
// FormData gönderimlerinde default 'application/json' header'ını kaldır —
// aksi takdirde axios'un boundary içeren 'multipart/form-data' ataması
// ezilir ve backend 415 döner.
instance.interceptors.request.use((config) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    if (config.data instanceof FormData) {
        delete config.headers['Content-Type'];
    }
    return config;
});

// Demo modda backend'in engellediği yazma istekleri için global uyarı.
// axios interceptor React context'ine erişemediğinden, ToastProvider'ın dinlediği
// bir DOM event'i yayınlanır — böylece her sayfa/aksiyon için tek tek kod eklemeye gerek kalmaz.
export const DEMO_MODE_BLOCKED_EVENT = 'demo-mode-blocked';

// Bu endpoint'lerin kendi 401'i asla refresh akışını tetiklemesin: login/register/demo-login
// için 401 "hatalı bilgi" anlamına gelir (oturum süresi dolması değil), refresh'in kendi 401'i
// ise zaten geçersiz bir refresh token'ı işaret eder — tekrar refresh denemek sonsuz döngü olur.
const REFRESH_EXEMPT_PATHS = ['/auth/login', '/auth/demo-login', '/auth/register', '/auth/refresh'];

function isExemptFromRefresh(url?: string): boolean {
    if (!url) return false;
    return REFRESH_EXEMPT_PATHS.some((path) => url.includes(path));
}

function redirectToLogin() {
    useAuthStore.getState().clearAuth();
    localStorage.removeItem('afet_token');
    if (window.location.pathname !== '/login') {
        window.location.href = '/login';
    }
}

instance.interceptors.response.use(
    (res) => res,
    async (error: any) => {
        const originalRequest = error.config;
        const status = error.response?.status;
        const exempt = isExemptFromRefresh(originalRequest?.url);

        if (status === 401 && originalRequest && !exempt) {
            if (!originalRequest._retry) {
                originalRequest._retry = true;
                try {
                    const newAccessToken = await refreshAccessToken();
                    useAuthStore.getState().setAccessToken(newAccessToken);
                    localStorage.setItem('afet_token', newAccessToken);
                    originalRequest.headers = originalRequest.headers ?? {};
                    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                    return instance(originalRequest);
                } catch {
                    redirectToLogin();
                    return Promise.reject(parseApiError(error));
                }
            }
            // Yeni token ile bir kez tekrar denendi ve yine 401 geldi — oturum kurtarılamaz.
            redirectToLogin();
        }

        if (status === 403 && error.response?.data?.error === 'DEMO_MODE_RESTRICTED') {
            window.dispatchEvent(new CustomEvent(DEMO_MODE_BLOCKED_EVENT, {
                detail: error.response.data.message || 'Bu işlem demo modunda kullanılamaz.',
            }));
        }
        return Promise.reject(parseApiError(error));
    }
);

export default instance;
