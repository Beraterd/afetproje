import type { Persister, PersistedClient } from '@tanstack/react-query-persist-client';
import { getDb } from './offlineDb';

const CACHE_KEY = 'rqClientState';

export function createIDBPersister(): Persister {
    return {
        persistClient: async (client: PersistedClient) => {
            try {
                const db = await getDb();
                await db.put('rq_cache', {
                    key: CACHE_KEY,
                    clientState: JSON.stringify(client),
                });
            } catch {
                // IDB unavailable or storage full — silently skip
            }
        },
        restoreClient: async (): Promise<PersistedClient | undefined> => {
            try {
                const db = await getDb();
                const entry = await db.get('rq_cache', CACHE_KEY);
                if (!entry) return undefined;
                return JSON.parse(entry.clientState) as PersistedClient;
            } catch {
                return undefined;
            }
        },
        removeClient: async () => {
            try {
                const db = await getDb();
                await db.delete('rq_cache', CACHE_KEY);
            } catch {
                // Ignore
            }
        },
    };
}
