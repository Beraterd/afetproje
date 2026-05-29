import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient } from '@tanstack/react-query';
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import App from '@/App';
import { createIDBPersister } from '@/lib/idbPersister';
import { initSyncService } from '@/lib/syncService';
import './index.css';

const queryClient = new QueryClient({
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

const idbPersister = createIDBPersister();

// Boot offline sync service (online/offline listeners + initial queue flush)
initSyncService();

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
    <React.StrictMode>
        <PersistQueryClientProvider
            client={queryClient}
            persistOptions={{
                persister: idbPersister,
                // Discard persisted cache older than 24 h
                maxAge: 24 * 60 * 60 * 1000,
                dehydrateOptions: {
                    // Only persist queries that have successfully resolved
                    shouldDehydrateQuery: (query) => query.state.status === 'success',
                },
            }}
        >
            <BrowserRouter>
                <App />
            </BrowserRouter>
        </PersistQueryClientProvider>
    </React.StrictMode>,
);
