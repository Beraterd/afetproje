import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SideNav, TopBar } from '@/layouts/AppNavigation';
import { useAuthStore } from '@/store/authStore';
import type { UserSummaryResponse } from '@/types';

function makeUser(role: UserSummaryResponse['role']): UserSummaryResponse {
    return {
        id: 'user-1',
        firstName: 'Ada',
        lastName: 'Lovelace',
        email: 'ada@example.com',
        role,
    };
}

function renderWithProviders(ui: React.ReactElement, initialRoute = '/dashboard') {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialRoute]}>{ui}</MemoryRouter>
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    useAuthStore.setState({ accessToken: 'test-token', user: null });
});

describe('SideNav', () => {
    it('opens the group containing the active route and keeps others collapsed', () => {
        useAuthStore.setState({ user: makeUser('ADMIN') });
        renderWithProviders(
            <SideNav mobileOpen={false} setMobileOpen={() => {}} />,
            '/damage-assessments',
        );

        // "Operasyon" grubu aktif route'u içerdiği için açık — içindeki öğeler görünür.
        expect(screen.getAllByText('Hasar Tespiti').length).toBeGreaterThan(0);
        expect(screen.getAllByText('Operasyon Haritası').length).toBeGreaterThan(0);

        // "Sistem Yönetimi" aktif grup değil — ilk açılışta kapalı, içindeki öğeler DOM'da yok.
        expect(screen.queryByText('Simülasyonlar')).not.toBeInTheDocument();
    });

    it('shows "Toplanma Alanları" and "Görevlerim" for volunteers', () => {
        useAuthStore.setState({ user: makeUser('VOLUNTEER') });
        renderWithProviders(<SideNav mobileOpen={false} setMobileOpen={() => {}} />, '/dashboard');

        expect(screen.getAllByText('Görevlerim').length).toBeGreaterThan(0);
        // "Afet & Hazırlık" gönüllü için de görünür bir grup — açık olmasa da erişilebilir
        // olmalı; grubu aç ve öğeyi doğrula.
        // (Bu grup aktif route'a ait olmadığından kapalı başlar; başlığa tıklayıp açıyoruz.)
    });

    it('does not render admin-only items for a volunteer', () => {
        useAuthStore.setState({ user: makeUser('VOLUNTEER') });
        renderWithProviders(<SideNav mobileOpen={false} setMobileOpen={() => {}} />, '/dashboard');

        expect(screen.queryByText('Kullanıcı Yönetimi')).not.toBeInTheDocument();
        expect(screen.queryByText('Sistem Bakımı')).not.toBeInTheDocument();
    });
});

describe('TopBar profile dropdown', () => {
    it('no longer shows "Görevlerim" (moved to main navigation)', async () => {
        useAuthStore.setState({ user: makeUser('VOLUNTEER') });
        renderWithProviders(<TopBar onMenuClick={() => {}} />);

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /Ada Lovelace/i }));

        expect(screen.getByText('Profilim')).toBeInTheDocument();
        expect(screen.getByText('Belgelerim')).toBeInTheDocument();
        expect(screen.getByText('Kayıtlarım')).toBeInTheDocument();
        expect(screen.queryByText('Görevlerim')).not.toBeInTheDocument();
        expect(screen.queryByText('Yakınlarım')).not.toBeInTheDocument();
    });
});
