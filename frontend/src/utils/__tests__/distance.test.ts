import { describe, it, expect } from 'vitest';
import { calculateDistanceMeters, formatDistance, estimateWalkingMinutes, formatWalkingEstimate } from '@/utils/distance';

describe('calculateDistanceMeters', () => {
    it('returns ~0 for identical coordinates', () => {
        expect(calculateDistanceMeters(41.0, 29.0, 41.0, 29.0)).toBeCloseTo(0, 3);
    });

    it('computes a known great-circle distance within a reasonable tolerance', () => {
        // Kadıköy (40.9903, 29.0275) -> Beşiktaş (41.0422, 29.0092): gerçek kuş uçuşu ~6.0 km
        const meters = calculateDistanceMeters(40.9903, 29.0275, 41.0422, 29.0092);
        expect(meters).toBeGreaterThan(5000);
        expect(meters).toBeLessThan(7000);
    });

    it('is symmetric', () => {
        const a = calculateDistanceMeters(40.99, 29.02, 41.04, 29.01);
        const b = calculateDistanceMeters(41.04, 29.01, 40.99, 29.02);
        expect(a).toBeCloseTo(b, 6);
    });
});

describe('formatDistance', () => {
    it('formats sub-1000m distances in meters, rounded', () => {
        expect(formatDistance(620)).toBe('620 m');
        expect(formatDistance(45.6)).toBe('46 m');
        expect(formatDistance(999)).toBe('999 m');
    });

    it('formats 1000m and above in km with one decimal', () => {
        expect(formatDistance(1000)).toBe('1.0 km');
        expect(formatDistance(1400)).toBe('1.4 km');
        expect(formatDistance(12400)).toBe('12.4 km');
    });
});

describe('estimateWalkingMinutes / formatWalkingEstimate', () => {
    it('never returns less than 1 minute even for very short distances', () => {
        expect(estimateWalkingMinutes(10)).toBeGreaterThanOrEqual(1);
    });

    it('produces a plausible estimate at ~4.8 km/h walking speed', () => {
        // 4.8 km/h = 80 m/dk -> 620 m ≈ 7.75 dk -> yuvarlanınca 8 dk
        expect(estimateWalkingMinutes(620)).toBe(8);
    });

    it('formats the walking estimate as an explicit "Tahmini" label — never implies real routing data', () => {
        expect(formatWalkingEstimate(620)).toBe('Tahmini 8 dk yürüme');
    });
});
