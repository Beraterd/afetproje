import { useEffect, useState } from 'react';

const QUERY = '(prefers-reduced-motion: reduce)';

function getSnapshot(): boolean {
    if (typeof window === 'undefined' || !window.matchMedia) return false;
    return window.matchMedia(QUERY).matches;
}

/** Kullanıcının OS/tarayıcı seviyesinde "prefers-reduced-motion: reduce" tercihini izler. */
export function usePrefersReducedMotion(): boolean {
    const [prefersReducedMotion, setPrefersReducedMotion] = useState(getSnapshot);

    useEffect(() => {
        if (typeof window === 'undefined' || !window.matchMedia) return;
        const mediaQueryList = window.matchMedia(QUERY);
        const handleChange = () => setPrefersReducedMotion(mediaQueryList.matches);
        handleChange();
        mediaQueryList.addEventListener('change', handleChange);
        return () => mediaQueryList.removeEventListener('change', handleChange);
    }, []);

    return prefersReducedMotion;
}
