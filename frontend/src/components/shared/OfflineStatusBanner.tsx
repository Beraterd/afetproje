import React from 'react';
import { WifiOff, RefreshCw, AlertTriangle } from 'lucide-react';
import { useOfflineStore } from '@/store/offlineStore';
import { processQueue } from '@/lib/syncService';

export const OfflineStatusBanner: React.FC = () => {
    const { isOnline, queueItems, isSyncing } = useOfflineStore();

    const pendingCount = queueItems.filter((i) => i.status === 'PENDING').length;
    const failedCount = queueItems.filter((i) => i.status === 'FAILED').length;

    if (isOnline && pendingCount === 0 && failedCount === 0) return null;

    if (!isOnline) {
        return (
            <div className="bg-amber-50 border-b border-amber-200 px-4 py-2 flex items-center justify-between gap-3 text-sm">
                <div className="flex items-center gap-2 text-amber-800 min-w-0">
                    <WifiOff className="h-4 w-4 shrink-0" />
                    <span className="font-medium truncate">
                        Çevrimdışısınız. Yeni işlemler bağlantı geldiğinde gönderilecek.
                    </span>
                </div>
                {pendingCount > 0 && (
                    <span className="shrink-0 rounded-full bg-amber-200 px-2 py-0.5 text-xs font-semibold text-amber-900">
                        {pendingCount} bekleyen
                    </span>
                )}
            </div>
        );
    }

    if (isSyncing) {
        return (
            <div className="bg-blue-50 border-b border-blue-200 px-4 py-2 flex items-center gap-2 text-sm text-blue-800">
                <RefreshCw className="h-4 w-4 shrink-0 animate-spin" />
                <span>Bekleyen {pendingCount} işlem gönderiliyor...</span>
            </div>
        );
    }

    if (failedCount > 0) {
        return (
            <div className="bg-red-50 border-b border-red-200 px-4 py-2 flex items-center justify-between gap-3 text-sm">
                <div className="flex items-center gap-2 text-red-700">
                    <AlertTriangle className="h-4 w-4 shrink-0" />
                    <span>{failedCount} işlem gönderilemedi.</span>
                </div>
                <button
                    onClick={() => processQueue()}
                    className="shrink-0 text-xs font-medium text-red-600 hover:text-red-800 underline"
                >
                    Tekrar dene
                </button>
            </div>
        );
    }

    return null;
};
