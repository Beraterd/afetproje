import { useCallback } from 'react';
import { enqueue } from '@/lib/offlineQueue';
import { refreshQueueState } from '@/lib/syncService';
import type { QueueItemType, QueueItemPayload, OfflineQueueItem } from '@/lib/offlineDb';

interface OfflineActionOptions {
    type: QueueItemType;
    endpoint: string;
    method?: QueueItemPayload['method'];
    authRequired?: boolean;
}

interface OfflineActionResult {
    queued: boolean;
    clientGeneratedId: string;
    item?: OfflineQueueItem;
}

/**
 * Returns an `execute` function that tries an online API call first.
 * When the device is offline the action is saved to IndexedDB and
 * synced automatically once connectivity is restored.
 *
 * Usage:
 *   const { execute } = useOfflineAction();
 *   const result = await execute(
 *       { type: 'USER_STATUS_REPORT', endpoint: '/users/me/emergency-status-messages/send' },
 *       payload,
 *       async (clientGeneratedId) => { await api.sendStatusMessage({ ...payload, clientGeneratedId }); },
 *   );
 *   if (result.queued) showToast('Çevrimdışı olarak kaydedildi');
 */
export function useOfflineAction() {
    const execute = useCallback(
        async (
            options: OfflineActionOptions,
            body: Record<string, unknown>,
            onlineAction: (clientGeneratedId: string) => Promise<unknown>,
        ): Promise<OfflineActionResult> => {
            const clientGeneratedId = crypto.randomUUID();

            if (!navigator.onLine) {
                const item = await enqueue(
                    options.type,
                    {
                        endpoint: options.endpoint,
                        method: options.method ?? 'POST',
                        body: { ...body, clientGeneratedId },
                        authRequired: options.authRequired ?? true,
                    },
                    clientGeneratedId,
                );
                await refreshQueueState();
                return { queued: true, clientGeneratedId, item };
            }

            await onlineAction(clientGeneratedId);
            return { queued: false, clientGeneratedId };
        },
        [],
    );

    return { execute };
}
