import { describe, it, expect } from 'vitest';
import { SEMANTIC_BADGE_VARIANT } from '@/utils/statusStyles';

describe('statusStyles — SEMANTIC_BADGE_VARIANT', () => {
    it('maps critical (e.g. CRITICAL) to the danger (red) badge variant', () => {
        expect(SEMANTIC_BADGE_VARIANT.critical).toBe('danger');
    });

    it('maps high (e.g. HIGH) to the high (orange) badge variant', () => {
        expect(SEMANTIC_BADGE_VARIANT.high).toBe('high');
    });

    it('maps pending (e.g. PENDING/WAITING) to the warning (yellow/amber) badge variant', () => {
        expect(SEMANTIC_BADGE_VARIANT.pending).toBe('warning');
    });

    it('maps success (e.g. COMPLETED/APPROVED) to the success (green) badge variant', () => {
        expect(SEMANTIC_BADGE_VARIANT.success).toBe('success');
    });

    it('maps info (e.g. INFO/IN_PROGRESS) to the info (blue) badge variant', () => {
        expect(SEMANTIC_BADGE_VARIANT.info).toBe('info');
    });
});
