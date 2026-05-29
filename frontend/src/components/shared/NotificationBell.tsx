import React, { useEffect, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell, CheckCheck, X } from 'lucide-react';
import { cn } from '@/utils/cn';
import { queryKeys } from '@/utils/queryKeys';
import {
    getNotifications,
    getUnreadCount,
    markAllNotificationsRead,
    markNotificationRead,
} from '@/api/notifications.api';
import { NotificationResponse, NotificationType } from '@/types';
import { useAuthStore } from '@/store/authStore';
import { formatDistanceToNow } from 'date-fns';
import { tr } from 'date-fns/locale';

const COORDINATOR_ROLES = ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'] as const;
const POLL_INTERVAL_MS = 30_000;

const TYPE_LABELS: Record<NotificationType, string> = {
    DOCUMENT_APPROVAL:       'Belge Onayı',
    RESOURCE_REQUEST:        'Kaynak Talebi',
    TEAM_NEED:               'Ekip İhtiyacı',
    DAMAGE_REPORT:           'Hasar Tespiti',
    NEW_EARTHQUAKE:          'Yeni Deprem',
    SIMULATION_RESULT:       'Simülasyon',
    MESSAGE_DELIVERY_STATUS: 'SMS/Mail Durumu',
};

const TYPE_COLORS: Record<NotificationType, string> = {
    DOCUMENT_APPROVAL:       'bg-blue-100 text-blue-700',
    RESOURCE_REQUEST:        'bg-orange-100 text-orange-700',
    TEAM_NEED:               'bg-purple-100 text-purple-700',
    DAMAGE_REPORT:           'bg-yellow-100 text-yellow-700',
    NEW_EARTHQUAKE:          'bg-red-100 text-red-700',
    SIMULATION_RESULT:       'bg-indigo-100 text-indigo-700',
    MESSAGE_DELIVERY_STATUS: 'bg-gray-100 text-gray-600',
};

type FilterTab = 'ALL' | NotificationType | 'UNREAD';

const FILTER_TABS: { key: FilterTab; label: string }[] = [
    { key: 'ALL',                    label: 'Tümü' },
    { key: 'UNREAD',                 label: 'Okunmamış' },
    { key: 'NEW_EARTHQUAKE',         label: 'Deprem' },
    { key: 'DOCUMENT_APPROVAL',      label: 'Belge Onayı' },
    { key: 'RESOURCE_REQUEST',       label: 'Kaynak' },
    { key: 'TEAM_NEED',              label: 'Ekip' },
    { key: 'DAMAGE_REPORT',          label: 'Hasar' },
    { key: 'SIMULATION_RESULT',      label: 'Simülasyon' },
    { key: 'MESSAGE_DELIVERY_STATUS',label: 'Mail/SMS' },
];

export const NotificationBell: React.FC = () => {
    const user = useAuthStore((s) => s.user);
    const queryClient = useQueryClient();
    const [open, setOpen] = useState(false);
    const [activeFilter, setActiveFilter] = useState<FilterTab>('ALL');
    const panelRef = useRef<HTMLDivElement>(null);

    // ⚠ Rules of Hooks: tüm hook'lar koşulsuz çağrılmalı — early return AŞAĞIDA.
    const isCoordinator = !!user && (COORDINATOR_ROLES as readonly string[]).includes(user.role);

    const notifParams = {
        page: 0,
        size: 20,
        type:   activeFilter === 'ALL' || activeFilter === 'UNREAD'
                    ? undefined
                    : activeFilter as NotificationType,
        isRead: activeFilter === 'UNREAD' ? false : undefined,
    };

    // Okunmamış sayısı — sadece koordinatörler için polling
    const { data: countData } = useQuery({
        queryKey: queryKeys.notifications.unreadCount(),
        queryFn: getUnreadCount,
        enabled: isCoordinator,
        refetchInterval: isCoordinator ? POLL_INTERVAL_MS : false,
        refetchOnMount: true,
        refetchIntervalInBackground: false,
    });

    // Bildirim listesi — dropdown açıkken ve koordinatörse fetch
    const { data: notifData, isLoading } = useQuery({
        queryKey: queryKeys.notifications.list(notifParams),
        queryFn: () => getNotifications(notifParams),
        enabled: isCoordinator && open,
        refetchInterval: isCoordinator && open ? POLL_INTERVAL_MS : false,
        refetchOnMount: true,
    });

    const markAllMutation = useMutation({
        mutationFn: markAllNotificationsRead,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
        },
    });

    const markOneMutation = useMutation({
        mutationFn: markNotificationRead,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
        },
    });

    // Panel dışına tıklanınca kapat — temizleme fonksiyonu memory leak'i önler
    useEffect(() => {
        if (!isCoordinator) return;
        const handler = (e: MouseEvent) => {
            if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, [isCoordinator]);

    // Early return hook'lardan SONRA — Rules of Hooks ihlali önlendi
    if (!isCoordinator) return null;

    const unread = countData?.unreadCount ?? 0;
    const notifications = notifData?.content ?? [];

    const handleNotifClick = (n: NotificationResponse) => {
        if (!n.isRead) {
            markOneMutation.mutate(n.id);
        }
    };

    return (
        <div className="relative" ref={panelRef}>
            {/* Zil ikonu */}
            <button
                onClick={() => setOpen((v) => !v)}
                className="relative flex items-center justify-center h-9 w-9 rounded-full hover:bg-gray-100 transition-colors"
                aria-label="Bildirimler"
            >
                <Bell className="h-5 w-5 text-gray-600" />
                {unread > 0 && (
                    <span className="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white">
                        {unread > 99 ? '99+' : unread}
                    </span>
                )}
            </button>

            {/* Dropdown panel */}
            {open && (
                <div className="absolute right-0 mt-2 w-96 max-h-[560px] flex flex-col bg-white rounded-xl shadow-xl border border-gray-200 z-50 overflow-hidden">
                    {/* Başlık */}
                    <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
                        <h3 className="text-sm font-semibold text-gray-900">
                            Bildirimler {unread > 0 && (
                                <span className="ml-1.5 inline-flex items-center rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
                                    {unread} okunmamış
                                </span>
                            )}
                        </h3>
                        <div className="flex items-center gap-2">
                            {unread > 0 && (
                                <button
                                    onClick={() => markAllMutation.mutate()}
                                    disabled={markAllMutation.isPending}
                                    title="Tümünü okundu işaretle"
                                    className="text-xs text-brand-600 hover:text-brand-800 flex items-center gap-1 disabled:opacity-50"
                                >
                                    <CheckCheck className="h-3.5 w-3.5" />
                                    Tümünü oku
                                </button>
                            )}
                            <button onClick={() => setOpen(false)} className="text-gray-400 hover:text-gray-600">
                                <X className="h-4 w-4" />
                            </button>
                        </div>
                    </div>

                    {/* Filtre sekmeleri */}
                    <div className="flex gap-1 overflow-x-auto px-3 py-2 border-b border-gray-100 scrollbar-hide">
                        {FILTER_TABS.map((tab) => (
                            <button
                                key={tab.key}
                                onClick={() => setActiveFilter(tab.key)}
                                className={cn(
                                    'shrink-0 rounded-full px-2.5 py-1 text-xs font-medium transition-colors',
                                    activeFilter === tab.key
                                        ? 'bg-brand-600 text-white'
                                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                )}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>

                    {/* Bildirim listesi */}
                    <div className="overflow-y-auto flex-1">
                        {isLoading && (
                            <div className="flex justify-center items-center py-8">
                                <div className="h-5 w-5 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
                            </div>
                        )}

                        {!isLoading && notifications.length === 0 && (
                            <div className="flex flex-col items-center justify-center py-10 text-gray-400">
                                <Bell className="h-8 w-8 mb-2 opacity-30" />
                                <p className="text-sm">Bildirim bulunamadı</p>
                            </div>
                        )}

                        {!isLoading && notifications.map((n) => (
                            <button
                                key={n.id}
                                onClick={() => handleNotifClick(n)}
                                className={cn(
                                    'w-full text-left px-4 py-3 border-b border-gray-50 hover:bg-gray-50 transition-colors',
                                    !n.isRead && 'bg-blue-50/40'
                                )}
                            >
                                <div className="flex items-start gap-3">
                                    <div className="mt-1.5 shrink-0">
                                        {!n.isRead
                                            ? <span className="block h-2 w-2 rounded-full bg-blue-500" />
                                            : <span className="block h-2 w-2 rounded-full bg-transparent" />
                                        }
                                    </div>

                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-0.5">
                                            <span className={cn(
                                                'inline-block shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold',
                                                TYPE_COLORS[n.type]
                                            )}>
                                                {TYPE_LABELS[n.type]}
                                            </span>
                                        </div>
                                        <p className={cn(
                                            'text-sm leading-snug',
                                            n.isRead ? 'text-gray-600 font-normal' : 'text-gray-900 font-medium'
                                        )}>
                                            {n.title}
                                        </p>
                                        {n.message && (
                                            <p className="mt-0.5 text-xs text-gray-500 line-clamp-2">{n.message}</p>
                                        )}
                                        <p className="mt-1 text-[11px] text-gray-400">
                                            {formatDistanceToNow(new Date(n.createdAt), { addSuffix: true, locale: tr })}
                                        </p>
                                    </div>
                                </div>
                            </button>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};
