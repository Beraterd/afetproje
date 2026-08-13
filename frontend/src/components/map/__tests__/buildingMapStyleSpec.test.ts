import { describe, it, expect } from 'vitest';
import { validateStyleMin } from '@maplibre/maplibre-gl-style-spec';
import { buildFillColorExpression, buildFillOpacityExpression, buildHeightExpression } from '../buildingColorExpressions';
import type { BuildingMapMode } from '../BuildingMap';
import type { BuildingColorMode } from '../BuildingColorModeControl';

/** Regresyon testi: `addLayer({ type: 'fill-extrusion', paint: {...} })` gerçek MapLibre'de mocklanmış
 *  test'lerin (`BuildingMap.test.tsx`) YAKALAYAMADIĞI bir sınıf bug'a açıktı — `fill-extrusion-opacity`
 *  style-spec'te data-driven (`['get', ...]`) expression DESTEKLEMEZ; önceki
 *  `['case', ['==', ['get','damageStatus'], null], 0.72, 0.88]` MapLibre tarafından reddediliyordu
 *  (canlı Kurtköy testinde `buildingLayerExists: false` olarak gözlemlendi — layer HİÇ eklenmiyordu).
 *  Bu test, üç production expression builder'ının ürettiği paint değerlerini gerçek
 *  `@maplibre/maplibre-gl-style-spec` validator'ından geçirir — sahte/mock bir `Map` değil,
 *  MapLibre'nin kendi stil doğrulayıcısı. */
function buildStyle(mode: BuildingMapMode, colorMode: BuildingColorMode) {
    return {
        version: 8 as const,
        sources: {
            'buildings': { type: 'geojson' as const, data: { type: 'FeatureCollection' as const, features: [] } },
        },
        layers: [
            {
                id: 'buildings-fill',
                type: 'fill-extrusion' as const,
                source: 'buildings',
                paint: {
                    'fill-extrusion-height': buildHeightExpression(16),
                    'fill-extrusion-base': 0,
                    'fill-extrusion-opacity': buildFillOpacityExpression(mode, colorMode),
                    'fill-extrusion-color': buildFillColorExpression(mode, colorMode),
                },
            },
        ],
    };
}

describe('BuildingMap fill-extrusion paint — gerçek MapLibre style-spec doğrulaması', () => {
    const combinations: Array<[BuildingMapMode, BuildingColorMode]> = [
        ['operations', 'damage'],
        ['operations', 'height'],
        ['selector', 'damage'],
    ];

    it.each(combinations)('mode=%s colorMode=%s → addLayer geçerli (0 style-spec hatası)', (mode, colorMode) => {
        const errors = validateStyleMin(buildStyle(mode, colorMode) as never);
        expect(errors.map((e) => e.message)).toEqual([]);
    });

    it('fill-extrusion-opacity her zaman sabit/zoom-only bir değerdir — asla data expression değil', () => {
        // style-spec: "fill-extrusion-opacity ... Data-driven styling not supported." — bu invariant'ı
        // doğrudan da kilitleriz, yalnızca validator'a güvenmek yerine (validator kütüphanesi
        // güncellenip bu kısıtı gevşetirse bile niyet açık kalsın).
        for (const [mode, colorMode] of combinations) {
            const opacity = buildFillOpacityExpression(mode, colorMode);
            expect(typeof opacity).toBe('number');
        }
    });
});
