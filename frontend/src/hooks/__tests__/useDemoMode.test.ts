import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useDemoMode, DEMO_MODE_DISABLED_REASON } from '@/hooks/useDemoMode';
import { useAuthStore } from '@/store/authStore';

beforeEach(() => {
    useAuthStore.setState({ accessToken: null, user: null });
});

describe('useDemoMode', () => {
    it('reports isDemo=false for a normal user', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com', role: 'ADMIN', demo: false },
        });
        const { result } = renderHook(() => useDemoMode());
        expect(result.current.isDemo).toBe(false);
        expect(result.current.disabledReason).toBeUndefined();
    });

    it('reports isDemo=true with a disabledReason for the demo account', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'Demo', lastName: 'Admin', email: 'demo@b.com', role: 'ADMIN', demo: true },
        });
        const { result } = renderHook(() => useDemoMode());
        expect(result.current.isDemo).toBe(true);
        expect(result.current.disabledReason).toBe(DEMO_MODE_DISABLED_REASON);
    });
});
