import { QueryClient } from '@tanstack/react-query';

// Ayrı modülde tutulur: hem main.tsx (Provider) hem syncService.ts (arka planda senkron
// tamamlandığında ilgili query'leri invalidate etmek için) aynı singleton'a erişebilsin diye.
export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 30_000,
            // 24 h in IndexedDB so data is available when app re-opens offline
            gcTime: 24 * 60 * 60 * 1000,
            retry: 1,
            refetchOnWindowFocus: true,
        },
    },
});
