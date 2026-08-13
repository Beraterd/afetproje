import {
    getDb,
    OfflineQueueItem,
    QueueItemType,
    QueueItemPayload,
    QueueItemStatus,
    QueuedPhotoRef,
} from './offlineDb';

export async function enqueue(
    type: QueueItemType,
    payload: Omit<QueueItemPayload, 'clientGeneratedId'>,
    clientGeneratedId?: string,
): Promise<OfflineQueueItem> {
    const db = await getDb();
    const item: OfflineQueueItem = {
        id: crypto.randomUUID(),
        type,
        payload: { ...payload, clientGeneratedId: clientGeneratedId ?? crypto.randomUUID() },
        createdAt: new Date().toISOString(),
        retryCount: 0,
        status: 'PENDING',
    };
    await db.put('offline_queue', item);
    return item;
}

/** PENDING kayıtları oluşturulma sırasına göre döner — CREATE işlemleri, ona bağımlı
 *  (aynı varlığa referans veren) kayıtlardan önce işlenmelidir. */
export async function getPendingItems(): Promise<OfflineQueueItem[]> {
    const db = await getDb();
    const items = await db.getAllFromIndex('offline_queue', 'by-status', 'PENDING');
    return items.sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

export async function getItemsByStatus(status: QueueItemStatus): Promise<OfflineQueueItem[]> {
    const db = await getDb();
    return db.getAllFromIndex('offline_queue', 'by-status', status);
}

export async function getAllQueueItems(): Promise<OfflineQueueItem[]> {
    const db = await getDb();
    return db.getAll('offline_queue');
}

export async function updateQueueItem(id: string, updates: Partial<OfflineQueueItem>): Promise<void> {
    const db = await getDb();
    const item = await db.get('offline_queue', id);
    if (item) await db.put('offline_queue', { ...item, ...updates });
}

export async function removeQueueItem(id: string): Promise<void> {
    const db = await getDb();
    const item = await db.get('offline_queue', id);
    if (item?.payload.photoRefs?.length) {
        await Promise.all(item.payload.photoRefs.map((ref) => deleteBlob(ref.blobKey)));
    }
    await db.delete('offline_queue', id);
}

export async function clearQueue(): Promise<void> {
    const db = await getDb();
    const items = await db.getAll('offline_queue');
    await Promise.all(items.flatMap((item) =>
        (item.payload.photoRefs ?? []).map((ref) => deleteBlob(ref.blobKey))));
    await db.clear('offline_queue');
}

// ── Blob storage (offline fotoğraf/dosya) ───────────────────────────────────────

/** Bir dosyayı IndexedDB'ye Blob olarak kaydeder ve kuyruk kaydında referans olarak
 *  kullanılacak bir {@link QueuedPhotoRef} döner. */
export async function storePhotoBlob(file: File, fieldName: string): Promise<QueuedPhotoRef> {
    const db = await getDb();
    const key = crypto.randomUUID();
    await db.put('offline_blobs', {
        key,
        blob: file,
        fileName: file.name,
        contentType: file.type || 'application/octet-stream',
        createdAt: new Date().toISOString(),
    });
    return { blobKey: key, fieldName, fileName: file.name, contentType: file.type || 'application/octet-stream' };
}

export async function getPhotoBlob(blobKey: string): Promise<Blob | undefined> {
    const db = await getDb();
    const record = await db.get('offline_blobs', blobKey);
    return record?.blob;
}

export async function deleteBlob(blobKey: string): Promise<void> {
    const db = await getDb();
    await db.delete('offline_blobs', blobKey);
}

// ── Local ID → server ID eşlemesi ────────────────────────────────────────────────

/** Offline oluşturulan bir varlığın local ID'sini, senkronizasyon sonrası aldığı gerçek
 *  sunucu ID'sine eşler. Aynı local ID'ye referans veren diğer bekleyen kuyruk kayıtları
 *  bu eşlemeyi {@link resolveEntityId} ile okuyup endpoint'lerini günceller. */
export async function mapLocalToServerId(localId: string, serverId: string): Promise<void> {
    const db = await getDb();
    await db.put('offline_id_map', { localId, serverId, createdAt: new Date().toISOString() });
}

export async function resolveEntityId(localId: string): Promise<string | undefined> {
    const db = await getDb();
    const record = await db.get('offline_id_map', localId);
    return record?.serverId;
}
