import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import type { UserSummaryResponse } from '@/types';

/**
 * 3B bina katmanının Pendik'e özel olmadığını, ve İLÇE değiştirmenin (yalnızca mahalle değil)
 * eski binaları/seçimi temizlediğini doğrular. Kadıköy burada "ikinci, Pendik-dışı bir ilçe"
 * olarak kullanılıyor — is3dActive tamamen building3dConfig.enabledDistricts'e bağlı olduğu için
 * her iki ilçede de aynı şekilde çalışmalı (bkz. MapPage.tsx is3dEligible, hiçbir yerde
 * district.name === 'Pendik' kontrolü yok).
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
const KADIKOY_POLYGON = { type: 'Polygon', coordinates: [[[29.0, 40.95], [29.1, 40.95], [29.1, 41.02], [29.0, 41.02], [29.0, 40.95]]] };
const KURTKOY_POLYGON = { type: 'Polygon', coordinates: [[[29.28, 40.90], [29.31, 40.90], [29.31, 40.92], [29.28, 40.92], [29.28, 40.90]]] };
const MODA_POLYGON = { type: 'Polygon', coordinates: [[[29.02, 40.98], [29.05, 40.98], [29.05, 41.0], [29.02, 41.0], [29.02, 40.98]]] };

const PENDIK = { id: 'pendik-id', name: 'Pendik', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, polygon: PENDIK_POLYGON };
const KADIKOY = { id: 'kadikoy-id', name: 'Kadıköy', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, polygon: KADIKOY_POLYGON };
const KURTKOY = { id: 'kurtkoy-id', name: 'Kurtköy', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, buildingCount: 1505, polygon: KURTKOY_POLYGON };
const MODA = { id: 'moda-id', name: 'Moda', riskScore: 1, riskColor: 'GREEN', riskLevel: 'LOW', openEventCount: 0, openResourceRequestCount: 0, damageCount: 0, buildingCount: 842, polygon: MODA_POLYGON };

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

vi.mock('@/components/map/OperationMap', () => ({
    OperationMap: (props: any) => (
        <div data-testid="operation-map-stub">
            <button onClick={() => props.onSelectDistrict(PENDIK)}>select-pendik</button>
            <button onClick={() => props.onSelectDistrict(KADIKOY)}>select-kadikoy</button>
            <button onClick={() => props.onSelectNeighborhood(KURTKOY)}>select-kurtkoy</button>
            <button onClick={() => props.onSelectNeighborhood(MODA)}>select-moda</button>
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
    (getMapDistricts as any).mockResolvedValue([PENDIK, KADIKOY]);
    (getMapNeighborhoods as any).mockImplementation((districtId: string) =>
        Promise.resolve(districtId === KADIKOY.id ? [MODA] : [KURTKOY]));
    (getMapDamagePoints as any).mockResolvedValue([]);
    (getDistrictCentersForMap as any).mockResolvedValue([]);
    (getNeighborhoodCentersForMap as any).mockResolvedValue([]);
    (getMapDamageSummary as any).mockResolvedValue([]);
    // İki ilçe de config-driven olarak enabled — hiçbiri "Pendik" hardcode'una bağlı değil.
    (getBuilding3dConfig as any).mockResolvedValue({
        enabledDistricts: ['Pendik', 'Kadıköy'], buildingMinZoom: 15, extrusionMinZoom: 16, totalBuildingCount: 21552,
    });
});

describe('MapPage — ilçeden ilçeye geçiş (Kadıköy, Pendik-dışı bir ilçe olarak)', () => {
    it('switching from Pendik/Kurtköy to Kadıköy/Moda remounts BuildingMap and clears the old selected building', async () => {
        (getBuildingById as any).mockResolvedValue({
            id: 'b-1', displayAddress: 'Bağdat Cd. 1', neighborhoodName: 'Kurtköy', buildingType: null,
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
        expect(await screen.findByText(/Bağdat Cd\. 1/)).toBeInTheDocument();

        // İlçe değiştir (yalnızca mahalle değil) — Kadıköy, Pendik-dışı ikinci bir ilçe.
        await user.click(await screen.findByRole('button', { name: '← Şehir Görünümü' }));
        await user.click(await screen.findByRole('button', { name: 'select-kadikoy' }));
        await user.click(await screen.findByRole('button', { name: 'select-moda' }));

        await waitFor(() => expect(FakeMap.instances).toHaveLength(2));
        const modaMap = FakeMap.instances[1];
        act(() => modaMap.fireLoad());
        const modaBoundarySource = modaMap.getSource('neighborhood-boundary') as any;
        expect(modaBoundarySource.initialData).toMatchObject({ geometry: MODA_POLYGON });

        expect(await screen.findByText(/Moda Mahallesi/)).toBeInTheDocument();
        // Eski Pendik/Kurtköy seçili binası Kadıköy'e sızmamalı (handleDistrictClick artık
        // selectedBuildingId/buildingFlyTo'yu da temizliyor).
        expect(screen.queryByText(/Bağdat Cd\. 1/)).not.toBeInTheDocument();
    });
});
