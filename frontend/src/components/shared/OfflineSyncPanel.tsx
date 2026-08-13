import React, { useEffect } from 'react';
import { Clock, CheckCircle2, XCircle, RefreshCw, Trash2, Loader2 } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { tr } from 'date-fns/locale';
import { useOfflineStore } from '@/store/offlineStore';
import { removeQueueItem, clearQueue, updateQueueItem } from '@/lib/offlineQueue';
import { processQueue, refreshQueueState } from '@/lib/syncService';
import type { QueueItemStatus } from '@/lib/offlineDb';

const TYPE_LABELS: Record<string, string> = {
    DAMAGE_ASSESSMENT_CREATE: 'Hasar Tespiti Oluşturma',
    DAMAGE_ASSESSMENT_VERIFY: 'Hasar Tespiti Doğrulama',
    TASK_FIELD_VERIFIED: 'Sahada Doğrulama',
    TASK_COMPLETE: 'Görev Tamamlama',
    TASK_PHOTO_UPLOAD: 'Saha Fotoğrafı',
};

const SENT_AUTO_CLEAR_MS = 5000;

const StatusIcon: React.FC<{ status: QueueItemStatus }> = ({ status }) => {
    switch (status) {
        case 'PENDING':  return <Clock className="h-4 w-4 text-amber-500" />;
        case 'SENDING':  return <Loader2 className="h-4 w-4 animate-spin text-blue-500" />;
        case 'SENT':     return <CheckCircle2 className="h-4 w-4 text-green-500" />;
        case 'FAILED':   return <XCircle className="h-4 w-4 text-red-500" />;
    }
};

const STATUS_LABELS: Record<QueueItemStatus, string> = {
    PENDING: 'Bekliyor',
    SENDING: 'Gönderiliyor',
    SENT: 'Tamamlandı',
    FAILED: 'Hata',
};

export const OfflineSyncPanel: React.FC = () => {
    const { queueItems, isOnline, isSyncing } = useOfflineStore();

    // Başarıyla senkronize edilen kayıtlar kısa süre "Tamamlandı" olarak görünüp otomatik silinir.
    useEffect(() => {
        const sentIds = queueItems.filter((i) => i.status === 'SENT').map((i) => i.id);
        if (sentIds.length === 0) return;
        const timer = setTimeout(async () => {
            for (const id of sentIds) await removeQueueItem(id);
            await refreshQueueState();
        }, SENT_AUTO_CLEAR_MS);
        return () => clearTimeout(timer);
    }, [queueItems]);

    if (queueItems.length === 0) {
        return (
            <p className="text-sm text-gray-500 text-center py-4">
                Bekleyen çevrimdışı işlem yok.
            </p>
        );
    }

    const handleRetry = async () => {
        await processQueue();
    };

    const handleRetryOne = async (id: string) => {
        await updateQueueItem(id, { status: 'PENDING', retryCount: 0, lastError: undefined });
        await refreshQueueState();
        await processQueue();
    };

    const handleRetryAllFailed = async () => {
        const failed = queueItems.filter((i) => i.status === 'FAILED');
        for (const item of failed) {
            await updateQueueItem(item.id, { status: 'PENDING', retryCount: 0, lastError: undefined });
        }
        await refreshQueueState();
        await processQueue();
    };

    const handleDelete = async (id: string) => {
        await removeQueueItem(id);
        await refreshQueueState();
    };

    const handleClearSent = async () => {
        for (const item of queueItems.filter((i) => i.status === 'SENT')) {
            await removeQueueItem(item.id);
        }
        await refreshQueueState();
    };

    const handleClearAll = async () => {
        await clearQueue();
        await refreshQueueState();
    };

    const sentCount = queueItems.filter((i) => i.status === 'SENT').length;
    const failedCount = queueItems.filter((i) => i.status === 'FAILED').length;

    return (
        <div className="space-y-3">
            <div className="flex items-center justify-between">
                <h4 className="text-sm font-semibold text-gray-900">
                    Çevrimdışı Kuyruk ({queueItems.length})
                </h4>
                <div className="flex flex-wrap items-center justify-end gap-2">
                    {isOnline && (
                        <button
                            onClick={handleRetry}
                            disabled={isSyncing}
                            className="flex items-center gap-1 text-xs font-medium text-brand-600 hover:text-brand-800 disabled:opacity-50"
                        >
                            <RefreshCw className={`h-3 w-3 ${isSyncing ? 'animate-spin' : ''}`} />
                            Senkronize et
                        </button>
                    )}
                    {isOnline && failedCount > 0 && (
                        <button
                            onClick={handleRetryAllFailed}
                            disabled={isSyncing}
                            className="flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                        >
                            <RefreshCw className="h-3 w-3" />
                            Tümünü Tekrar Dene
                        </button>
                    )}
                    {sentCount > 0 && (
                        <button
                            onClick={handleClearSent}
                            className="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700"
                        >
                            <Trash2 className="h-3 w-3" />
                            Gönderilenleri sil
                        </button>
                    )}
                    <button
                        onClick={handleClearAll}
                        className="text-xs text-red-500 hover:text-red-700"
                    >
                        Tümünü temizle
                    </button>
                </div>
            </div>

            <div className="space-y-2 max-h-80 overflow-y-auto">
                {queueItems.map((item) => (
                    <div
                        key={item.id}
                        className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg border border-gray-100"
                    >
                        <div className="mt-0.5 shrink-0">
                            <StatusIcon status={item.status} />
                        </div>
                        <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                                <p className="text-sm font-medium text-gray-900">
                                    {TYPE_LABELS[item.type] ?? item.type}
                                </p>
                                <span className="text-[11px] text-gray-400">— {STATUS_LABELS[item.status]}</span>
                            </div>
                            <p className="text-xs text-gray-500 mt-0.5">
                                {formatDistanceToNow(new Date(item.createdAt), {
                                    addSuffix: true,
                                    locale: tr,
                                })}
                                {item.retryCount > 0 && ` · ${item.retryCount} deneme`}
                            </p>
                            {item.lastError && (
                                <p className="text-xs text-red-500 mt-0.5 truncate" title={item.lastError}>
                                    {item.lastError}
                                </p>
                            )}
                            {item.status === 'FAILED' && isOnline && (
                                <button
                                    onClick={() => handleRetryOne(item.id)}
                                    className="mt-1 flex items-center gap-1 text-xs font-medium text-brand-600 hover:text-brand-800"
                                >
                                    <RefreshCw className="h-3 w-3" />
                                    Tekrar Dene
                                </button>
                            )}
                        </div>
                        <button
                            onClick={() => handleDelete(item.id)}
                            className="shrink-0 text-gray-300 hover:text-gray-500 mt-0.5"
                            title="Kaldır"
                        >
                            <XCircle className="h-3.5 w-3.5" />
                        </button>
                    </div>
                ))}
            </div>
        </div>
    );
};
