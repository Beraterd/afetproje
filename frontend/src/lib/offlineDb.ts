import { openDB, DBSchema, IDBPDatabase } from 'idb';

export type QueueItemType =
    | 'DAMAGE_ASSESSMENT_CREATE'
    | 'DAMAGE_ASSESSMENT_VERIFY'
    | 'TASK_FIELD_VERIFIED'
    | 'TASK_COMPLETE'
    | 'TASK_PHOTO_UPLOAD';

export type QueueItemStatus = 'PENDING' | 'SENDING' | 'SENT' | 'FAILED';

/** Bir kuyruk kaydının hangi (henüz senkronize olmamış olabilecek) varlığa ait olduğunu tutar.
 *  `localId` doluysa varlık offline oluşturulmuş ve henüz gerçek bir sunucu ID'si yok; ilgili
 *  CREATE kaydı senkronize olduğunda {@link resolveEntityId} ile `serverId`'ye çözümlenir. */
export interface EntityRef {
    kind: 'DAMAGE_ASSESSMENT' | 'DAMAGE_ASSESSMENT_TASK';
    localId?: string;
    serverId?: string;
}

/** IndexedDB'de saklanan bir offline fotoğraf/dosya referansı. Gerçek Blob `offline_blobs`
 *  store'unda tutulur — büyük ikili verinin kuyruk kaydıyla birlikte tekrar tekrar kopyalanmasını
 *  önler. */
export interface QueuedPhotoRef {
    blobKey: string;
    fieldName: string;
    fileName: string;
    contentType: string;
}

export interface QueueItemPayload {
    /** `{entityId}` içerebilir — gönderim anında entityRef üzerinden çözümlenir. */
    endpoint: string;
    method: 'POST' | 'PUT' | 'PATCH';
    bodyKind: 'json' | 'multipart' | 'none';
    /** JSON body, ya da multipart'ta 'data' parçası olarak gönderilecek alanlar. */
    body?: Record<string, unknown>;
    photoRefs?: QueuedPhotoRef[];
    authRequired: boolean;
    clientGeneratedId: string;
    entityRef?: EntityRef;
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

export interface OfflineBlobRecord {
    key: string;
    blob: Blob;
    fileName: string;
    contentType: string;
    createdAt: string;
}

export interface IdMapRecord {
    localId: string;
    serverId: string;
    createdAt: string;
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
    offline_blobs: {
        key: string;
        value: OfflineBlobRecord;
    };
    offline_id_map: {
        key: string;
        value: IdMapRecord;
    };
}

const DB_VERSION = 2;

let _db: Promise<IDBPDatabase<AfetDB>> | null = null;

export function getDb(): Promise<IDBPDatabase<AfetDB>> {
    if (!_db) {
        _db = openDB<AfetDB>('afet-offline', DB_VERSION, {
            upgrade(db) {
                if (!db.objectStoreNames.contains('offline_queue')) {
                    const q = db.createObjectStore('offline_queue', { keyPath: 'id' });
                    q.createIndex('by-status', 'status');
                }
                if (!db.objectStoreNames.contains('offline_cache')) {
                    db.createObjectStore('offline_cache', { keyPath: 'key' });
                }
                if (!db.objectStoreNames.contains('rq_cache')) {
                    db.createObjectStore('rq_cache', { keyPath: 'key' });
                }
                if (!db.objectStoreNames.contains('offline_blobs')) {
                    db.createObjectStore('offline_blobs', { keyPath: 'key' });
                }
                if (!db.objectStoreNames.contains('offline_id_map')) {
                    db.createObjectStore('offline_id_map', { keyPath: 'localId' });
                }
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
        db.clear('offline_blobs'),
        db.clear('offline_id_map'),
    ]);
}
