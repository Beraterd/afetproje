import React, { useEffect, useRef, useState } from 'react';
import { CloudOff, Cloud, RefreshCw, X } from 'lucide-react';
import { cn } from '@/utils/cn';
import { useOfflineStore } from '@/store/offlineStore';
import { OfflineSyncPanel } from './OfflineSyncPanel';

/** Header'daki "☁ N işlem bekliyor" göstergesi — tıklanınca kuyruğu gösteren bir panel açar.
 *  Kuyruk boşken ve çevrimiçiyken hiçbir şey göstermez, sayfada gereksiz yer kaplamaz. */
export const OfflineSyncIndicator: React.FC = () => {
    const { isOnline, isSyncing, queueItems } = useOfflineStore();
    const [open, setOpen] = useState(false);
    const panelRef = useRef<HTMLDivElement>(null);

    const pendingCount = queueItems.filter((i) => i.status === 'PENDING' || i.status === 'SENDING').length;
    const failedCount = queueItems.filter((i) => i.status === 'FAILED').length;
    const totalCount = pendingCount + failedCount;

    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    // Çevrimdışıyken kuyruk boş olsa bile durumu göster; çevrimiçiyken ve kuyruk boşsa gizle.
    if (isOnline && totalCount === 0) return null;

    return (
        <div className="relative" ref={panelRef}>
            <button
                onClick={() => setOpen((v) => !v)}
                className={cn(
                    'relative flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold transition-colors',
                    !isOnline
                        ? 'bg-amber-100 text-amber-800 hover:bg-amber-200'
                        : failedCount > 0
                            ? 'bg-red-100 text-red-700 hover:bg-red-200'
                            : 'bg-blue-100 text-blue-700 hover:bg-blue-200'
                )}
                aria-label="Çevrimdışı senkronizasyon durumu"
            >
                {isSyncing ? (
                    <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                ) : isOnline ? (
                    <Cloud className="h-3.5 w-3.5" />
                ) : (
                    <CloudOff className="h-3.5 w-3.5" />
                )}
                <span className="hidden sm:inline">
                    {!isOnline ? 'Çevrimdışı' : isSyncing ? 'Gönderiliyor' : `${totalCount} işlem bekliyor`}
                </span>
                {totalCount > 0 && (
                    <span className="sm:hidden">{totalCount}</span>
                )}
            </button>

            {open && (
                <div
                    className={cn(
                        'fixed left-3 right-3 top-[4.5rem] z-50 max-h-[calc(100vh-90px)]',
                        'sm:absolute sm:left-auto sm:right-0 sm:top-full sm:mt-2 sm:w-96 sm:max-w-[calc(100vw-24px)] sm:max-h-[560px]',
                        'flex flex-col bg-white rounded-xl shadow-xl border border-gray-200 overflow-hidden'
                    )}
                >
                    <div className="shrink-0 flex items-center justify-between border-b border-gray-200 px-4 py-3">
                        <h3 className="text-sm font-semibold text-gray-900">Çevrimdışı Senkronizasyon</h3>
                        <button
                            onClick={() => setOpen(false)}
                            aria-label="Kapat"
                            className="p-0.5 text-gray-400 hover:text-gray-600"
                        >
                            <X className="h-4 w-4" />
                        </button>
                    </div>
                    <div className="flex-1 overflow-y-auto bg-gray-50/50 p-3">
                        {!isOnline && (
                            <p className="mb-3 text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
                                Çevrimdışısınız. Yeni işlemler cihazda kaydedilir ve bağlantı geldiğinde
                                otomatik olarak gönderilir.
                            </p>
                        )}
                        <OfflineSyncPanel />
                    </div>
                </div>
            )}
        </div>
    );
};
