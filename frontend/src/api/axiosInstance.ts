import axios from 'axios';
import { useAuthStore } from '@/store/authStore';
import { parseApiError } from '@/utils/errorParser';

const _apiBase = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '');
const baseURL = _apiBase ? `${_apiBase}/api` : '/api';

const instance = axios.create({
    baseURL,
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' },
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

// On 401, clear session and redirect to login
instance.interceptors.response.use(
    (res) => res,
    (error: any) => {
        if (error.response?.status === 401) {
            useAuthStore.getState().clearAuth();
            localStorage.removeItem('afet_token');
            window.location.href = '/login';
        }
        if (error.response?.status === 403 && error.response?.data?.error === 'DEMO_MODE_RESTRICTED') {
            window.dispatchEvent(new CustomEvent(DEMO_MODE_BLOCKED_EVENT, {
                detail: error.response.data.message || 'Bu işlem demo modunda kullanılamaz.',
            }));
        }
        return Promise.reject(parseApiError(error));
    }
);

export default instance;
