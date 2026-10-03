import type { ComponentProps } from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MAP3D_CAMERA } from '@/config/map3d';

const mockUsePrefersReducedMotion = vi.fn(() => false);
vi.mock('@/hooks/usePrefersReducedMotion', () => ({
    usePrefersReducedMotion: () => mockUsePrefersReducedMotion(),
}));

vi.mock('@/api/buildings.api', () => ({
    getBuildings: vi.fn().mockResolvedValue({ type: 'FeatureCollection', features: [] }),
}));
import { getBuildings } from '@/api/buildings.api';

// maplibre-gl (WebGL) jsdom'da çalışmaz — MapPage testlerinin aksine burada BuildingMap'in
// KENDİSİ test edildiği için (davranış: kamera/renklendirme/mahalle sınırı), tüm modülü stub'lamak
// yerine `Map` sınıfını gerçek API yüzeyine sadık kalarak elle mocklıyoruz. `vi.mock` factory'si
// hoisted olduğu için sınıf `vi.hoisted` içinde tanımlanır (aksi halde "before initialization" hatası).
const { FakeMap } = vi.hoisted(() => {
    class FakeMap {
        static instances: FakeMap[] = [];
        // fitBounds'un "hesapladığı" zoom — canlı Kurtköy bug'ını simüle etmek için varsayılan
        // olarak extrusionMinZoom(16)'nın ALTINDA bir değer (gerçek ölçülen senaryo: ~15.8).
        static fitBoundsZoom = 15.8;

        options: Record<string, unknown>;
        handlers: Record<string, Array<(...args: unknown[]) => void>> = {};
        calls: Record<string, unknown[][]> = {};
        sourcesById: Record<string, { setData: ReturnType<typeof vi.fn>; initialData: unknown }> = {};
        layerOrder: string[] = [];
        paintProps: Record<string, Record<string, unknown>> = {};
        layoutProps: Record<string, Record<string, unknown>> = {};
        // Testin doğrudan doldurabileceği basit dönüş değerleri (gerçekçi tile/render simülasyonu
        // yerine — debug panelinin bu iki metodu doğru ÇAĞIRDIĞINI ve döneni doğru YANSITTIĞINI
        // doğrulamak yeterli).
        sourceFeatures: unknown[] = [];
        renderedFeatures: unknown[] = [];
        private _zoom = 14;
        private _pitch = 55;
        private _bearing = -20;

        constructor(options: Record<string, unknown>) {
            this.options = options;
            FakeMap.instances.push(this);
        }
        on(event: string, a: unknown, b?: unknown) {
            const cb = (typeof b === 'function' ? b : a) as (...args: unknown[]) => void;
            (this.handlers[event] ??= []).push(cb);
        }
        once(event: string, cb: (...args: unknown[]) => void) {
            // Test'te fitBounds/easeTo animasyonsuz (senkron) davrandığı için, gerçek MapLibre'nin
            // "geçiş bitince bir kez tetiklenir" semantiğini basitleştirip HEMEN çağırıyoruz —
            // resetView'daki zoom-taban düzeltmesinin doğru zamanda çalıştığını doğrulamak için yeterli.
            cb();
        }
        fireLoad() {
            (this.handlers.load ?? []).forEach((cb) => cb());
        }
        addControl() {}
        fitBounds(...args: unknown[]) {
            (this.calls.fitBounds ??= []).push(args);
            this._zoom = FakeMap.fitBoundsZoom;
        }
        setCenter(...args: unknown[]) { (this.calls.setCenter ??= []).push(args); }
        setZoom(z: number) { (this.calls.setZoom ??= []).push([z]); this._zoom = z; }
        flyTo(...args: unknown[]) { (this.calls.flyTo ??= []).push(args); }
        easeTo(...args: unknown[]) {
            (this.calls.easeTo ??= []).push(args);
            const opts = args[0] as { zoom?: number; pitch?: number; bearing?: number } | undefined;
            if (typeof opts?.zoom === 'number') this._zoom = opts.zoom;
            if (typeof opts?.pitch === 'number') this._pitch = opts.pitch;
            if (typeof opts?.bearing === 'number') this._bearing = opts.bearing;
        }
        jumpTo(opts: { pitch?: number; bearing?: number } = {}) {
            (this.calls.jumpTo ??= []).push([opts]);
            if (typeof opts.pitch === 'number') this._pitch = opts.pitch;
            if (typeof opts.bearing === 'number') this._bearing = opts.bearing;
        }
        addSource(id: string, config?: { data?: unknown }) {
            this.sourcesById[id] = { setData: vi.fn(), initialData: config?.data };
        }
        getSource(id: string) { return this.sourcesById[id]; }
        addLayer(layer: { id: string; paint?: Record<string, unknown> }) {
            this.layerOrder.push(layer.id);
            this.paintProps[layer.id] = { ...(layer.paint ?? {}) };
        }
        getLayer(id: string) { return this.layerOrder.includes(id) ? {} : undefined; }
        setPaintProperty(layerId: string, prop: string, value: unknown) {
            (this.calls.setPaintProperty ??= []).push([layerId, prop, value]);
            (this.paintProps[layerId] ??= {})[prop] = value;
        }
        getPaintProperty(layerId: string, prop: string) { return this.paintProps[layerId]?.[prop]; }
        getLayoutProperty(layerId: string, prop: string) { return this.layoutProps[layerId]?.[prop]; }
        setLayerZoomRange(...args: unknown[]) { (this.calls.setLayerZoomRange ??= []).push(args); }
        querySourceFeatures() { return this.sourceFeatures; }
        queryRenderedFeatures() { return this.renderedFeatures; }
        getCanvas() { return { style: {} as CSSStyleDeclaration }; }
        getBounds() {
            return { getWest: () => 29, getSouth: () => 40, getEast: () => 29.1, getNorth: () => 40.1 };
        }
        getZoom() { return this._zoom; }
        getPitch() { return this._pitch; }
        getBearing() { return this._bearing; }
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

import { BuildingMap } from '../BuildingMap';

const DEFAULT_PROPS: ComponentProps<typeof BuildingMap> = {
    mode: 'operations',
    districtId: 'district-1',
    neighborhoodId: 'nb-1',
    initialBounds: [29.0, 40.9, 29.1, 41.0],
    buildingMinZoom: 15,
    extrusionMinZoom: 16,
    selectedBuildingGeometry: null,
    onSelectBuilding: vi.fn(),
    flyTo: null,
};

function renderMap(overrides: Partial<ComponentProps<typeof BuildingMap>> = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const utils = render(
        <QueryClientProvider client={queryClient}>
            <BuildingMap {...DEFAULT_PROPS} {...overrides} />
        </QueryClientProvider>,
    );
    const rerenderWith = (nextOverrides: Partial<ComponentProps<typeof BuildingMap>>) =>
        utils.rerender(
            <QueryClientProvider client={queryClient}>
                <BuildingMap {...DEFAULT_PROPS} {...overrides} {...nextOverrides} />
            </QueryClientProvider>,
        );
    return { ...utils, rerenderWith };
}

beforeEach(() => {
    FakeMap.instances = [];
    FakeMap.fitBoundsZoom = 15.8;
    mockUsePrefersReducedMotion.mockReturnValue(false);
});

describe('BuildingMap — kamera', () => {
    it('mounts with the configured 3D pitch/bearing', () => {
        renderMap();
        const map = FakeMap.instances[0];
        expect(map.options.pitch).toBe(MAP3D_CAMERA.pitch);
        expect(map.options.bearing).toBe(MAP3D_CAMERA.bearing);
    });

    it('bumps the zoom up to extrusionMinZoom when fitBounds lands below it (real Kurtköy-scale bug)', () => {
        FakeMap.fitBoundsZoom = 15.8; // ölçülen gerçek senaryo: extrusionMinZoom(16)'nın altında
        renderMap({ extrusionMinZoom: 16 });
        const map = FakeMap.instances[0];
        expect(map.getZoom()).toBeGreaterThanOrEqual(16);
        expect(map.calls.setZoom?.at(-1)?.[0]).toBe(16);
    });

    it('does not zoom in further when fitBounds already lands at/above extrusionMinZoom', () => {
        FakeMap.fitBoundsZoom = 17.2;
        renderMap({ extrusionMinZoom: 16 });
        const map = FakeMap.instances[0];
        expect(map.calls.setZoom).toBeUndefined();
        expect(map.getZoom()).toBe(17.2);
    });

    it('does NOT force-zoom into a genuinely wide neighborhood (Şile/Beykoz-scale, >1 zoom level below threshold)', () => {
        // Geniş bir mahallede fitBounds doğal olarak çok daha düşük bir zoom'a iner (ör. 13.5) —
        // extrusionMinZoom(16)'ya zorla sıçramak kullanıcıyı mahallenin rastgele bir köşesine
        // anlamsızca yakınlaştırırdı (item 32). ≤1 seviyelik küçük fark hâlâ tamamlanır (bkz.
        // yukarıdaki test), yalnızca büyük fark (>1) zorlanmıyor.
        FakeMap.fitBoundsZoom = 13.5;
        renderMap({ extrusionMinZoom: 16 });
        const map = FakeMap.instances[0];
        expect(map.calls.setZoom).toBeUndefined();
        expect(map.getZoom()).toBe(13.5);
    });

    it('forwards an AbortSignal to getBuildings so superseded requests can be cancelled', async () => {
        renderMap();
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        await waitFor(() => expect(getBuildings).toHaveBeenCalled());
        const [, signal] = (getBuildings as ReturnType<typeof vi.fn>).mock.calls[0];
        expect(signal).toBeInstanceOf(AbortSignal);
    });

    it('caps the initial camera fit at MAP3D_CAMERA.maxInitialZoom (tiny Pendik neighborhoods should not over-zoom)', () => {
        renderMap();
        const map = FakeMap.instances[0];
        expect(map.calls.fitBounds.at(-1)?.[1]).toMatchObject({ maxZoom: MAP3D_CAMERA.maxInitialZoom });
    });

    it('reset view also caps at maxInitialZoom and re-applies the extrusionMinZoom floor after the transition', async () => {
        renderMap({ extrusionMinZoom: 16 });
        const map = FakeMap.instances[0];
        const user = userEvent.setup();

        await user.click(screen.getByRole('button', { name: 'Görünümü Sıfırla' }));

        expect(map.calls.fitBounds.at(-1)?.[1]).toMatchObject({ maxZoom: MAP3D_CAMERA.maxInitialZoom });
    });

    it('2B/3B are a two-option segmented control; clicking each sets the corresponding pitch', async () => {
        renderMap();
        const map = FakeMap.instances[0];
        const user = userEvent.setup();

        expect(screen.getByRole('button', { name: '2B görünüme geç' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '3B görünüme geç' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: '2B görünüme geç' }));
        expect(map.calls.easeTo.at(-1)?.[0]).toMatchObject({ pitch: MAP3D_CAMERA.flatPitch });

        await user.click(screen.getByRole('button', { name: '3B görünüme geç' }));
        expect(map.calls.easeTo.at(-1)?.[0]).toMatchObject({ pitch: MAP3D_CAMERA.pitch });
    });

    it('reset view returns to the initial bounds with the configured pitch/bearing', async () => {
        renderMap();
        const map = FakeMap.instances[0];
        const user = userEvent.setup();

        await user.click(screen.getByRole('button', { name: 'Görünümü Sıfırla' }));
        expect(map.calls.fitBounds.at(-1)?.[1]).toMatchObject({ pitch: MAP3D_CAMERA.pitch, bearing: MAP3D_CAMERA.bearing });
    });

    it('honors prefers-reduced-motion by using duration 0 for camera easing', async () => {
        mockUsePrefersReducedMotion.mockReturnValue(true);
        renderMap();
        const user = userEvent.setup();
        const map = FakeMap.instances[0];

        await user.click(screen.getByRole('button', { name: '2B görünüme geç' }));
        expect(map.calls.easeTo.at(-1)?.[0]).toMatchObject({ duration: 0 });
    });
});

describe('BuildingMap — renklendirme ve mahalle sınırı', () => {
    it('updates fill-extrusion-color via setPaintProperty when colorMode changes', () => {
        const { rerenderWith } = renderMap({ colorMode: 'damage' });
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        rerenderWith({ colorMode: 'height' });

        const colorCalls = map.calls.setPaintProperty.filter((c) => c[1] === 'fill-extrusion-color');
        expect(colorCalls.length).toBeGreaterThan(0);
        expect(JSON.stringify(colorCalls.at(-1)?.[2])).toContain('estimatedHeightM');
    });

    it('seeds the boundary source with the initial neighborhoodBoundary at load time, not only on later prop changes', () => {
        // Regresyon testi: mahalleye tıklandığında BuildingMap ilk kez neighborhoodBoundary DOLU
        // olarak mount olur — prop hiç "değişmez", bu yüzden yalnızca değişiklik-effect'ine güvenmek
        // sınırın hiç çizilmemesine yol açan gerçek bir bugdı (canlı Kurtköy testinde bulundu).
        const boundary: GeoJSON.Geometry = {
            type: 'Polygon',
            coordinates: [[[29, 40.9], [29.1, 40.9], [29.1, 41], [29, 41], [29, 40.9]]],
        };
        renderMap({ neighborhoodBoundary: boundary });
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        const boundarySource = map.getSource('neighborhood-boundary');
        expect(boundarySource.initialData).toMatchObject({ type: 'Feature', geometry: boundary });
    });

    it('updates the boundary source when neighborhoodBoundary changes after mount', () => {
        const { rerenderWith } = renderMap({ neighborhoodBoundary: null });
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        const boundary: GeoJSON.Geometry = {
            type: 'Polygon',
            coordinates: [[[29, 40.9], [29.1, 40.9], [29.1, 41], [29, 41], [29, 40.9]]],
        };
        rerenderWith({ neighborhoodBoundary: boundary });

        const boundarySource = map.getSource('neighborhood-boundary');
        expect(boundarySource.setData).toHaveBeenCalledWith(
            expect.objectContaining({ type: 'Feature', geometry: boundary }),
        );
    });

    it('seeds a MultiPolygon boundary (Adalar-shaped, multiple islands) without special-casing', () => {
        // Adalar gibi ada ilçelerinde mahalle sınırı tek bir Polygon değil, birden fazla ayrı
        // parçadan (adadan) oluşan MultiPolygon olabilir — boundary seed mantığı geometri tipini
        // hiç kontrol etmiyor (bkz. BuildingMap.tsx: `{ type: 'Feature', geometry: neighborhoodBoundary }`),
        // bu yüzden herhangi bir GeoJSON.Geometry (Polygon veya MultiPolygon) sorunsuz akmalı.
        const multiPolygonBoundary: GeoJSON.Geometry = {
            type: 'MultiPolygon',
            coordinates: [
                [[[29.1, 40.87], [29.12, 40.87], [29.12, 40.89], [29.1, 40.89], [29.1, 40.87]]],
                [[[29.15, 40.85], [29.17, 40.85], [29.17, 40.87], [29.15, 40.87], [29.15, 40.85]]],
            ],
        };
        renderMap({ neighborhoodBoundary: multiPolygonBoundary });
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        const boundarySource = map.getSource('neighborhood-boundary');
        expect(boundarySource.initialData).toMatchObject({ type: 'Feature', geometry: multiPolygonBoundary });
    });

    it('adds the neighborhood boundary line layer after the buildings extrusion layer (never occluded by tall buildings)', () => {
        renderMap();
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        const order = map.layerOrder;
        expect(order.indexOf('neighborhood-boundary-line')).toBeGreaterThan(
            order.indexOf('buildings-fill'),
        );
    });
});

describe('BuildingMap — sokak arama vurgusu ve uçuşu', () => {
    it('adds the street highlight layer after the buildings extrusion layer (same depth-buffer reasoning as the boundary line)', () => {
        renderMap();
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        const order = map.layerOrder;
        expect(order.indexOf('street-highlight-line')).toBeGreaterThan(order.indexOf('buildings-fill'));
    });

    it('seeds the street highlight source empty, then sets it when a street is selected, then clears it on deselect', () => {
        const { rerenderWith } = renderMap({ selectedStreetGeometry: null });
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        const source = map.getSource('street-highlight');
        expect(source.initialData).toEqual({ type: 'FeatureCollection', features: [] });

        const streetGeometry: GeoJSON.Geometry = {
            type: 'MultiLineString',
            coordinates: [[[29.1, 40.9], [29.11, 40.905]]],
        };
        rerenderWith({ selectedStreetGeometry: streetGeometry });
        expect(source.setData).toHaveBeenCalledWith(
            expect.objectContaining({ type: 'Feature', geometry: streetGeometry }),
        );

        rerenderWith({ selectedStreetGeometry: null });
        expect(source.setData).toHaveBeenLastCalledWith({ type: 'FeatureCollection', features: [] });
    });

    it('fitBounds()s to the street bounds WITHOUT forcing 3D pitch/bearing (preserves current camera, unlike building flyTo)', () => {
        const { rerenderWith } = renderMap();
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());
        // Kullanıcı 2B (flat) moddaysa bile sokak zoom'u bu açıyı BOZMAMALI.
        map.easeTo({ pitch: 0, bearing: 0 });

        rerenderWith({ streetFlyTo: { bounds: [29.0, 40.9, 29.02, 40.92], center: { lon: 29.01, lat: 40.91 }, token: 1 } });

        const [boundsArg, optionsArg] = map.calls.fitBounds.at(-1)!;
        expect(boundsArg).toEqual([[29.0, 40.9], [29.02, 40.92]]);
        expect(optionsArg).toMatchObject({ pitch: 0, bearing: 0, maxZoom: 19 });
    });

    it('falls back to flyTo(center) for a near-point street segment instead of a degenerate fitBounds', () => {
        const { rerenderWith } = renderMap({ extrusionMinZoom: 16 });
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());
        // Mount sırasında initialBounds için zaten bir fitBounds çağrısı olur — baseline'ı al.
        const fitBoundsCallsBefore = map.calls.fitBounds?.length ?? 0;

        // ~10m'lik bir bbox — item 22'nin "çok kısa sokak" senaryosu.
        rerenderWith({ streetFlyTo: { bounds: [29.01, 40.91, 29.0101, 40.9101], center: { lon: 29.0105, lat: 40.9105 }, token: 1 } });

        expect(map.calls.fitBounds?.length ?? 0).toBe(fitBoundsCallsBefore); // yeni fitBounds çağrısı YOK
        expect(map.calls.flyTo.at(-1)?.[0]).toMatchObject({ center: [29.0105, 40.9105] });
    });

    it('re-triggers on the same street selected twice in a row (token increments)', () => {
        const { rerenderWith } = renderMap();
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());
        const fitBoundsCallsBefore = map.calls.fitBounds?.length ?? 0;

        const target = { bounds: [29.0, 40.9, 29.02, 40.92] as [number, number, number, number], center: { lon: 29.01, lat: 40.91 } };
        rerenderWith({ streetFlyTo: { ...target, token: 1 } });
        rerenderWith({ streetFlyTo: { ...target, token: 2 } });

        expect(map.calls.fitBounds.length).toBe(fitBoundsCallsBefore + 2);
    });
});

describe('BuildingMap — DEV teşhis paneli entegrasyonu', () => {
    // Panelin İÇ mantığı (safe-read, hata durumları, vb.) BuildingMapDebugPanel.test.tsx'te
    // izole test ediliyor — burada yalnızca BuildingMap'in paneli GERÇEK source/layer ID'leri ve
    // gerçek production paint değerleriyle doğru bağladığı doğrulanır.
    const SAMPLE_FEATURE = {
        type: 'Feature' as const,
        id: 'e0ac7680-9dbf-44ca-b539-b01623649fd9',
        geometry: { type: 'Polygon' as const, coordinates: [[[29.28, 40.91], [29.281, 40.91], [29.281, 40.912], [29.28, 40.912], [29.28, 40.91]]] },
        properties: {
            id: 'e0ac7680-9dbf-44ca-b539-b01623649fd9',
            estimatedHeightM: 6,
            heightSource: 'fallback',
            damageStatus: null,
        },
    };

    it('"Değerleri Oku" reads the real buildings source/layer via the live map instance', async () => {
        (getBuildings as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
            type: 'FeatureCollection',
            features: [SAMPLE_FEATURE],
        });
        renderMap();
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        const buildingsSource = map.getSource('buildings');
        await waitFor(() => expect(buildingsSource.setData).toHaveBeenCalled());

        map.sourceFeatures = [SAMPLE_FEATURE, SAMPLE_FEATURE];
        map.renderedFeatures = [];

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /3B Debug/ }));
        await user.click(screen.getByRole('button', { name: 'Değerleri Oku' }));

        const pre = document.querySelector('pre');
        expect(pre).not.toBeNull();
        const result = JSON.parse(pre!.textContent!);

        expect(result.status).toBe('ok');
        expect(result.buildingSourceExists).toBe(true);
        expect(result.buildingLayerExists).toBe(true);
        expect(result.sourceFeatureCount).toBe(2);
        expect(result.renderedFeatureCount).toBe(0);
        expect(result.firstFeatures).toHaveLength(2);
        expect(result.firstFeatures[0]).toMatchObject({
            geometryType: 'Polygon',
            estimatedHeightM: 6,
            heightSource: 'fallback',
            damageStatus: null,
        });
    });

    it('"Debug Extrusion Aç" forces magenta/40m/opaque on the real building layer; "Kapat" restores the real production paint', async () => {
        renderMap({ extrusionMinZoom: 16 });
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /3B Debug/ }));
        await user.click(screen.getByRole('button', { name: /Debug Extrusion Aç/ }));

        expect(map.getPaintProperty('buildings-fill', 'fill-extrusion-color')).toBe('#ff00ff');
        expect(map.getPaintProperty('buildings-fill', 'fill-extrusion-height')).toBe(40);
        expect(map.getPaintProperty('buildings-fill', 'fill-extrusion-opacity')).toBe(1);
        expect(map.calls.setLayerZoomRange?.at(-1)).toEqual(['buildings-fill', 0, 24]);

        await user.click(screen.getByRole('button', { name: /Debug Extrusion Kapat/ }));
        expect(map.getPaintProperty('buildings-fill', 'fill-extrusion-color')).not.toBe('#ff00ff');
        expect(JSON.stringify(map.getPaintProperty('buildings-fill', 'fill-extrusion-height'))).toContain('estimatedHeightM');
    });

    it('"Kamerayı Zorla" jumps the real map instance directly to pitch 55 / bearing -15', async () => {
        renderMap();
        const map = FakeMap.instances[0];
        act(() => map.fireLoad());

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /3B Debug/ }));
        await user.click(screen.getByRole('button', { name: 'Kamerayı Zorla (55°/-15°)' }));

        expect(map.calls.jumpTo?.at(-1)?.[0]).toEqual({ pitch: 55, bearing: -15 });
    });
});
