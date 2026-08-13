import { describe, it, expect } from 'vitest';
import { buildFillColorExpression, buildFillOpacityExpression } from '../buildingColorExpressions';

describe('buildFillColorExpression', () => {
    it('returns a flat neutral color for mode="selector", regardless of colorMode', () => {
        expect(buildFillColorExpression('selector', 'damage')).toBe('#93c5fd');
        expect(buildFillColorExpression('selector', 'height')).toBe('#93c5fd');
    });

    it('returns a damageStatus "match" expression for mode="operations"/colorMode="damage"', () => {
        const expr = buildFillColorExpression('operations', 'damage') as unknown[];
        expect(expr[0]).toBe('match');
        expect(expr).toContain('COLLAPSED');
        expect(expr).toContain('HEAVY');
    });

    it('returns an estimatedHeightM "interpolate" expression for mode="operations"/colorMode="height"', () => {
        const expr = buildFillColorExpression('operations', 'height') as unknown[];
        expect(expr[0]).toBe('interpolate');
        expect(JSON.stringify(expr)).toContain('estimatedHeightM');
    });
});

describe('buildFillOpacityExpression', () => {
    it('returns a flat opacity for mode="selector"', () => {
        expect(buildFillOpacityExpression('selector', 'damage')).toBe(0.75);
    });

    it('returns a constant opacity in damage mode — never a data expression', () => {
        // Regresyon testi: önceki ['case', ['==', ['get','damageStatus'], null], 0.72, 0.88] bir
        // DATA expression'dı ("rapor yok" durumuna göre binadan binaya değişiyordu) —
        // fill-extrusion-opacity MapLibre style-spec'te bunu desteklemiyor, addLayer canlı Kurtköy
        // testinde sessizce reddediliyordu (bkz. buildingMapStyleSpec.test.ts, gerçek
        // @maplibre/maplibre-gl-style-spec validator'ıyla doğrulanan kök neden). "Rapor yok"
        // binaların daha soluk görünmesi artık yalnızca renkle (NO_REPORT_COLOR) sağlanıyor.
        const opacity = buildFillOpacityExpression('operations', 'damage');
        expect(typeof opacity).toBe('number');
        expect(opacity).toBeGreaterThanOrEqual(0.6);
    });

    it('uses a uniform opacity in height mode (no "has report" distinction)', () => {
        expect(buildFillOpacityExpression('operations', 'height')).toBe(0.8);
    });
});
