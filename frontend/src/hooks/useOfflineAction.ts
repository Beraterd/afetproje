import { useCallback } from 'react';
import { enqueue, storePhotoBlob } from '@/lib/offlineQueue';
import { refreshQueueState } from '@/lib/syncService';
import type { QueueItemType, QueueItemPayload, OfflineQueueItem, EntityRef } from '@/lib/offlineDb';

interface OfflineActionOptions {
    type: QueueItemType;
    /** Sunucu tarafındaki gerçek endpoint. Varlık henüz offline oluşturulmuşsa `{entityId}`
     *  placeholder'ı kullanılabilir — CREATE senkronize olduğunda otomatik çözümlenir. */
    endpoint: string;
    method?: QueueItemPayload['method'];
    bodyKind?: QueueItemPayload['bodyKind'];
    authRequired?: boolean;
    entityRef?: EntityRef;
    /** Yalnızca bodyKind 'multipart' iken kullanılır — offline'da Blob olarak saklanır. */
    photos?: File[];
    photoFieldName?: string;
}

interface OfflineActionResult {
    queued: boolean;
    clientGeneratedId: string;
    item?: OfflineQueueItem;
}

/** Fotoğraflar senkronize olana kadar cihazda (IndexedDB) tutulur — normal upload'ın aksine
 *  kalıcı yer kaplar. Kotanın büyük kısmı doluysa kullanıcıyı açıkça uyar, sessizce
 *  IndexedDB'yi şişirip belirsiz hatalara yol açma. */
async function ensureStorageQuota(incomingBytes: number): Promise<void> {
    if (!navigator.storage?.estimate) return;
    try {
        const { usage = 0, quota = 0 } = await navigator.storage.estimate();
        if (quota > 0 && usage + incomingBytes > quota * 0.9) {
            throw new Error(
                'Cihaz depolama alanı yetersiz. Bağlantı gelmeden önce bekleyen fotoğrafların ' +
                'senkronize olmasını bekleyin veya daha az fotoğrafla tekrar deneyin.',
            );
        }
    } catch (err) {
        if (err instanceof Error && err.message.startsWith('Cihaz depolama')) throw err;
        // estimate() itself failing shouldn't block the offline action.
    }
}

/**
 * Returns an `execute` function that tries an online API call first.
 * When the device is offline the action is saved to IndexedDB (including any photos,
 * stored as Blobs) and synced automatically once connectivity is restored.
 *
 * Usage:
 *   const { execute } = useOfflineAction();
 *   const result = await execute(
 *       { type: 'DAMAGE_ASSESSMENT_CREATE', endpoint: '/damage-assessments', bodyKind: 'multipart', photos },
 *       formFields,
 *       async (clientGeneratedId) => { await createDamageAssessment({ ...formFields, clientGeneratedId }, photos); },
 *   );
 *   if (result.queued) showToast('Çevrimdışı olarak kaydedildi');
 */
export function useOfflineAction() {
    const execute = useCallback(
        async (
            options: OfflineActionOptions,
            body: Record<string, unknown>,
            onlineAction: (clientGeneratedId: string) => Promise<unknown>,
            presetClientGeneratedId?: string,
        ): Promise<OfflineActionResult> => {
            const clientGeneratedId = presetClientGeneratedId ?? crypto.randomUUID();

            if (!navigator.onLine) {
                if (options.photos?.length) {
                    await ensureStorageQuota(options.photos.reduce((sum, f) => sum + f.size, 0));
                }
                const photoRefs = options.photos?.length
                    ? await Promise.all(
                          options.photos.map((file) => storePhotoBlob(file, options.photoFieldName ?? 'photos')),
                      )
                    : undefined;

                const item = await enqueue(
                    options.type,
                    {
                        endpoint: options.endpoint,
                        method: options.method ?? 'POST',
                        bodyKind: options.bodyKind ?? 'json',
                        body: { ...body, clientGeneratedId },
                        photoRefs,
                        authRequired: options.authRequired ?? true,
                        entityRef: options.entityRef,
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
