import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import type { UserSummaryResponse } from '@/types';

vi.mock('@/api/map.api', () => ({
    getMapDistricts: vi.fn(),
    getMapNeighborhoods: vi.fn(),
    getMapDamagePoints: vi.fn(),
    getDistrictCentersForMap: vi.fn(),
    getNeighborhoodCentersForMap: vi.fn(),
}));
vi.mock('@/api/operationsAi.api', () => ({
    queryOperationsAi: vi.fn(),
}));

// Gerçek Leaflet haritasını jsdom'da mount etmek yerine, sadece MapPage'in kendi
// state/prop kablolamasını (layer'lar, seçili entity, AI drawer) test etmek için sahte bir
// OperationMap kullanılır — iki "marker"ı simüle eden düğmeler içerir.
vi.mock('@/components/map/OperationMap', () => ({
    OperationMap: (props: any) => (
        <div data-testid="operation-map-stub">
            <button onClick={() => props.onSelectEntity({ kind: 'DAMAGE', data: { id: 'd1', latitude: 1, longitude: 1, damageLevelLabel: 'Ağır', verificationStatusLabel: 'Onaylandı' } })}>
                select-damage-1
            </button>
            <button onClick={() => props.onSelectEntity({ kind: 'DAMAGE', data: { id: 'd2', latitude: 2, longitude: 2, damageLevelLabel: 'Hafif', verificationStatusLabel: 'Onaylandı' } })}>
                select-damage-2
            </button>
        </div>
    ),
}));

import { getMapDistricts, getMapNeighborhoods, getMapDamagePoints, getDistrictCentersForMap, getNeighborhoodCentersForMap } from '@/api/map.api';
import { MapPage } from '@/pages/map/MapPage';

function makeUser(role: UserSummaryResponse['role']): UserSummaryResponse {
    return { id: 'u1', firstName: 'Ada', lastName: 'Lovelace', email: 'a@b.com', role };
}

function renderMapPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/map']}>
                <MapPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    (getMapDistricts as any).mockResolvedValue([]);
    (getMapNeighborhoods as any).mockResolvedValue([]);
    (getMapDamagePoints as any).mockResolvedValue([]);
    (getDistrictCentersForMap as any).mockResolvedValue([]);
    (getNeighborhoodCentersForMap as any).mockResolvedValue([]);
});

describe('MapPage — layer controls', () => {
    it('renders the layer control panel', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();

        expect(await screen.findByRole('button', { name: /katmanlar/i })).toBeInTheDocument();
    });

    it('toggling a layer checkbox updates its checked state', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: /katmanlar/i }));
        const riskCheckbox = await screen.findByRole('checkbox', { name: /risk/i });
        expect(riskCheckbox).toBeChecked(); // ADMIN default: risk açık

        await user.click(riskCheckbox);
        expect(riskCheckbox).not.toBeChecked();
    });

    it('applies role-based default layers on first render (volunteer)', async () => {
        useAuthStore.setState({ user: makeUser('VOLUNTEER'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: /katmanlar/i }));
        expect(await screen.findByRole('checkbox', { name: 'Risk' })).toBeChecked();
        expect(screen.getByRole('checkbox', { name: 'Hasar Tespiti' })).toBeChecked();
        expect(screen.getByRole('checkbox', { name: 'İlçe Merkezleri' })).not.toBeChecked();
    });
});

describe('MapPage — AI Karar Destek drawer', () => {
    it('map renders even while the AI panel is closed by default', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();

        expect(await screen.findByTestId('operation-map-stub')).toBeInTheDocument();
        expect(screen.queryByLabelText(/AI Karar Destek panelini kapat/i)).not.toBeInTheDocument();
    });

    it('opens and closes via the toggle button / close button', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: /ai karar destek/i }));
        expect(await screen.findByRole('dialog', { name: /ai karar destek/i })).toBeInTheDocument();

        await user.click(screen.getByLabelText(/ai karar destek panelini kapat/i));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });

    it('closes via the Escape key', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: /ai karar destek/i }));
        expect(await screen.findByRole('dialog', { name: /ai karar destek/i })).toBeInTheDocument();

        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });

    it('hides the AI Karar Destek button for a VOLUNTEER', async () => {
        useAuthStore.setState({ user: makeUser('VOLUNTEER'), accessToken: 't' });
        renderMapPage();

        expect(await screen.findByTestId('operation-map-stub')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /ai karar destek/i })).not.toBeInTheDocument();
    });

    it('shows the AI Karar Destek button for a DISTRICT_COORDINATOR', async () => {
        useAuthStore.setState({ user: makeUser('DISTRICT_COORDINATOR'), accessToken: 't' });
        renderMapPage();

        expect(await screen.findByRole('button', { name: /ai karar destek/i })).toBeInTheDocument();
    });
});

describe('MapPage — single entity selection', () => {
    it('selecting a new marker replaces the previously shown detail (only one panel at a time)', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-damage-1' }));
        expect(await screen.findByText(/Ağır/)).toBeInTheDocument();
        expect(screen.queryByText(/Hafif/)).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'select-damage-2' }));
        expect(await screen.findByText(/Hafif/)).toBeInTheDocument();
        expect(screen.queryByText(/Ağır/)).not.toBeInTheDocument();

        // Yalnızca tek bir "Detayı kapat" butonu var — birden fazla panel aynı anda açık değil.
        expect(screen.getAllByLabelText(/detayı kapat/i)).toHaveLength(1);
    });
});
