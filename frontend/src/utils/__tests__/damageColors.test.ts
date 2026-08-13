import { describe, it, expect } from 'vitest';
import { DAMAGE_STATUS_COLORS, NO_REPORT_COLOR } from '../damageColors';

// Backend `DamageLevel` enum (bkz. backend/.../domain/enums/DamageLevel.java) — bu 5 değer
// eksiksiz kapsanmalı, aksi halde harita/lejant sessizce yanlış (fallback) renk kullanır.
const DAMAGE_LEVEL_VALUES = ['UNASSESSED', 'LIGHT', 'MODERATE', 'HEAVY', 'COLLAPSED'];

describe('DAMAGE_STATUS_COLORS', () => {
    it('covers every DamageLevel enum value', () => {
        for (const level of DAMAGE_LEVEL_VALUES) {
            expect(DAMAGE_STATUS_COLORS).toHaveProperty(level);
        }
        expect(Object.keys(DAMAGE_STATUS_COLORS).sort()).toEqual(DAMAGE_LEVEL_VALUES.sort());
    });

    it('uses valid hex colors for every entry', () => {
        for (const color of Object.values(DAMAGE_STATUS_COLORS)) {
            expect(color).toMatch(/^#[0-9a-f]{6}$/i);
        }
        expect(NO_REPORT_COLOR).toMatch(/^#[0-9a-f]{6}$/i);
    });

    it('keeps NO_REPORT_COLOR distinct from every damage-level color', () => {
        expect(Object.values(DAMAGE_STATUS_COLORS)).not.toContain(NO_REPORT_COLOR);
    });

    it('keeps NO_REPORT_COLOR dark/saturated enough to stay visible against the pale CARTO Light basemap', () => {
        // Regresyon testi: eski değer (#e2e8f0, luminance ~0.91) CARTO Light basemap'e (çok açık
        // gri/krem) neredeyse aynı tondaydı — canlı Kurtköy testinde binaların (çoğunluğu "rapor
        // yok") pratikte görünmez olmasının nedenlerinden biriydi. Perceptual luminance düşük
        // tutularak bu regresyon önlenir.
        const perceptualLuminance = (hex: string): number => {
            const r = parseInt(hex.slice(1, 3), 16);
            const g = parseInt(hex.slice(3, 5), 16);
            const b = parseInt(hex.slice(5, 7), 16);
            return (0.299 * r + 0.587 * g + 0.114 * b) / 255;
        };
        expect(perceptualLuminance(NO_REPORT_COLOR)).toBeLessThan(0.75);
    });
});
