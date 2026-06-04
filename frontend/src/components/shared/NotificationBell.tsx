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
import { notificationTypeLabels, formatNotificationType } from '@/utils/labels';

const COORDINATOR_ROLES = ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'] as const;
const POLL_INTERVAL_MS = 30_000;

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
                <div
                    className={cn(
                        // Mobil: viewport'a sabit, sağ/sol 12px boşlukla — taşma yok, ortalı kalır
                        'fixed left-3 right-3 top-[4.5rem] z-50 max-h-[calc(100vh-90px)]',
                        // Masaüstü: zile göre sağa yaslı dropdown, sabit genişlik (dar pencerede taşmaz)
                        'sm:absolute sm:left-auto sm:right-0 sm:top-full sm:mt-2 sm:w-[440px] sm:max-w-[calc(100vw-24px)] sm:max-h-[560px]',
                        'flex flex-col bg-white rounded-xl shadow-xl border border-gray-200 overflow-hidden'
                    )}
                >
                    {/* Sabit filtre bloğu: başlık + sekmeler. Liste alanına dahil değildir, scroll etmez. */}
                    <div className="shrink-0 border-b border-gray-200 bg-white">
                        {/* Başlık — dar ekranda butonlar alt satıra sarılır, üst üste binmez */}
                        <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 px-4 py-3">
                            <h3 className="flex items-center gap-1.5 min-w-0 text-sm font-semibold text-gray-900">
                                <span>Bildirimler</span>
                                {unread > 0 && (
                                    <span className="inline-flex shrink-0 items-center rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
                                        {unread} okunmamış
                                    </span>
                                )}
                            </h3>
                            <div className="flex items-center gap-2 shrink-0">
                                {unread > 0 && (
                                    <button
                                        onClick={() => markAllMutation.mutate()}
                                        disabled={markAllMutation.isPending}
                                        title="Tümünü okundu işaretle"
                                        className="flex items-center gap-1 whitespace-nowrap text-xs text-brand-600 hover:text-brand-800 disabled:opacity-50"
                                    >
                                        <CheckCheck className="h-3.5 w-3.5" />
                                        Tümünü oku
                                    </button>
                                )}
                                <button
                                    onClick={() => setOpen(false)}
                                    aria-label="Kapat"
                                    className="p-0.5 text-gray-400 hover:text-gray-600"
                                >
                                    <X className="h-4 w-4" />
                                </button>
                            </div>
                        </div>

                        {/* Filtre sekmeleri — belirgin chip'ler, tek satır, taşarsa yatay scroll */}
                        <div className="flex flex-nowrap items-center gap-2 overflow-x-auto whitespace-nowrap px-4 pb-3 min-h-[40px] scrollbar-thin">
                            {FILTER_TABS.map((tab) => (
                                <button
                                    key={tab.key}
                                    onClick={() => setActiveFilter(tab.key)}
                                    className={cn(
                                        'inline-flex shrink-0 items-center whitespace-nowrap rounded-full border px-3 py-1.5 min-h-[32px] text-[13px] font-medium transition-colors',
                                        activeFilter === tab.key
                                            ? 'border-brand-600 bg-brand-600 text-white shadow-sm'
                                            : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
                                    )}
                                >
                                    {tab.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Bildirim listesi — her bildirim ayrı kart, dikey scroll */}
                    <div className="flex-1 space-y-2 overflow-y-auto bg-gray-50/50 p-2">
                        {isLoading && (
                            <div className="flex justify-center items-center py-8">
                                <div className="h-5 w-5 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
                            </div>
                        )}

                        {!isLoading && notifications.length === 0 && (
                            <div className="flex flex-col items-center justify-center py-10 text-gray-400">
                                <Bell className="h-8 w-8 mb-2 opacity-30" />
                                <p className="text-sm">Bildirim yok</p>
                            </div>
                        )}

                        {!isLoading && notifications.map((n) => (
                            <button
                                key={n.id}
                                onClick={() => handleNotifClick(n)}
                                className={cn(
                                    'w-full rounded-xl border p-3.5 text-left transition-colors',
                                    !n.isRead
                                        ? 'border-blue-200 bg-blue-50/60 hover:bg-blue-50'
                                        : 'border-gray-100 bg-white hover:bg-gray-50'
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
                                        <div className="mb-1 flex items-center gap-2">
                                            <span className={cn(
                                                'inline-block shrink-0 rounded px-2 py-0.5 text-[11px] font-semibold',
                                                TYPE_COLORS[n.type]
                                            )}>
                                                {notificationTypeLabels[n.type] ?? formatNotificationType(n.type)}
                                            </span>
                                        </div>
                                        <p
                                            title={n.title}
                                            className={cn(
                                                'text-sm leading-snug break-words line-clamp-2',
                                                n.isRead ? 'text-gray-600 font-normal' : 'text-gray-900 font-medium'
                                            )}
                                        >
                                            {n.title}
                                        </p>
                                        {n.message && (
                                            <p className="mt-1 text-xs text-gray-500 break-words line-clamp-3">{n.message}</p>
                                        )}
                                        <p className="mt-1.5 text-[11px] text-gray-400">
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
