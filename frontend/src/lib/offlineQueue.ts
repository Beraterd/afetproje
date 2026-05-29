import { getDb, OfflineQueueItem, QueueItemType, QueueItemPayload, QueueItemStatus } from './offlineDb';

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

export async function getPendingItems(): Promise<OfflineQueueItem[]> {
    const db = await getDb();
    return db.getAllFromIndex('offline_queue', 'by-status', 'PENDING');
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
    await db.delete('offline_queue', id);
}

export async function clearQueue(): Promise<void> {
    const db = await getDb();
    await db.clear('offline_queue');
}
