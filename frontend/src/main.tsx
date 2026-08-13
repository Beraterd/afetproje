import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import App from '@/App';
import { ErrorBoundary } from '@/components/shared/ErrorBoundary';
import { createIDBPersister } from '@/lib/idbPersister';
import { initSyncService } from '@/lib/syncService';
import { queryClient } from '@/lib/queryClient';
import './index.css';

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
            <ErrorBoundary level="global">
                <BrowserRouter>
                    <App />
                </BrowserRouter>
            </ErrorBoundary>
        </PersistQueryClientProvider>
    </React.StrictMode>,
);
