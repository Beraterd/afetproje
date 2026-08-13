import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import type { UserSummaryResponse } from '@/types';

const PENDIK_POLYGON = { type: 'Polygon', coordinates: [[[29.0, 40.8], [29.4, 40.8], [29.4, 41.0], [29.0, 41.0], [29.0, 40.8]]] };
const KADIKOY_POLYGON = { type: 'Polygon', coordinates: [[[29.0, 40.9], [29.1, 40.9], [29.1, 41.0], [29.0, 41.0], [29.0, 40.9]]] };
const KURTKOY_POLYGON = { type: 'Polygon', coordinates: [[[29.28, 40.9], [29.31, 40.9], [29.31, 40.92], [29.28, 40.92], [29.28, 40.9]]] };
// Gerçek Pendik mahalle sınırı (docker exec ile DB'den doğrulanan) — Kurtköy'e özel olmadığını,
// gerçekten 0 bina kaydı bulunan bir mahallede de doğru davranıldığını göstermek için Paşaköy
// fixture'ı kullanılıyor (Kurtköy↔Yenişehir geçişi ayrı dosyada, MapPage.neighborhoodSwitch.test.tsx'te).
const PASAKOY_POLYGON = { type: 'Polygon', coordinates: [[[29.36, 40.95], [29.37, 40.95], [29.37, 40.96], [29.36, 40.96], [29.36, 40.95]]] };

const PENDIK = { id: 'pendik-id', name: 'Pendik', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, polygon: PENDIK_POLYGON };
const KADIKOY = { id: 'kadikoy-id', name: 'Kadıköy', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, polygon: KADIKOY_POLYGON };
const KURTKOY = { id: 'kurtkoy-id', name: 'Kurtköy', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, buildingCount: 1842, polygon: KURTKOY_POLYGON };
const PASAKOY = { id: 'pasakoy-id', name: 'Paşaköy', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, buildingCount: 0, polygon: PASAKOY_POLYGON };
const KADIKOY_NB = { id: 'kadikoy-nb-id', name: 'Bir Mahalle', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, buildingCount: 0, polygon: KURTKOY_POLYGON };

vi.mock('@/api/map.api', () => ({
    getMapDistricts: vi.fn(),
    getMapNeighborhoods: vi.fn(),
    getMapDamagePoints: vi.fn(),
    getDistrictCentersForMap: vi.fn(),
    getNeighborhoodCentersForMap: vi.fn(),
}));
vi.mock('@/api/operationsAi.api', () => ({ queryOperationsAi: vi.fn() }));
vi.mock('@/api/damageAssessments.api', () => ({ getMapDamageSummary: vi.fn() }));
vi.mock('@/api/buildings.api', () => ({
    getBuilding3dConfig: vi.fn(),
    getBuildingById: vi.fn(),
    getBuildings: vi.fn(),
    searchBuildings: vi.fn(),
}));
vi.mock('@/utils/webgl', () => ({ isWebglSupported: vi.fn(() => true) }));

// Gerçek maplibre-gl (WebGL) jsdom'da çalışmaz — BuildingMap stub'lanır, yalnızca
// MapPage'in "hangi render motorunu seçtiği" (Leaflet vs BuildingMap) ve hangi prop'ları
// geçtiği (colorMode, neighborhoodBoundary) test edilir.
vi.mock('@/components/map/BuildingMap', () => ({
    BuildingMap: (props: any) => (
        <div
            data-testid="building-map-stub"
            data-color-mode={props.colorMode}
            data-has-boundary={props.neighborhoodBoundary ? 'yes' : 'no'}
        >
            <button onClick={() => props.onSelectBuilding('b-1')}>select-building-1</button>
        </div>
    ),
}));
vi.mock('@/components/map/OperationMap', () => ({
    OperationMap: (props: any) => (
        <div data-testid="operation-map-stub">
            <button onClick={() => props.onSelectDistrict({ id: 'pendik-id', name: 'Pendik', polygon: PENDIK_POLYGON })}>
                select-pendik
            </button>
            <button onClick={() => props.onSelectDistrict({ id: 'kadikoy-id', name: 'Kadıköy', polygon: KADIKOY_POLYGON })}>
                select-kadikoy
            </button>
            <button onClick={() => props.onSelectNeighborhood({ id: 'kurtkoy-id', name: 'Kurtköy', polygon: KURTKOY_POLYGON })}>
                select-kurtkoy
            </button>
            <button onClick={() => props.onSelectNeighborhood({ id: 'pasakoy-id', name: 'Paşaköy', polygon: PASAKOY_POLYGON })}>
                select-pasakoy
            </button>
            <button onClick={() => props.onSelectNeighborhood({ id: 'kadikoy-nb-id', name: 'Bir Mahalle', polygon: KURTKOY_POLYGON })}>
                select-kadikoy-nb
            </button>
        </div>
    ),
}));

import { getMapDistricts, getMapNeighborhoods, getMapDamagePoints, getDistrictCentersForMap, getNeighborhoodCentersForMap } from '@/api/map.api';
import { getMapDamageSummary } from '@/api/damageAssessments.api';
import { getBuilding3dConfig, getBuildingById } from '@/api/buildings.api';
import { isWebglSupported } from '@/utils/webgl';
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
    (getMapDistricts as any).mockResolvedValue([PENDIK, KADIKOY]);
    (getMapNeighborhoods as any).mockImplementation((districtId: string) =>
        Promise.resolve(districtId === 'pendik-id' ? [KURTKOY, PASAKOY] : [KADIKOY_NB])
    );
    (getMapDamagePoints as any).mockResolvedValue([]);
    (getDistrictCentersForMap as any).mockResolvedValue([]);
    (getNeighborhoodCentersForMap as any).mockResolvedValue([]);
    (getMapDamageSummary as any).mockResolvedValue([]);
    (isWebglSupported as any).mockReturnValue(true);
    (getBuilding3dConfig as any).mockResolvedValue({
        enabledDistricts: ['Pendik'],
        buildingMinZoom: 15,
        extrusionMinZoom: 16,
        totalBuildingCount: 100,
    });
    (getBuildingById as any).mockResolvedValue({
        id: 'b-1',
        displayAddress: 'Test Sokak 1, Kurtköy Mah., Pendik/İstanbul',
        neighborhoodName: 'Kurtköy',
        buildingType: null,
        levels: null,
        isEstimatedHeight: true,
        estimatedHeightM: 9,
        damageStatus: null,
        damageStatusLabel: null,
        damageAssessmentCount: 0,
        verifiedAssessmentCount: 0,
        latestAssessment: null,
        geometry: { type: 'Polygon', coordinates: [[[29.29, 40.91], [29.3, 40.91], [29.3, 40.92], [29.29, 40.92], [29.29, 40.91]]] },
    });
});

describe('MapPage — Pendik 3B bina drill-down', () => {
    it('district-level Pendik view still uses the Leaflet operation map (no neighborhood selected)', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-pendik' }));

        expect(await screen.findByTestId('operation-map-stub')).toBeInTheDocument();
        expect(screen.queryByTestId('building-map-stub')).not.toBeInTheDocument();
    });

    it('selecting a Pendik neighborhood switches to the 3B building layer', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-pendik' }));
        await user.click(await screen.findByRole('button', { name: 'select-kurtkoy' }));

        expect(await screen.findByTestId('building-map-stub')).toBeInTheDocument();
        expect(screen.queryByTestId('operation-map-stub')).not.toBeInTheDocument();
    });

    it('selecting a neighborhood in a non-3B district keeps the Leaflet operation map', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-kadikoy' }));
        await user.click(await screen.findByRole('button', { name: 'select-kadikoy-nb' }));

        expect(await screen.findByTestId('operation-map-stub')).toBeInTheDocument();
        expect(screen.queryByTestId('building-map-stub')).not.toBeInTheDocument();
    });

    it('clicking a building in 3B mode opens the building details panel with real data', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-pendik' }));
        await user.click(await screen.findByRole('button', { name: 'select-kurtkoy' }));
        await user.click(await screen.findByRole('button', { name: 'select-building-1' }));

        expect(await screen.findByText(/Test Sokak 1/)).toBeInTheDocument();
    });

    it('shows an import-needed message instead of the map when no buildings have been imported', async () => {
        (getBuilding3dConfig as any).mockResolvedValue({
            enabledDistricts: ['Pendik'], buildingMinZoom: 15, extrusionMinZoom: 16, totalBuildingCount: 0,
        });
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-pendik' }));
        await user.click(await screen.findByRole('button', { name: 'select-kurtkoy' }));

        expect(await screen.findByText(/bina verisi henüz içe aktarılmamış/i)).toBeInTheDocument();
        expect(screen.queryByTestId('building-map-stub')).not.toBeInTheDocument();
    });

    it('shows the neighborhood name, building count and default "damage" color mode', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-pendik' }));
        await user.click(await screen.findByRole('button', { name: 'select-kurtkoy' }));

        expect(await screen.findByText(/Kurtköy Mahallesi/)).toBeInTheDocument();
        expect(screen.getByText(/1\.842 bina/)).toBeInTheDocument();
        const stub = screen.getByTestId('building-map-stub');
        expect(stub).toHaveAttribute('data-color-mode', 'damage');
        expect(stub).toHaveAttribute('data-has-boundary', 'yes');
    });

    it('falls back to the 2D operation map when WebGL is unsupported, even in a 3D-eligible neighborhood', async () => {
        (isWebglSupported as any).mockReturnValue(false);
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-pendik' }));
        await user.click(await screen.findByRole('button', { name: 'select-kurtkoy' }));

        expect(await screen.findByTestId('operation-map-stub')).toBeInTheDocument();
        expect(screen.queryByTestId('building-map-stub')).not.toBeInTheDocument();
        expect(screen.getByText(/3B görünüm bu cihazda desteklenmiyor/i)).toBeInTheDocument();
    });

    it('shows a neighborhood-level empty state (not the map) for a real Pendik neighborhood with zero buildings', async () => {
        // Gerçek DB envanterinde bulunan durum: Paşaköy dahil birkaç Pendik mahallesinin sınır
        // verisi bozuk/eksik olduğu için hiç bina eşlenmemiş — bu Kurtköy'e özel bir durum değil,
        // ilçe genelinde (totalBuildingCount>0) veri varken TEK bir mahallede yok. BuildingMap'i
        // bozuk bir sınırla hiç mount etmek yerine net bir mesaj gösterilmeli.
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-pendik' }));
        await user.click(await screen.findByRole('button', { name: 'select-pasakoy' }));

        expect(await screen.findByText(/Bu mahalle için bina verisi bulunamadı/i)).toBeInTheDocument();
        expect(screen.queryByTestId('building-map-stub')).not.toBeInTheDocument();
    });
});
