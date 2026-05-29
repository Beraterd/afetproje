import {
    getPendingItems,
    getItemsByStatus,
    getAllQueueItems,
    updateQueueItem,
} from './offlineQueue';
import { useOfflineStore } from '@/store/offlineStore';
import { useAuthStore } from '@/store/authStore';

const MAX_RETRIES = 5;
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '') + '/api';

let syncInProgress = false;

export async function refreshQueueState(): Promise<void> {
    const items = await getAllQueueItems();
    useOfflineStore.getState().setQueueItems(items);
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
            await updateQueueItem(item.id, { status: 'SENDING' });

            try {
                const token =
                    useAuthStore.getState().accessToken ??
                    localStorage.getItem('afet_token');

                const headers: Record<string, string> = {
                    'Content-Type': 'application/json',
                };
                if (item.payload.authRequired && token) {
                    headers['Authorization'] = `Bearer ${token}`;
                }

                const response = await fetch(`${API_BASE}${item.payload.endpoint}`, {
                    method: item.payload.method,
                    headers,
                    body: JSON.stringify(item.payload.body),
                });

                if (response.status === 401) {
                    // Token expired — mark all remaining items as failed
                    await updateQueueItem(item.id, {
                        status: 'FAILED',
                        retryCount: MAX_RETRIES,
                        lastError: 'Oturum süresi doldu. Lütfen tekrar giriş yapın.',
                    });
                    break;
                }

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }

                await updateQueueItem(item.id, { status: 'SENT' });
            } catch (err) {
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
