import { describe, it, expect } from 'vitest';
import { getDefaultLayersForRole, LAYER_ITEMS } from '@/components/map/operationMapLayers';

describe('getDefaultLayersForRole', () => {
    it('gives volunteers risk + damage but no management layers', () => {
        const layers = getDefaultLayersForRole('VOLUNTEER');
        expect(layers.risk).toBe(true);
        expect(layers.damage).toBe(true);
        expect(layers.resources).toBe(false);
        expect(layers.districtCenters).toBe(false);
        expect(layers.neighborhoodCenters).toBe(false);
    });

    it('gives district coordinators resources + district centers on top of the baseline', () => {
        const layers = getDefaultLayersForRole('DISTRICT_COORDINATOR');
        expect(layers.risk).toBe(true);
        expect(layers.damage).toBe(true);
        expect(layers.resources).toBe(true);
        expect(layers.districtCenters).toBe(true);
        expect(layers.neighborhoodCenters).toBe(false);
    });

    it('gives neighborhood coordinators neighborhood centers on top of the baseline', () => {
        const layers = getDefaultLayersForRole('NEIGHBORHOOD_COORDINATOR');
        expect(layers.risk).toBe(true);
        expect(layers.damage).toBe(true);
        expect(layers.neighborhoodCenters).toBe(true);
        expect(layers.resources).toBe(false);
        expect(layers.districtCenters).toBe(false);
    });

    it('does not turn on every layer for any role (avoids marker clutter)', () => {
        for (const role of ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] as const) {
            const layers = getDefaultLayersForRole(role);
            const activeCount = Object.values(layers).filter(Boolean).length;
            expect(activeCount).toBeLessThan(LAYER_ITEMS.length);
        }
    });

    it('falls back to the baseline for an unknown/undefined role', () => {
        const layers = getDefaultLayersForRole(undefined);
        expect(layers.risk).toBe(true);
        expect(layers.damage).toBe(true);
    });
});
