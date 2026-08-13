import { describe, it, expect, vi, afterEach } from 'vitest';
import { isWebglSupported } from '@/utils/webgl';

describe('isWebglSupported', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('returns true when the canvas can produce a webgl context', () => {
        vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation((type: string) =>
            type === 'webgl2' ? ({} as any) : null
        );
        expect(isWebglSupported()).toBe(true);
    });

    it('returns false when no webgl context is available (fallback path)', () => {
        vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
        expect(isWebglSupported()).toBe(false);
    });

    it('returns false instead of throwing if getContext itself throws', () => {
        vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation(() => {
            throw new Error('WebGL disabled by policy');
        });
        expect(isWebglSupported()).toBe(false);
    });
});
