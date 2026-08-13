import {
    getPendingItems,
    getItemsByStatus,
    getAllQueueItems,
    updateQueueItem,
    getPhotoBlob,
    mapLocalToServerId,
    resolveEntityId,
    deleteBlob,
} from './offlineQueue';
import type { OfflineQueueItem } from './offlineDb';
import { useOfflineStore } from '@/store/offlineStore';
import { useAuthStore } from '@/store/authStore';
import { refreshAccessToken } from '@/api/tokenRefresh';
import { queryClient } from '@/lib/queryClient';

const MAX_RETRIES = 5;
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '') + '/api';

/** Kuyruktan senkronize edilen bir kaydın sonucu diğer bileşenlere bu event ile duyurulur —
 *  react-query kullanmayan sayfalar (ör. DamageAssessmentsPage, MyTasksPage) bu event'i
 *  dinleyip kendi listelerini yeniden yükler. */
export const OFFLINE_SYNC_ITEM_SYNCED_EVENT = 'offline-sync-item-synced';

let syncInProgress = false;

export async function refreshQueueState(): Promise<void> {
    const items = await getAllQueueItems();
    useOfflineStore.getState().setQueueItems(items);
}

function resolveEndpoint(item: OfflineQueueItem, resolvedEntityId?: string): string | null {
    if (!item.payload.endpoint.includes('{entityId}')) {
        return item.payload.endpoint;
    }
    const id = resolvedEntityId ?? item.payload.entityRef?.serverId;
    if (!id) return null;
    return item.payload.endpoint.replace('{entityId}', id);
}

async function buildRequestBody(item: OfflineQueueItem): Promise<BodyInit | undefined> {
    if (item.payload.bodyKind === 'none') return undefined;

    if (item.payload.bodyKind === 'json') {
        return JSON.stringify(item.payload.body ?? {});
    }

    // multipart
    const formData = new FormData();
    if (item.payload.body) {
        formData.append('data', new Blob([JSON.stringify(item.payload.body)], { type: 'application/json' }));
    }
    for (const ref of item.payload.photoRefs ?? []) {
        const blob = await getPhotoBlob(ref.blobKey);
        if (blob) {
            formData.append(ref.fieldName, blob, ref.fileName);
        }
    }
    return formData;
}

function extractCreatedId(payload: unknown): string | undefined {
    if (payload && typeof payload === 'object' && 'id' in payload) {
        const id = (payload as { id?: unknown }).id;
        return typeof id === 'string' ? id : undefined;
    }
    return undefined;
}

async function cleanupPhotoBlobs(item: OfflineQueueItem): Promise<void> {
    if (!item.payload.photoRefs?.length) return;
    await Promise.all(item.payload.photoRefs.map((ref) => deleteBlob(ref.blobKey)));
}

/** Bir queued isteği bir kez dener. 401 alırsa access token'ı bir kez refresh edip tekrar dener
 *  (sync döngüsü ayrı bir fetch kullandığından axios interceptor'ın refresh mantığından
 *  yararlanamaz — burada aynı tek-uçuşlu refresh mekanizması tekrar kullanılır). */
async function sendOnce(item: OfflineQueueItem, url: string, accessToken: string | null): Promise<Response> {
    const headers: Record<string, string> = {};
    if (item.payload.bodyKind === 'json') {
        headers['Content-Type'] = 'application/json';
    }
    if (item.payload.authRequired && accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
    }
    const body = await buildRequestBody(item);
    return fetch(url, { method: item.payload.method, headers, body, credentials: 'include' });
}

export async function processQueue(): Promise<void> {
    if (syncInProgress || !navigator.onLine) return;

    syncInProgress = true;
    useOfflineStore.getState().setIsSyncing(true);

    try {
        // Reset SENDING items left over from a previous crashed session
        const stuck = await getItemsByStatus('SENDING');
        for (const item of stuck) {
            await updateQueueItem(item.id, { status: 'PENDING' });
        }

        const pending = await getPendingItems();
        if (pending.length === 0) return;

        for (const item of pending) {
            // Bu kayıt henüz sunucuda var olmayan (offline oluşturulmuş) bir varlığa referans
            // veriyorsa, o varlığın CREATE kaydı henüz senkronize olmamış demektir — bu turda
            // atla, bir sonraki turda tekrar denenir (CREATE kaydı normalde createdAt sırasına
            // göre önce işlenir, ama garanti değildir).
            let resolvedEntityId: string | undefined;
            const needsEntityResolution = item.payload.endpoint.includes('{entityId}')
                && item.payload.entityRef?.localId
                && !item.payload.entityRef.serverId;
            if (needsEntityResolution) {
                resolvedEntityId = await resolveEntityId(item.payload.entityRef!.localId!);
                if (!resolvedEntityId) continue;
            }

            const url = resolveEndpoint(item, resolvedEntityId);
            if (!url) continue;

            await updateQueueItem(item.id, { status: 'SENDING' });

            try {
                const token =
                    useAuthStore.getState().accessToken ??
                    localStorage.getItem('afet_token');

                let response = await sendOnce(item, `${API_BASE}${url}`, token);

                if (response.status === 401) {
                    try {
                        const newToken = await refreshAccessToken();
                        useAuthStore.getState().setAccessToken(newToken);
                        localStorage.setItem('afet_token', newToken);
                        response = await sendOnce(item, `${API_BASE}${url}`, newToken);
                    } catch {
                        await updateQueueItem(item.id, {
                            status: 'FAILED',
                            retryCount: MAX_RETRIES,
                            lastError: 'Oturum süresi doldu. Lütfen tekrar giriş yapın.',
                        });
                        continue;
                    }
                }

                if (response.ok) {
                    const responseBody = await response.json().catch(() => undefined);

                    if (item.type === 'DAMAGE_ASSESSMENT_CREATE' && item.payload.entityRef?.localId) {
                        const createdId = extractCreatedId(responseBody);
                        if (createdId) {
                            await mapLocalToServerId(item.payload.entityRef.localId, createdId);
                        }
                    }

                    await cleanupPhotoBlobs(item);
                    await updateQueueItem(item.id, { status: 'SENT' });
                    queryClient.invalidateQueries({ queryKey: ['map', 'damage-points'] });
                    queryClient.invalidateQueries({ queryKey: ['damageAssessments'] });
                    window.dispatchEvent(new CustomEvent(OFFLINE_SYNC_ITEM_SYNCED_EVENT, {
                        detail: { type: item.type, itemId: item.id },
                    }));
                    continue;
                }

                if (response.status >= 400 && response.status < 500) {
                    // Kalıcı hata (validasyon, yetki vb.) — tekrar denemenin anlamı yok.
                    const errorBody = await response.json().catch(() => undefined);
                    await updateQueueItem(item.id, {
                        status: 'FAILED',
                        retryCount: MAX_RETRIES,
                        lastError: errorBody?.message ?? `HTTP ${response.status}: ${response.statusText}`,
                    });
                    continue;
                }

                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            } catch (err) {
                // Network hatası veya 5xx — kontrollü retry.
                const newRetry = item.retryCount + 1;
                const error = err instanceof Error ? err.message : String(err);

                await updateQueueItem(item.id, {
                    status: newRetry >= MAX_RETRIES ? 'FAILED' : 'PENDING',
                    retryCount: newRetry,
                    lastError: error,
                });
            }
        }
    } finally {
        syncInProgress = false;
        useOfflineStore.getState().setIsSyncing(false);
        await refreshQueueState();
    }
}

export function initSyncService(): () => void {
    const handleOnline = () => {
        useOfflineStore.getState().setOnline(true);
        processQueue();
    };
    const handleOffline = () => {
        useOfflineStore.getState().setOnline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Sync on app open if already online
    if (navigator.onLine) {
        processQueue();
    }

    // Refresh queue state on init
    refreshQueueState();

    return () => {
        window.removeEventListener('online', handleOnline);
        window.removeEventListener('offline', handleOffline);
    };
}
