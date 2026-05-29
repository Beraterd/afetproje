import { openDB, DBSchema, IDBPDatabase } from 'idb';

export type QueueItemType = 'USER_STATUS_REPORT' | 'DAMAGE_ASSESSMENT';
export type QueueItemStatus = 'PENDING' | 'SENDING' | 'SENT' | 'FAILED';

export interface QueueItemPayload {
    endpoint: string;
    method: 'POST' | 'PUT' | 'PATCH';
    body: Record<string, unknown>;
    authRequired: boolean;
    clientGeneratedId: string;
}

export interface OfflineQueueItem {
    id: string;
    type: QueueItemType;
    payload: QueueItemPayload;
    createdAt: string;
    retryCount: number;
    lastError?: string;
    status: QueueItemStatus;
}

export interface OfflineCacheItem {
    key: string;
    data: unknown;
    updatedAt: string;
    expiresAt?: string;
}

interface AfetDB extends DBSchema {
    offline_queue: {
        key: string;
        value: OfflineQueueItem;
        indexes: { 'by-status': QueueItemStatus };
    };
    offline_cache: {
        key: string;
        value: OfflineCacheItem;
    };
    rq_cache: {
        key: string;
        value: { key: string; clientState: string };
    };
}

let _db: Promise<IDBPDatabase<AfetDB>> | null = null;

export function getDb(): Promise<IDBPDatabase<AfetDB>> {
    if (!_db) {
        _db = openDB<AfetDB>('afet-offline', 1, {
            upgrade(db) {
                const q = db.createObjectStore('offline_queue', { keyPath: 'id' });
                q.createIndex('by-status', 'status');
                db.createObjectStore('offline_cache', { keyPath: 'key' });
                db.createObjectStore('rq_cache', { keyPath: 'key' });
            },
        });
    }
    return _db;
}

export async function clearAllOfflineData(): Promise<void> {
    const db = await getDb();
    await Promise.all([
        db.clear('offline_queue'),
        db.clear('offline_cache'),
        db.clear('rq_cache'),
    ]);
}
