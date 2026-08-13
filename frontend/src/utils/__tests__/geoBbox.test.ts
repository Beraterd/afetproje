import { describe, it, expect } from 'vitest';
import { computeGeoJsonBbox } from '@/utils/geoBbox';

describe('computeGeoJsonBbox', () => {
    it('computes the bounding box of a Polygon geometry', () => {
        const polygon = {
            type: 'Polygon',
            coordinates: [[[29.0, 40.0], [29.1, 40.0], [29.1, 40.1], [29.0, 40.1], [29.0, 40.0]]],
        };
        expect(computeGeoJsonBbox(polygon)).toEqual([29.0, 40.0, 29.1, 40.1]);
    });

    it('unwraps a Feature and computes its geometry bbox', () => {
        const feature = {
            type: 'Feature',
            properties: {},
            geometry: {
                type: 'Polygon',
                coordinates: [[[10, 20], [12, 20], [12, 22], [10, 22], [10, 20]]],
            },
        };
        expect(computeGeoJsonBbox(feature)).toEqual([10, 20, 12, 22]);
    });

    it('handles MultiPolygon by covering all rings', () => {
        const multi = {
            type: 'MultiPolygon',
            coordinates: [
                [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]],
                [[[5, 5], [6, 5], [6, 6], [5, 6], [5, 5]]],
            ],
        };
        expect(computeGeoJsonBbox(multi)).toEqual([0, 0, 6, 6]);
    });

    it('returns null for null/undefined/malformed input', () => {
        expect(computeGeoJsonBbox(null)).toBeNull();
        expect(computeGeoJsonBbox(undefined)).toBeNull();
        expect(computeGeoJsonBbox({})).toBeNull();
    });
});
