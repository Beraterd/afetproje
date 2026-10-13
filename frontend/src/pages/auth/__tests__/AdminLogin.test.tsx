import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { LoginPage } from '../LoginPage';
import { AuthLayout } from '@/layouts/AuthLayout';
import { DemoModeBanner } from '@/components/shared/DemoModeBanner';
import { useAuthStore } from '@/store/authStore';
import { login, logout } from '@/api/auth.api';
import { clearAllOfflineData } from '@/lib/offlineDb';

vi.mock('@/api/auth.api', () => ({ login: vi.fn(), logout: vi.fn(), demoLogin: vi.fn() }));
vi.mock('@/lib/offlineDb', () => ({ clearAllOfflineData: vi.fn() }));
vi.mock('@/components/shared/ToastProvider', () => ({
    useToast: () => ({ success: vi.fn(), info: vi.fn(), error: vi.fn() }),
}));

const demoUser = { id: 'demo', firstName: 'Demo', lastName: 'User', email: 'demo@example.test', role: 'ADMIN' as const, demo: true };

function setup(path = '/login?mode=admin') {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    client.setQueryData(['demo-data'], 'old session data');
    const view = render(
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={[path]}>
                <Routes>
                    <Route element={<AuthLayout />}>
                        <Route path="/login" element={<LoginPage />} />
                    </Route>
                    <Route path="/demo" element={<DemoModeBanner />} />
                    <Route path="/dashboard" element={<h1>Dashboard</h1>} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return { client, ...view };
}

function submit(container: HTMLElement) {
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'admin@example.test' } });
    fireEvent.change(container.querySelector('input[type=password]')!, { target: { value: 'test-password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Admin Moduna Geç' }));
}

beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.setState({ accessToken: 'demo-token', user: demoUser });
    vi.mocked(logout).mockResolvedValue(undefined);
    vi.mocked(clearAllOfflineData).mockResolvedValue(undefined);
});

describe('Admin mode login', () => {
    it('opens the credential form without elevating the demo session', async () => {
        setup('/demo');
        fireEvent.click(screen.getByRole('link', { name: 'Admin Modu' }));
        expect(await screen.findByRole('heading', { name: 'Yönetici Girişi' })).toBeInTheDocument();
        expect(useAuthStore.getState().user?.demo).toBe(true);
        expect(login).not.toHaveBeenCalled();
    });

    it('switches to the authenticated admin and clears demo caches', async () => {
        const admin = { ...demoUser, id: 'admin', demo: false };
        vi.mocked(login).mockResolvedValue({ token: 'admin-token', user: admin, tokenType: 'Bearer', expiresIn: 3600 });
        const { container, client } = setup();
        submit(container);
        expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
        expect(useAuthStore.getState().user).toEqual(admin);
        expect(localStorage.getItem('afet_token')).toBe('admin-token');
        expect(client.getQueryData(['demo-data'])).toBeUndefined();
        expect(clearAllOfflineData).toHaveBeenCalled();
    });

    it('rejects a valid login that has no administrator role', async () => {
        vi.mocked(login).mockResolvedValue({ token: 'volunteer-token', user: { ...demoUser, role: 'VOLUNTEER', demo: false }, tokenType: 'Bearer', expiresIn: 3600 });
        const { container } = setup();
        submit(container);
        expect(await screen.findByText(/Bu hesap yönetici yetkisine sahip değil/)).toBeInTheDocument();
        expect(logout).toHaveBeenCalled();
        expect(useAuthStore.getState().accessToken).toBeNull();
        expect(localStorage.getItem('afet_token')).toBeNull();
    });

    it('keeps demo permissions when credentials fail', async () => {
        vi.mocked(login).mockRejectedValue({ status: 401 });
        const { container } = setup();
        submit(container);
        await waitFor(() => expect(screen.getByText('Kullanıcı adı/e-posta veya şifre hatalı')).toBeInTheDocument());
        expect(useAuthStore.getState().user?.demo).toBe(true);
        expect(useAuthStore.getState().accessToken).toBe('demo-token');
    });
});
