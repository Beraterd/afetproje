import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useDebounce } from '@/hooks/useDebounce';

describe('useDebounce', () => {
    it('settles to the latest value after the delay', async () => {
        const { result, rerender } = renderHook(({ value }) => useDebounce(value, 50), {
            initialProps: { value: 'a' },
        });
        expect(result.current).toBe('a');

        rerender({ value: 'ab' });
        rerender({ value: 'abc' });

        await waitFor(() => expect(result.current).toBe('abc'));
    });
});
