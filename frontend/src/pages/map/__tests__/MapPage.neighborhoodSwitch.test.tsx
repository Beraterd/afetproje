import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import type { UserSummaryResponse } from '@/types';

/**
 * Kurtköy'ün "özel durum" olmadığını uçtan uca kanıtlayan test: BuildingMap burada STUB'LANMAZ
 * (diğer MapPage.building3d.test.tsx dosyasının aksine) — gerçek BuildingMap + mocklanmış
 * maplibre-gl ile mahalleden mahalleye geçişte gerçekten ne olduğu doğrulanır. Bulunan gerçek bug:
 * BuildingMap'in kamera/boundary-seed kurulumu yalnızca mount-time effect'inde çalışıyor
 * (`useEffect(..., [])`), yani `initialBounds`/`neighborhoodBoundary` prop'u DEĞİŞSE bile ikinci
 * bir mahalleye geçişte kamera/sınır ESKİ mahallede kalırdı — MapPage artık `key={neighborhoodId}`
 * ile her mahalle seçiminde BuildingMap'i TAMAMEN yeniden mount ediyor. Bu test, remount'un
 * gerçekten olduğunu (yeni bir MapLibre `Map` instance'ı kurulduğunu) ve yeni mahallenin gerçek
 * bounds'unu aldığını doğrudan doğrular.
 */

const { FakeMap } = vi.hoisted(() => {
    class FakeMap {
        static instances: FakeMap[] = [];
        options: Record<string, unknown>;
        handlers: Record<string, Array<(...args: unknown[]) => void>> = {};
        calls: Record<string, unknown[][]> = {};
        sourcesById: Record<string, { setData: ReturnType<typeof vi.fn> }> = {};
        layerOrder: string[] = [];
        private _zoom = 16;

        constructor(options: Record<string, unknown>) {
            this.options = options;
            FakeMap.instances.push(this);
        }
        on(event: string, a: unknown, b?: unknown) {
            const cb = (typeof b === 'function' ? b : a) as (...args: unknown[]) => void;
            (this.handlers[event] ??= []).push(cb);
        }
        once(_event: string, cb: (...args: unknown[]) => void) { cb(); }
        fireLoad() { (this.handlers.load ?? []).forEach((cb) => cb()); }
        fireBuildingClick(featureId: string) {
            (this.handlers.click ?? []).forEach((cb) => cb({ features: [{ properties: { id: featureId } }] }));
        }
        addControl() {}
        fitBounds(...args: unknown[]) { (this.calls.fitBounds ??= []).push(args); }
        setCenter(...args: unknown[]) { (this.calls.setCenter ??= []).push(args); }
        setZoom(z: number) { (this.calls.setZoom ??= []).push([z]); this._zoom = z; }
        flyTo(...args: unknown[]) { (this.calls.flyTo ??= []).push(args); }
        easeTo(...args: unknown[]) { (this.calls.easeTo ??= []).push(args); }
        jumpTo(...args: unknown[]) { (this.calls.jumpTo ??= []).push(args); }
        addSource(id: string, config?: { data?: unknown }) {
            this.sourcesById[id] = { setData: vi.fn() };
            (this.sourcesById[id] as unknown as { initialData: unknown }).initialData = config?.data;
        }
        getSource(id: string) { return this.sourcesById[id]; }
        addLayer(layer: { id: string }) { this.layerOrder.push(layer.id); }
        getLayer(id: string) { return this.layerOrder.includes(id) ? {} : undefined; }
        setPaintProperty(...args: unknown[]) { (this.calls.setPaintProperty ??= []).push(args); }
        getPaintProperty() { return undefined; }
        getLayoutProperty() { return undefined; }
        setLayerZoomRange(...args: unknown[]) { (this.calls.setLayerZoomRange ??= []).push(args); }
        querySourceFeatures() { return []; }
        queryRenderedFeatures() { return []; }
        getCanvas() { return { style: {} as CSSStyleDeclaration }; }
        getBounds() { return { getWest: () => 29, getSouth: () => 40, getEast: () => 29.1, getNorth: () => 40.1 }; }
        getZoom() { return this._zoom; }
        getPitch() { return 55; }
        getBearing() { return -20; }
        remove() {}
    }
    return { FakeMap };
});

vi.mock('maplibre-gl', () => ({
    Map: FakeMap,
    NavigationControl: vi.fn(),
    AttributionControl: vi.fn(),
}));
vi.mock('maplibre-gl/dist/maplibre-gl.css', () => ({}));

const PENDIK_POLYGON = { type: 'Polygon', coordinates: [[[29.0, 40.8], [29.4, 40.8], [29.4, 41.0], [29.0, 41.0], [29.0, 40.8]]] };
// Gerçek Pendik mahalle sınırları (DB'den doğrulanan Kurtköy/Yenişehir bbox'larına yakın örnek
// koordinatlar) — birbirinden AÇIKÇA farklı, karışırlarsa test yakalar.
const KURTKOY_POLYGON = { type: 'Polygon', coordinates: [[[29.28, 40.90], [29.31, 40.90], [29.31, 40.92], [29.28, 40.92], [29.28, 40.90]]] };
const YENISEHIR_POLYGON = { type: 'Polygon', coordinates: [[[29.32, 40.87], [29.35, 40.87], [29.35, 40.89], [29.32, 40.89], [29.32, 40.87]]] };

const PENDIK = { id: 'pendik-id', name: 'Pendik', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, polygon: PENDIK_POLYGON };
const KURTKOY = { id: 'kurtkoy-id', name: 'Kurtköy', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, buildingCount: 1505, polygon: KURTKOY_POLYGON };
const YENISEHIR = { id: 'yenisehir-id', name: 'Yenişehir', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, buildingCount: 1314, polygon: YENISEHIR_POLYGON };

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
    getBuildings: vi.fn().mockResolvedValue({ type: 'FeatureCollection', features: [] }),
    searchBuildings: vi.fn(),
}));
vi.mock('@/utils/webgl', () => ({ isWebglSupported: vi.fn(() => true) }));

// Yalnızca 2B/Leaflet render'ı stub'lanır — 3B tarafı (BuildingMap) GERÇEK, tek mocklanan şey
// altındaki maplibre-gl.
vi.mock('@/components/map/OperationMap', () => ({
    OperationMap: (props: any) => (
        <div data-testid="operation-map-stub">
            <button onClick={() => props.onSelectDistrict({ id: 'pendik-id', name: 'Pendik', polygon: PENDIK_POLYGON })}>
                select-pendik
            </button>
            <button onClick={() => props.onSelectNeighborhood({ id: 'kurtkoy-id', name: 'Kurtköy', polygon: KURTKOY_POLYGON })}>
                select-kurtkoy
            </button>
            <button onClick={() => props.onSelectNeighborhood({ id: 'yenisehir-id', name: 'Yenişehir', polygon: YENISEHIR_POLYGON })}>
                select-yenisehir
            </button>
        </div>
    ),
}));

import { getMapDistricts, getMapNeighborhoods, getMapDamagePoints, getDistrictCentersForMap, getNeighborhoodCentersForMap } from '@/api/map.api';
import { getMapDamageSummary } from '@/api/damageAssessments.api';
import { getBuilding3dConfig, getBuildingById } from '@/api/buildings.api';
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
    FakeMap.instances = [];
    (getMapDistricts as any).mockResolvedValue([PENDIK]);
    (getMapNeighborhoods as any).mockResolvedValue([KURTKOY, YENISEHIR]);
    (getMapDamagePoints as any).mockResolvedValue([]);
    (getDistrictCentersForMap as any).mockResolvedValue([]);
    (getNeighborhoodCentersForMap as any).mockResolvedValue([]);
    (getMapDamageSummary as any).mockResolvedValue([]);
    (getBuilding3dConfig as any).mockResolvedValue({
        enabledDistricts: ['Pendik'], buildingMinZoom: 15, extrusionMinZoom: 16, totalBuildingCount: 20710,
    });
});

describe('MapPage — gerçek BuildingMap ile mahalleden mahalleye geçiş (Kurtköy özel durum değil)', () => {
    it('switching from Kurtköy to Yenişehir fully remounts BuildingMap with a fresh MapLibre instance seeded with the new boundary', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-pendik' }));
        await user.click(await screen.findByRole('button', { name: 'select-kurtkoy' }));

        await waitFor(() => expect(FakeMap.instances).toHaveLength(1));
        const kurtkoyMap = FakeMap.instances[0];
        act(() => kurtkoyMap.fireLoad());
        const kurtkoyBoundarySource = kurtkoyMap.getSource('neighborhood-boundary') as any;
        expect(kurtkoyBoundarySource.initialData).toMatchObject({ geometry: KURTKOY_POLYGON });

        // Mahalle listesine geri dön, Yenişehir'i seç — Kurtköy'de kullanılan AYNI kod yolu.
        await user.click(await screen.findByRole('button', { name: 'İlçe Görünümü' }));
        await user.click(await screen.findByRole('button', { name: 'select-yenisehir' }));

        // Yeni bir MapLibre Map instance'ı kurulmuş olmalı — remount kanıtı.
        await waitFor(() => expect(FakeMap.instances).toHaveLength(2));
        const yenisehirMap = FakeMap.instances[1];
        expect(yenisehirMap).not.toBe(kurtkoyMap);

        act(() => yenisehirMap.fireLoad());
        const yenisehirBoundarySource = yenisehirMap.getSource('neighborhood-boundary') as any;
        expect(yenisehirBoundarySource.initialData).toMatchObject({ geometry: YENISEHIR_POLYGON });
        // Eski Kurtköy map instance'ı hâlâ Kurtköy'ün sınırını taşıyor (kanıt: iki instance
        // birbirinden bağımsız, biri diğerini "güncellemedi") — Yenişehir'e sızmadı.
        expect(kurtkoyBoundarySource.initialData).toMatchObject({ geometry: KURTKOY_POLYGON });

        expect(await screen.findByText(/Yenişehir Mahallesi/)).toBeInTheDocument();
        expect(screen.getByText(/1\.314 bina/)).toBeInTheDocument();
        expect(screen.queryByText(/Kurtköy Mahallesi/)).not.toBeInTheDocument();
    });

    it('clears the selected building when switching neighborhoods', async () => {
        (getBuildingById as any).mockResolvedValue({
            id: 'b-1', displayAddress: 'Test Sokak 1', neighborhoodName: 'Kurtköy', buildingType: null,
            levels: null, isEstimatedHeight: true, estimatedHeightM: 9, damageStatus: null, damageStatusLabel: null,
            damageAssessmentCount: 0, verifiedAssessmentCount: 0, latestAssessment: null,
            geometry: { type: 'Polygon', coordinates: [] },
        });
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        renderMapPage();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: 'select-pendik' }));
        await user.click(await screen.findByRole('button', { name: 'select-kurtkoy' }));
        await waitFor(() => expect(FakeMap.instances).toHaveLength(1));
        act(() => FakeMap.instances[0].fireLoad());
        act(() => FakeMap.instances[0].fireBuildingClick('b-1'));

        expect(await screen.findByText(/Test Sokak 1/)).toBeInTheDocument();

        await user.click(await screen.findByRole('button', { name: 'İlçe Görünümü' }));
        await user.click(await screen.findByRole('button', { name: 'select-yenisehir' }));

        expect(await screen.findByText(/Yenişehir Mahallesi/)).toBeInTheDocument();
        expect(screen.queryByText(/Test Sokak 1/)).not.toBeInTheDocument();
    });
});
