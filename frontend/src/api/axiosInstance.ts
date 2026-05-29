import axios from 'axios';
import { useAuthStore } from '@/store/authStore';
import { parseApiError } from '@/utils/errorParser';

const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL + '/api',
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

// On 401, clear session and redirect to login
instance.interceptors.response.use(
    (res) => res,
    (error: any) => {
        if (error.response?.status === 401) {
            useAuthStore.getState().clearAuth();
            localStorage.removeItem('afet_token');
            window.location.href = '/login';
        }
        return Promise.reject(parseApiError(error));
    }
);

export default instance;
