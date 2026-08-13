import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Map as MaplibreMap } from 'maplibre-gl';
import { BuildingMapDebugPanel } from '../BuildingMapDebugPanel';

const BUILDING_SOURCE_ID = 'buildings';
const BUILDING_LAYER_ID = 'buildings-fill';

const PRODUCTION_PAINT = {
    color: ['match', ['get', 'damageStatus'], 'HEAVY', '#dc2626', '#e2e8f0'],
    height: ['step', ['zoom'], 0, 16, ['get', 'estimatedHeightM']],
    opacity: 0.72,
    base: 0,
};

/** İzole, sade bir sahte MapLibre `Map` — `BuildingMap.test.tsx`'in tam-sadık `FakeMap`'inin aksine,
 *  burada amaç yalnızca `BuildingMapDebugPanel`'in HER okumayı bağımsız güvenli şekilde yapıp
 *  render edip etmediğini test etmek, gerçekçi bir MapLibre kurulumunu simüle etmek değil. */
function createFakeMap(overrides: Partial<Record<string, unknown>> = {}): MaplibreMap {
    const base = {
        getZoom: () => 16.234,
        getPitch: () => 55,
        getBearing: () => -15,
        getSource: (id: string) => (id === BUILDING_SOURCE_ID ? {} : undefined),
        getLayer: (id: string) => (id === BUILDING_LAYER_ID ? {} : undefined),
        getLayoutProperty: () => 'visible',
        getPaintProperty: (_layerId: string, prop: string) => {
            if (prop === 'fill-extrusion-height') return PRODUCTION_PAINT.height;
            if (prop === 'fill-extrusion-color') return PRODUCTION_PAINT.color;
            if (prop === 'fill-extrusion-opacity') return PRODUCTION_PAINT.opacity;
            if (prop === 'fill-extrusion-base') return PRODUCTION_PAINT.base;
            return undefined;
        },
        querySourceFeatures: vi.fn(() => []),
        queryRenderedFeatures: vi.fn(() => []),
        setPaintProperty: vi.fn(),
        setLayerZoomRange: vi.fn(),
        jumpTo: vi.fn(),
    };
    return { ...base, ...overrides } as unknown as MaplibreMap;
}

function renderPanel(getMap: () => MaplibreMap | null) {
    render(
        <BuildingMapDebugPanel
            getMap={getMap}
            buildingSourceId={BUILDING_SOURCE_ID}
            buildingLayerId={BUILDING_LAYER_ID}
            productionPaint={PRODUCTION_PAINT}
        />,
    );
}

async function openPanel() {
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /3B Debug/ }));
    return user;
}

function readResultJson(): Record<string, unknown> {
    const pre = document.querySelector('pre');
    expect(pre).not.toBeNull();
    return JSON.parse(pre!.textContent!);
}

beforeEach(() => {
    vi.spyOn(console, 'log').mockImplementation(() => {});
    vi.spyOn(console, 'warn').mockImplementation(() => {});
});

describe('BuildingMapDebugPanel', () => {
    it('1. "Değerleri Oku" click handler actually runs (logs + produces a result)', async () => {
        const map = createFakeMap();
        renderPanel(() => map);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Değerleri Oku' }));

        expect(console.log).toHaveBeenCalledWith('[3B DEBUG] read clicked');
        expect(readResultJson().status).toBe('ok');
    });

    it('3. shows "Map instance hazır değil." when the map ref is null, without throwing', async () => {
        renderPanel(() => null);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Değerleri Oku' }));

        expect(console.warn).toHaveBeenCalledWith('[3B DEBUG] map instance missing');
        expect(screen.getByText('Map instance hazır değil.')).toBeInTheDocument();
    });

    it('4. if getZoom() throws, the rest of the result still renders (per-field isolation)', async () => {
        const map = createFakeMap({
            getZoom: () => { throw new Error('boom: getZoom'); },
        });
        renderPanel(() => map);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Değerleri Oku' }));

        const result = readResultJson();
        expect(result.status).toBe('ok');
        expect(result.zoom).toEqual({ error: 'boom: getZoom' });
        expect(result.pitch).toBe(55); // diğer alanlar etkilenmedi
        expect(result.buildingLayerExists).toBe(true);
    });

    it('5. if getPaintProperty() throws, the result still renders with the other fields intact', async () => {
        const map = createFakeMap({
            getPaintProperty: () => { throw new Error('boom: getPaintProperty'); },
        });
        renderPanel(() => map);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Değerleri Oku' }));

        const result = readResultJson();
        expect(result.status).toBe('ok');
        expect(result.fillExtrusionHeight).toEqual({ error: 'boom: getPaintProperty' });
        expect(result.fillExtrusionColor).toEqual({ error: 'boom: getPaintProperty' });
        expect(result.zoom).toBe(16.234); // ilgisiz alan etkilenmedi
    });

    it('6. does not call querySourceFeatures when the building source does not exist', async () => {
        const map = createFakeMap({ getSource: () => undefined });
        renderPanel(() => map);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Değerleri Oku' }));

        expect(map.querySourceFeatures).not.toHaveBeenCalled();
        const result = readResultJson();
        expect(result.buildingSourceExists).toBe(false);
        expect(result.sourceFeatureCount).toBe(0);
    });

    it('7. does not call queryRenderedFeatures when the building layer does not exist', async () => {
        const map = createFakeMap({ getLayer: () => undefined });
        renderPanel(() => map);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Değerleri Oku' }));

        expect(map.queryRenderedFeatures).not.toHaveBeenCalled();
        const result = readResultJson();
        expect(result.buildingLayerExists).toBe(false);
        expect(result.renderedFeatureCount).toBe(0);
    });

    it('8. the result renders as JSON inside a <pre> element', async () => {
        const map = createFakeMap();
        renderPanel(() => map);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Değerleri Oku' }));

        const pre = document.querySelector('pre');
        expect(pre).not.toBeNull();
        expect(() => JSON.parse(pre!.textContent!)).not.toThrow();
    });

    it('9. missing/undefined feature properties are normalized to null (not silently dropped)', async () => {
        const map = createFakeMap({
            querySourceFeatures: vi.fn(() => [
                { type: 'Feature', id: 'b-1', geometry: { type: 'Polygon', coordinates: [] }, properties: {} },
            ]),
        });
        renderPanel(() => map);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Değerleri Oku' }));

        const result = readResultJson();
        const firstFeatures = result.firstFeatures as Array<Record<string, unknown>>;
        expect(firstFeatures[0]).toMatchObject({
            estimatedHeightM: null,
            renderHeight: null,
            height: null,
            levels: null,
            heightSource: null,
            damageStatus: null,
        });
    });

    it('10. "Debug Extrusion Aç" shows an error message when the building layer is missing', async () => {
        const map = createFakeMap({ getLayer: () => undefined });
        renderPanel(() => map);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: /Debug Extrusion Aç/ }));

        expect(screen.getByText('Building extrusion layer bulunamadı.')).toBeInTheDocument();
        expect(map.setPaintProperty).not.toHaveBeenCalled();
    });

    it('11. "Kamerayı Zorla" calls map.jumpTo with pitch 55 / bearing -15', async () => {
        const map = createFakeMap();
        renderPanel(() => map);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Kamerayı Zorla (55°/-15°)' }));

        expect(map.jumpTo).toHaveBeenCalledWith({ pitch: 55, bearing: -15 });
    });

    it('"Kamerayı Zorla" shows an error message when the map instance is missing', async () => {
        renderPanel(() => null);
        const user = await openPanel();

        await user.click(screen.getByRole('button', { name: 'Kamerayı Zorla (55°/-15°)' }));

        expect(screen.getByText('Map instance hazır değil.')).toBeInTheDocument();
    });
});
