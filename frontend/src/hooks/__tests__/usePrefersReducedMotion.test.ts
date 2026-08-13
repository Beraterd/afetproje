import { describe, it, expect, vi, afterEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { usePrefersReducedMotion } from '@/hooks/usePrefersReducedMotion';

function mockMatchMedia(matches: boolean) {
    const listeners: Array<() => void> = [];
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
        matches,
        media: query,
        addEventListener: (_: string, cb: () => void) => listeners.push(cb),
        removeEventListener: vi.fn(),
    })) as unknown as typeof window.matchMedia;
    return { listeners };
}

describe('usePrefersReducedMotion', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('returns false when the OS does not request reduced motion', () => {
        mockMatchMedia(false);
        const { result } = renderHook(() => usePrefersReducedMotion());
        expect(result.current).toBe(false);
    });

    it('returns true when the OS requests reduced motion', () => {
        mockMatchMedia(true);
        const { result } = renderHook(() => usePrefersReducedMotion());
        expect(result.current).toBe(true);
    });
});
