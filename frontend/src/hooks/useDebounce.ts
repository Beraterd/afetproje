import { useEffect, useState } from 'react';

/** Verilen değeri `delayMs` boyunca değişmeden kalırsa günceller — agresif request atmayı önler. */
export function useDebounce<T>(value: T, delayMs = 400): T {
    const [debounced, setDebounced] = useState(value);

    useEffect(() => {
        const timer = setTimeout(() => setDebounced(value), delayMs);
        return () => clearTimeout(timer);
    }, [value, delayMs]);

    return debounced;
}
