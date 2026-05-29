import { create } from 'zustand';
import type { OfflineQueueItem } from '@/lib/offlineDb';

interface OfflineState {
    isOnline: boolean;
    isSyncing: boolean;
    queueItems: OfflineQueueItem[];
    setOnline: (v: boolean) => void;
    setIsSyncing: (v: boolean) => void;
    setQueueItems: (items: OfflineQueueItem[]) => void;
}

export const useOfflineStore = create<OfflineState>((set) => ({
    isOnline: typeof navigator !== 'undefined' ? navigator.onLine : true,
    isSyncing: false,
    queueItems: [],
    setOnline: (isOnline) => set({ isOnline }),
    setIsSyncing: (isSyncing) => set({ isSyncing }),
    setQueueItems: (queueItems) => set({ queueItems }),
}));
