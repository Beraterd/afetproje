import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { enqueue, updateQueueItem, getAllQueueItems, getPendingItems, removeQueueItem } from '@/lib/offlineQueue';
import { clearAllOfflineData } from '@/lib/offlineDb';
import { processQueue } from '@/lib/syncService';
import { useOfflineStore } from '@/store/offlineStore';
import { useAuthStore } from '@/store/authStore';

function setOnline(value: boolean) {
    Object.defineProperty(navigator, 'onLine', { value, configurable: true });
}

beforeEach(async () => {
    await clearAllOfflineData();
    useOfflineStore.setState({ isOnline: true, isSyncing: false, queueItems: [] });
    useAuthStore.setState({ accessToken: 'test-access-token', user: null });
    setOnline(true);
});

afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
});

describe('offline queue', () => {
    it('queues a mutation while offline instead of sending it', async () => {
        setOnline(false);

        const item = await enqueue('DAMAGE_ASSESSMENT_VERIFY', {
            endpoint: '/damage-assessments/abc-123/verify',
            method: 'PATCH',
            bodyKind: 'json',
            body: { verificationStatus: 'SAHADA_DOGRULANDI' },
            authRequired: true,
        });

        expect(item.status).toBe('PENDING');
        const pending = await getPendingItems();
        expect(pending.map((i) => i.id)).toContain(item.id);

        // processQueue must not attempt any network call while offline.
        const fetchMock = vi.fn();
        vi.stubGlobal('fetch', fetchMock);
        await processQueue();
        expect(fetchMock).not.toHaveBeenCalled();
    });
});

describe('sync engine', () => {
    it('sends a pending item once the device is back online', async () => {
        const item = await enqueue('TASK_COMPLETE', {
            endpoint: '/my-tasks/damage-assessments/assign-1/complete',
            method: 'POST',
            bodyKind: 'none',
            authRequired: true,
        });

        const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ assignmentId: 'assign-1' }), { status: 200 }));
        vi.stubGlobal('fetch', fetchMock);

        await processQueue();

        expect(fetchMock).toHaveBeenCalledTimes(1);
        const [url, init] = fetchMock.mock.calls[0];
        expect(String(url)).toContain('/my-tasks/damage-assessments/assign-1/complete');
        expect(init.method).toBe('POST');

        const all = await getAllQueueItems();
        expect(all.find((i) => i.id === item.id)?.status).toBe('SENT');
    });

    it('removes a record once it has synced successfully', async () => {
        const item = await enqueue('TASK_FIELD_VERIFIED', {
            endpoint: '/my-tasks/damage-assessments/assign-2/field-verified',
            method: 'POST',
            bodyKind: 'none',
            authRequired: true,
        });
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({}), { status: 200 })));

        await processQueue();
        await removeQueueItem(item.id);

        const all = await getAllQueueItems();
        expect(all.find((i) => i.id === item.id)).toBeUndefined();
    });

    it('marks a permanent (4xx) failure as FAILED without endless retries', async () => {
        const item = await enqueue('DAMAGE_ASSESSMENT_VERIFY', {
            endpoint: '/damage-assessments/abc/verify',
            method: 'PATCH',
            bodyKind: 'json',
            body: { verificationStatus: 'SAHADA_DOGRULANDI' },
            authRequired: true,
        });
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
            new Response(JSON.stringify({ message: 'Geçersiz durum' }), { status: 422 })
        ));

        await processQueue();

        const all = await getAllQueueItems();
        const synced = all.find((i) => i.id === item.id)!;
        expect(synced.status).toBe('FAILED');
        expect(synced.lastError).toContain('Geçersiz durum');
    });

    it('retries after a network error and succeeds on the next pass', async () => {
        const item = await enqueue('TASK_COMPLETE', {
            endpoint: '/my-tasks/damage-assessments/assign-3/complete',
            method: 'POST',
            bodyKind: 'none',
            authRequired: true,
        });

        const fetchMock = vi.fn().mockRejectedValueOnce(new TypeError('Failed to fetch'));
        vi.stubGlobal('fetch', fetchMock);
        await processQueue();

        let all = await getAllQueueItems();
        let updated = all.find((i) => i.id === item.id)!;
        expect(updated.status).toBe('PENDING');
        expect(updated.retryCount).toBe(1);

        fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({}), { status: 200 }));
        await processQueue();

        all = await getAllQueueItems();
        updated = all.find((i) => i.id === item.id)!;
        expect(updated.status).toBe('SENT');
    });

    it('resolves a locally-created entity id and routes dependent uploads to the real server id', async () => {
        const createItem = await enqueue('DAMAGE_ASSESSMENT_CREATE', {
            endpoint: '/damage-assessments',
            method: 'POST',
            bodyKind: 'multipart',
            body: { address: 'Test address' },
            authRequired: true,
            entityRef: { kind: 'DAMAGE_ASSESSMENT', localId: 'local-abc' },
        });
        await updateQueueItem(createItem.id, { createdAt: '2026-01-01T00:00:00.000Z' });

        const photoItem = await enqueue('TASK_PHOTO_UPLOAD', {
            endpoint: '/my-tasks/damage-assessments/{entityId}/photos',
            method: 'POST',
            bodyKind: 'none',
            authRequired: true,
            entityRef: { kind: 'DAMAGE_ASSESSMENT_TASK', localId: 'local-abc' },
        });
        await updateQueueItem(photoItem.id, { createdAt: '2026-01-01T00:00:01.000Z' });

        const fetchMock = vi.fn()
            .mockResolvedValueOnce(new Response(JSON.stringify({ id: 'server-984' }), { status: 201 }))
            .mockResolvedValueOnce(new Response(JSON.stringify({}), { status: 200 }));
        vi.stubGlobal('fetch', fetchMock);

        await processQueue();

        expect(fetchMock).toHaveBeenCalledTimes(2);
        const secondUrl = String(fetchMock.mock.calls[1][0]);
        expect(secondUrl).toContain('/my-tasks/damage-assessments/server-984/photos');

        const all = await getAllQueueItems();
        expect(all.find((i) => i.id === photoItem.id)?.status).toBe('SENT');
    });
});
