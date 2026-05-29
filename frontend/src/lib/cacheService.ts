import { getDb } from './offlineDb';

const DEFAULT_TTL_MS = 24 * 60 * 60 * 1000;

export async function cacheSet(key: string, data: unknown, ttlMs = DEFAULT_TTL_MS): Promise<void> {
    try {
        const db = await getDb();
        await db.put('offline_cache', {
            key,
            data,
            updatedAt: new Date().toISOString(),
            expiresAt: ttlMs > 0 ? new Date(Date.now() + ttlMs).toISOString() : undefined,
        });
    } catch {
        // Storage full or unavailable — silently ignore
    }
}

export async function cacheGet<T>(key: string): Promise<{ data: T; updatedAt: string } | null> {
    try {
        const db = await getDb();
        const item = await db.get('offline_cache', key);
        if (!item) return null;
        if (item.expiresAt && new Date(item.expiresAt) < new Date()) {
            await db.delete('offline_cache', key);
            return null;
        }
        return { data: item.data as T, updatedAt: item.updatedAt };
    } catch {
        return null;
    }
}

export async function cacheClear(prefix?: string): Promise<void> {
    try {
        const db = await getDb();
        if (!prefix) {
            await db.clear('offline_cache');
            return;
        }
        const keys = await db.getAllKeys('offline_cache');
        await Promise.all(
            keys.filter((k) => k.startsWith(prefix)).map((k) => db.delete('offline_cache', k)),
        );
    } catch {
        // Ignore
    }
}
