import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import {
    getEventById, joinEvent, leaveEvent, closeEvent,
    completeEvent, updateEvent, getEventParticipants,
} from '@/api/events.api';
import {
    createEventTeamRecommendation,
    getLatestEventTeamRecommendation,
    approveTeamRecommendation,
    rejectTeamRecommendation,
} from '@/api/teamRecommendations.api';
import { queryKeys } from '@/utils/queryKeys';
import { Button, Badge, LoadingSpinner, ConfirmationDialog } from '@/components/ui';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/components/shared/ToastProvider';
import {
    MapPin, Users, Calendar, AlertTriangle, Edit2,
    CheckCircle, Sparkles, Mail, X, UserCheck,
} from 'lucide-react';
import { format } from 'date-fns';
import { EventStatus } from '@/types';
import type { RecommendedMemberResponse, TeamRecommendationResponse, EventParticipantResponse } from '@/types';

const TEAM_TR: Record<string, string> = {
    SEARCH_RESCUE: 'Arama Kurtarma Ekibi',
    FOOD_WATER: 'Yemek ve İçme Suyu Dağıtım Ekibi',
    EVACUATION: 'Tahliye Ekibi',
    COMMUNICATION: 'İletişim Ekibi',
    PSYCHOSOCIAL: 'Psikososyal Destek Ekibi',
    HASAR_TESPIT_EKIBI: 'Hasar Tespit Ekibi',
    OTHER: 'Diğer',
};

const PROXIMITY_TR: Record<string, string> = {
    SAME_NEIGHBORHOOD: 'Aynı mahalle',
    SAME_DISTRICT: 'Aynı ilçe',
    NEIGHBOR_DISTRICT: 'Komşu ilçe',
    ISTANBUL_WIDE: 'İstanbul geneli',
    NONE: '-',
};

const AVAILABILITY_TR: Record<string, string> = {
    AVAILABLE: 'Müsait',
    ACTIVE_TASK: 'Aktif görevde',
    UNAVAILABLE: 'Müsait değil',
};

const PARTICIPANT_STATUS_TR: Record<string, { label: string; color: string }> = {
    JOINED:    { label: 'Katıldı',       color: 'text-green-600 bg-green-50' },
    ACCEPTED:  { label: 'Kabul Etti',    color: 'text-green-600 bg-green-50' },
    INVITED:   { label: 'Davet Edildi',  color: 'text-blue-600 bg-blue-50' },
    DECLINED:  { label: 'Reddetti',      color: 'text-red-600 bg-red-50' },
    CANCELLED: { label: 'İptal',         color: 'text-gray-500 bg-gray-50' },
};

// Bu mesajlar "zaten katıldınız" anlamına gelir → hata değil, info toast + refresh
const ALREADY_JOINED_MSGS = [
    'zaten katılıyorsunuz',
    'davet yoluyla zaten kabul',
    'already joined',
];

function isAlreadyJoinedError(msg: string): boolean {
    const lower = msg?.toLowerCase() ?? '';
    return ALREADY_JOINED_MSGS.some((s) => lower.includes(s));
}

function statusBadge(status: EventStatus) {
    switch (status) {
        case 'OPEN':
        case 'IN_PROGRESS': return <Badge variant="warning">Devam Ediyor</Badge>;
        case 'COMPLETED':   return <Badge variant="info">Tamamlandı</Badge>;
        case 'CLOSED':      return <Badge variant="neutral">İptal Edildi</Badge>;
        default:            return <Badge variant="neutral">{status}</Badge>;
    }
}

export const EventDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const queryClient = useQueryClient();
    const toast = useToast();
    const user = useAuthStore((s) => s.user);

    const [closeDialogOpen,    setCloseDialogOpen]    = useState(false);
    const [completeDialogOpen, setCompleteDialogOpen] = useState(false);
    const [editModalOpen,      setEditModalOpen]      = useState(false);
    const [editForm, setEditForm] = useState({ title: '', description: '', requiredPeople: 1 });

    // AI öneri state
    const [recommendation,  setRecommendation]  = useState<TeamRecommendationResponse | null>(null);
    const [selectedUserIds, setSelectedUserIds] = useState<Set<string>>(new Set());

    // ── Queries ────────────────────────────────────────────────────────────────

    const { data: event, isLoading, isError } = useQuery({
        queryKey: queryKeys.events.detail(id!),
        queryFn:  () => getEventById(id!),
        enabled:  !!id,
    });

    const { data: participants = [] } = useQuery({
        queryKey: queryKeys.events.participants(id!),
        queryFn:  () => getEventParticipants(id!),
        enabled:  !!id,
    });

    // §3 — Olay oluşturulduğunda öneri arka planda üretilir. Yönetici detayını açtığında
    // otomatik üretilen öneri çekilir; henüz hazır değilse (204) kısa aralıkla poll edilir.
    // Manuel "Ekip önerisini yenile" akışı (recommendMutation) korunur.
    const isManagerRole = !!user && ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'].includes(user.role);
    const autoRecQuery = useQuery({
        queryKey: ['events', id, 'latest-recommendation'],
        queryFn:  () => getLatestEventTeamRecommendation(id!),
        enabled:  !!id && !!event && (event.status === 'IN_PROGRESS' || event.status === 'OPEN') && isManagerRole,
        refetchInterval: (query) => (query.state.data ? false : 5000),
    });

    useEffect(() => {
        const data = autoRecQuery.data;
        if (data && !recommendation) {
            setRecommendation(data);
            const topN = Math.min(event?.requiredPeople ?? data.requiredTeamSize, data.recommendedPersonnel.length);
            setSelectedUserIds(new Set(data.recommendedPersonnel.slice(0, topN).map((m) => m.userId)));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [autoRecQuery.data]);

    // ── Helpers ────────────────────────────────────────────────────────────────

    const invalidateAll = () => {
        queryClient.invalidateQueries({ queryKey: queryKeys.events.detail(id!) });
        queryClient.invalidateQueries({ queryKey: queryKeys.events.participants(id!) });
        queryClient.invalidateQueries({ queryKey: queryKeys.events.all });
        queryClient.invalidateQueries({ queryKey: ['map'], exact: false });
    };

    const openEdit = () => {
        if (!event) return;
        setEditForm({
            title:         event.title,
            description:   event.description || '',
            requiredPeople: event.requiredPeople,
        });
        setEditModalOpen(true);
    };

    const toggleSelect = (userId: string) => {
        setSelectedUserIds((prev) => {
            const next = new Set(prev);
            next.has(userId) ? next.delete(userId) : next.add(userId);
            return next;
        });
    };

    // ── Mutations ──────────────────────────────────────────────────────────────

    const joinMutation = useMutation({
        mutationFn: () => joinEvent(id!),
        onSuccess: () => {
            toast.success('Olaya başarıyla katıldınız');
            invalidateAll();
        },
        onError: (err: any) => {
            const msg: string = err?.response?.data?.message || err?.message || '';
            if (isAlreadyJoinedError(msg)) {
                toast.info('Bu göreve zaten katılmışsınız.');
                invalidateAll();
            } else {
                toast.error(msg || 'Olaya katılınamadı');
            }
        },
    });

    const leaveMutation = useMutation({
        mutationFn: () => leaveEvent(id!),
        onSuccess: () => {
            toast.success('Olaydan ayrıldınız');
            invalidateAll();
        },
        onError: (err: any) => toast.error(err.message || 'Olaydan ayrılınamadı'),
    });

    const closeMutation = useMutation({
        mutationFn: () => closeEvent(id!),
        onSuccess: (data) => {
            toast.success('Olay iptal edildi');
            setCloseDialogOpen(false);
            queryClient.setQueryData(queryKeys.events.detail(id!), (old: any) =>
                old ? { ...old, status: data.status, closedAt: data.closedAt } : old
            );
            invalidateAll();
        },
        onError: (err: any) => {
            toast.error(err?.response?.data?.message || err.message || 'Olay kapatılamadı');
            setCloseDialogOpen(false);
        },
    });

    const completeMutation = useMutation({
        mutationFn: () => completeEvent(id!),
        onSuccess: (data) => {
            toast.success('Olay tamamlandı');
            setCompleteDialogOpen(false);
            queryClient.setQueryData(queryKeys.events.detail(id!), (old: any) =>
                old ? { ...old, status: data.status, closedAt: data.closedAt } : old
            );
            invalidateAll();
        },
        onError: (err: any) => {
            toast.error(err?.response?.data?.message || 'Tamamlama başarısız');
            setCompleteDialogOpen(false);
        },
    });

    const updateMutation = useMutation({
        mutationFn: () => updateEvent(id!, {
            title:         editForm.title || undefined,
            description:   editForm.description || undefined,
            requiredPeople: editForm.requiredPeople,
        }),
        onSuccess: (data) => {
            toast.success('Olay güncellendi');
            setEditModalOpen(false);
            queryClient.setQueryData(queryKeys.events.detail(id!), data);
            invalidateAll();
        },
        onError: (err: any) => toast.error(err?.response?.data?.message || 'Güncelleme başarısız'),
    });

    const recommendMutation = useMutation({
        mutationFn: () => createEventTeamRecommendation(id!),
        onSuccess: (data) => {
            setRecommendation(data);
            const topN = Math.min(
                event?.requiredPeople ?? data.requiredTeamSize,
                data.recommendedPersonnel.length
            );
            setSelectedUserIds(
                new Set(data.recommendedPersonnel.slice(0, topN).map((m: RecommendedMemberResponse) => m.userId))
            );
        },
        onError: (err: any) => toast.error(err?.response?.data?.message || 'AI öneri oluşturulamadı'),
    });

    const approveMutation = useMutation({
        mutationFn: () => approveTeamRecommendation(recommendation!.id, {
            selectedUserIds: Array.from(selectedUserIds),
        }),
        onSuccess: (data) => {
            toast.success(
                `${data.mailSentCount} kişiye görev daveti gönderildi` +
                (data.mailFailedCount > 0 ? ` (${data.mailFailedCount} mail başarısız)` : '')
            );
            if (data.mailNote) toast.error(data.mailNote);
            setRecommendation(null);
            setSelectedUserIds(new Set());
            invalidateAll();
        },
        onError: (err: any) => toast.error(err?.response?.data?.message || 'Onay başarısız'),
    });

    const rejectMutation = useMutation({
        mutationFn: () => rejectTeamRecommendation(recommendation!.id),
        onSuccess: () => {
            toast.success('Öneri reddedildi');
            setRecommendation(null);
            setSelectedUserIds(new Set());
        },
        onError: (err: any) => toast.error(err?.response?.data?.message || 'Red başarısız'),
    });

    // ── Render guards ──────────────────────────────────────────────────────────

    if (isLoading) return <LoadingSpinner label="Olay yükleniyor..." />;
    if (isError || !event) return <div className="text-red-600">Olay detayları yüklenemedi.</div>;

    const canManage  = user && ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'].includes(user.role);
    const isVolunteer = user?.role === 'VOLUNTEER';
    const isActiveEvent = event.status === 'IN_PROGRESS' || event.status === 'OPEN';

    // İki kaynaktan kontrol: backend isParticipating/@JsonProperty fix + currentUserJoined alias
    // Participants listesinden fallback: kullanıcı ID eşleşmesi
    const isJoined =
        event.isParticipating === true ||
        event.currentUserJoined === true ||
        (!!user && participants.some(
            (p: EventParticipantResponse) =>
                p.userId === user.id &&
                ['JOINED', 'ACCEPTED', 'ASSIGNED'].includes(p.status)
        ));

    // Birleşik kabul sayısı (backend: assignedVolunteers + invitationAccepted)
    const acceptedCount = event.acceptedCount ?? 0;

    // Aktif katılımcılar (participants listesinde JOINED veya ACCEPTED olanlar)
    const activeParticipants = participants.filter(
        (p: EventParticipantResponse) => p.status === 'JOINED' || p.status === 'ACCEPTED'
    );

    return (
        <div className="max-w-4xl mx-auto space-y-6">

            {/* ── Ana detay kartı ─────────────────────────────────────────── */}
            <div className="bg-white shadow sm:rounded-lg overflow-hidden border border-gray-200">
                <div className="px-4 py-5 sm:px-6 flex justify-between items-start gap-4">
                    <div className="min-w-0">
                        <div className="flex items-center gap-3 flex-wrap">
                            <h3 className="text-2xl font-bold text-gray-900">{event.title}</h3>
                            {statusBadge(event.status)}
                        </div>
                        {event.description && (
                            <p className="mt-2 max-w-2xl text-sm text-gray-500 whitespace-pre-wrap">
                                {event.description}
                            </p>
                        )}
                    </div>

                    <div className="flex flex-wrap gap-2 justify-end shrink-0">
                        {canManage && isActiveEvent && (
                            <Button variant="secondary" onClick={openEdit} leftIcon={<Edit2 className="h-4 w-4" />}>
                                Düzenle
                            </Button>
                        )}
                        {canManage && isActiveEvent && (
                            <Button
                                variant="primary"
                                onClick={() => setCompleteDialogOpen(true)}
                                leftIcon={<CheckCircle className="h-4 w-4" />}
                            >
                                Tamamlandı
                            </Button>
                        )}
                        {canManage && isActiveEvent && (
                            <Button variant="danger" onClick={() => setCloseDialogOpen(true)}>
                                İptal Et
                            </Button>
                        )}

                        {/* Gönüllü katılım butonu */}
                        {isActiveEvent && isVolunteer && (
                            isJoined ? (
                                <button
                                    disabled
                                    className="inline-flex items-center gap-2 px-4 py-2 rounded-md
                                               bg-green-50 text-green-700 border border-green-200
                                               text-sm font-medium cursor-not-allowed"
                                >
                                    <UserCheck className="h-4 w-4" />
                                    Katıldınız
                                </button>
                            ) : (
                                <Button
                                    variant="primary"
                                    onClick={() => joinMutation.mutate()}
                                    loading={joinMutation.isPending}
                                >
                                    Katıl
                                </Button>
                            )
                        )}

                        {/* Ayrıl: sadece doğrudan katılan volunteer ayrılabilir */}
                        {isActiveEvent && isVolunteer && isJoined && event.assignedVolunteers > 0 && (
                            <Button
                                variant="secondary"
                                onClick={() => leaveMutation.mutate()}
                                loading={leaveMutation.isPending}
                            >
                                Ayrıl
                            </Button>
                        )}
                    </div>
                </div>

                <div className="border-t border-gray-200 px-4 py-5 sm:p-0">
                    <dl className="sm:divide-y sm:divide-gray-200">
                        <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500 flex items-center">
                                <MapPin className="mr-2 h-4 w-4" /> Konum
                            </dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                {event.neighborhood?.districtName} / {event.neighborhood?.name}
                            </dd>
                        </div>

                        <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500 flex items-center">
                                <Users className="mr-2 h-4 w-4" /> Kabul Eden / Gerekli Kişi
                            </dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0 flex items-center gap-2">
                                <span className={acceptedCount >= event.requiredPeople
                                    ? 'text-green-600 font-semibold' : ''}>
                                    {acceptedCount}
                                </span>
                                {' / '}{event.requiredPeople} kişi
                                {acceptedCount >= event.requiredPeople && (
                                    <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">
                                        Kadro Tamamlandı
                                    </span>
                                )}
                            </dd>
                        </div>

                        <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500 flex items-center">
                                <AlertTriangle className="mr-2 h-4 w-4" /> Görevlendirilen Ekip
                            </dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                {event.team
                                ? (() => {
                                    const code = event.eventTeamCode || event.team.teamCode;
                                    const label = TEAM_TR[event.team.name] || event.team.name;
                                    return code ? `${code} — ${label}` : label;
                                })()
                                : '-'}
                            </dd>
                        </div>

                        {event.createdAt && (
                            <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                                <dt className="text-sm font-medium text-gray-500 flex items-center">
                                    <Calendar className="mr-2 h-4 w-4" /> Oluşturulma Tarihi
                                </dt>
                                <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                    {format(new Date(event.createdAt), 'dd.MM.yyyy HH:mm')}
                                </dd>
                            </div>
                        )}

                        {event.closedAt && (
                            <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                                <dt className="text-sm font-medium text-gray-500 flex items-center">
                                    <Calendar className="mr-2 h-4 w-4" />
                                    {event.status === 'COMPLETED' ? 'Tamamlanma Tarihi' : 'Kapanma Tarihi'}
                                </dt>
                                <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                    {format(new Date(event.closedAt), 'dd.MM.yyyy HH:mm')}
                                </dd>
                            </div>
                        )}

                        {event.createdBy && (
                            <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                                <dt className="text-sm font-medium text-gray-500">Oluşturan</dt>
                                <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                    {event.createdBy.firstName} {event.createdBy.lastName}
                                </dd>
                            </div>
                        )}
                    </dl>
                </div>
            </div>

            {/* ── Göreve Katılanlar ─────────────────────────────────────────── */}
            {participants.length > 0 && (
                <div className="bg-white shadow sm:rounded-lg border border-gray-200 overflow-hidden">
                    <div className="px-4 py-3 sm:px-6 bg-gray-50 border-b border-gray-200 flex items-center gap-2">
                        <Users className="h-4 w-4 text-gray-600" />
                        <h4 className="text-sm font-semibold text-gray-800">
                            Göreve Katılanlar
                            <span className="ml-2 text-xs text-gray-500 font-normal">
                                ({activeParticipants.length} aktif / {participants.length} toplam)
                            </span>
                        </h4>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200 text-sm">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-4 py-2 text-left font-medium text-gray-500">Ad Soyad</th>
                                    <th className="px-4 py-2 text-left font-medium text-gray-500">E-posta</th>
                                    <th className="px-4 py-2 text-left font-medium text-gray-500">Kaynak</th>
                                    <th className="px-4 py-2 text-left font-medium text-gray-500">Durum</th>
                                    <th className="px-4 py-2 text-left font-medium text-gray-500">Tarih</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100 bg-white">
                                {participants.map((p: EventParticipantResponse) => {
                                    const st = PARTICIPANT_STATUS_TR[p.status] ?? { label: p.status, color: 'text-gray-600 bg-gray-50' };
                                    return (
                                        <tr key={`${p.userId}-${p.source}`}>
                                            <td className="px-4 py-2 font-medium text-gray-900">
                                                {p.firstName} {p.lastName}
                                            </td>
                                            <td className="px-4 py-2 text-gray-600">{p.email}</td>
                                            <td className="px-4 py-2">
                                                <span className={`text-xs px-2 py-0.5 rounded-full ${
                                                    p.source === 'DIRECT_JOIN'
                                                        ? 'bg-purple-50 text-purple-700'
                                                        : 'bg-blue-50 text-blue-700'
                                                }`}>
                                                    {p.source === 'DIRECT_JOIN' ? 'Doğrudan' : 'AI Davet'}
                                                </span>
                                            </td>
                                            <td className="px-4 py-2">
                                                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${st.color}`}>
                                                    {st.label}
                                                </span>
                                            </td>
                                            <td className="px-4 py-2 text-gray-500 text-xs">
                                                {p.joinedAt
                                                    ? format(new Date(p.joinedAt), 'dd.MM.yyyy HH:mm')
                                                    : '-'}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* ── AI Ekip Önerisi — yalnızca aktif olay + yönetici ─────────── */}
            {isActiveEvent && canManage && (
                <div className="bg-white shadow sm:rounded-lg border border-blue-100 overflow-hidden">
                    <div className="px-4 py-3 sm:px-6 flex items-center justify-between bg-blue-50 border-b border-blue-100">
                        <div className="flex items-center gap-2">
                            <Sparkles className="h-5 w-5 text-blue-600" />
                            <h4 className="text-base font-semibold text-blue-900">AI Ekip Önerisi</h4>
                        </div>
                        {recommendation && (
                            <button
                                onClick={() => { setRecommendation(null); setSelectedUserIds(new Set()); }}
                                className="text-gray-400 hover:text-gray-600"
                            >
                                <X className="h-4 w-4" />
                            </button>
                        )}
                    </div>

                    <div className="px-4 py-4 sm:px-6">
                        {!recommendation && !recommendMutation.isPending && (
                            <div className="space-y-3">
                                <p className="text-sm text-gray-600">
                                    Bu öneri, geçmiş tamamlanan görevler ve personel uygunluğu analiz edilerek
                                    otomatik üretilir. Nihai karar yöneticiye aittir.
                                </p>
                                {autoRecQuery.isFetching && !autoRecQuery.data ? (
                                    <div className="flex items-center gap-3 py-1">
                                        <LoadingSpinner />
                                        <span className="text-sm text-gray-600">
                                            Otomatik ekip önerisi hazırlanıyor...
                                        </span>
                                    </div>
                                ) : (
                                    <p className="text-xs text-gray-500">
                                        Otomatik öneri henüz hazır değil. Dilerseniz hemen yeni bir öneri oluşturabilirsiniz.
                                    </p>
                                )}
                                <Button
                                    variant="primary"
                                    onClick={() => recommendMutation.mutate()}
                                    leftIcon={<Sparkles className="h-4 w-4" />}
                                >
                                    AI ile Kişi Öner
                                </Button>
                            </div>
                        )}

                        {recommendMutation.isPending && (
                            <div className="flex items-center gap-3 py-4">
                                <LoadingSpinner />
                                <span className="text-sm text-gray-600">
                                    Geçmiş görevler ve personel uygunluğu analiz ediliyor...
                                </span>
                            </div>
                        )}

                        {recommendation && (
                            <div className="space-y-4">
                                {recommendation.aiExplanation && (
                                    <div className="bg-blue-50 border border-blue-200 rounded-md p-3 text-sm text-blue-800">
                                        {recommendation.aiExplanation}
                                    </div>
                                )}

                                {recommendation.recommendedPersonnel.length === 0 ? (
                                    <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3">
                                        <p className="text-sm text-amber-800">
                                            Bu görev tipi için daha önce başarıyla tamamlanmış görevi olan
                                            uygun gönüllü bulunamadı. Aday havuzu ve geçmiş görev
                                            eşleşmeleri kontrol edilmelidir.
                                        </p>
                                    </div>
                                ) : (
                                    <div className="overflow-x-auto">
                                        <table className="min-w-full divide-y divide-gray-200 text-sm">
                                            <thead className="bg-gray-50">
                                                <tr>
                                                    <th className="px-3 py-2 w-8" />
                                                    <th className="px-3 py-2 text-left font-medium text-gray-500">Ad Soyad</th>
                                                    <th className="px-3 py-2 text-left font-medium text-gray-500">E-posta</th>
                                                    <th className="px-3 py-2 text-left font-medium text-gray-500">Skor</th>
                                                    <th className="px-3 py-2 text-left font-medium text-gray-500">Önceki Görev</th>
                                                    <th className="px-3 py-2 text-left font-medium text-gray-500">Yakınlık</th>
                                                    <th className="px-3 py-2 text-left font-medium text-gray-500">Durum</th>
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-gray-100 bg-white">
                                                {recommendation.recommendedPersonnel.map((m) => (
                                                    // Güvenlik: VOLUNTEER olmayan kişiler backend'den gelmemeli,
                                                    // ama gelirse gösterme
                                                    <tr key={m.userId}
                                                        className={selectedUserIds.has(m.userId) ? 'bg-blue-50' : ''}>
                                                        <td className="px-3 py-2">
                                                            <input
                                                                type="checkbox"
                                                                checked={selectedUserIds.has(m.userId)}
                                                                onChange={() => toggleSelect(m.userId)}
                                                                className="h-4 w-4 text-blue-600 rounded border-gray-300"
                                                            />
                                                        </td>
                                                        <td className="px-3 py-2 font-medium text-gray-900">
                                                            {m.firstName} {m.lastName}
                                                        </td>
                                                        <td className="px-3 py-2 text-gray-600">{m.email}</td>
                                                        <td className="px-3 py-2">
                                                            <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                                                                m.score >= 60 ? 'bg-green-100 text-green-800' :
                                                                m.score >= 30 ? 'bg-yellow-100 text-yellow-800' :
                                                                'bg-red-100 text-red-800'
                                                            }`}>
                                                                {m.score}
                                                            </span>
                                                        </td>
                                                        <td className="px-3 py-2 text-gray-500 max-w-xs truncate"
                                                            title={m.previousSimilarTask || '-'}>
                                                            {m.previousSimilarTask || '-'}
                                                        </td>
                                                        <td className="px-3 py-2 text-gray-500">
                                                            {PROXIMITY_TR[m.proximityLevel || 'NONE'] || '-'}
                                                        </td>
                                                        <td className="px-3 py-2">
                                                            <span className={`text-xs font-medium ${
                                                                m.availability === 'AVAILABLE'    ? 'text-green-600' :
                                                                m.availability === 'ACTIVE_TASK'  ? 'text-orange-600' :
                                                                'text-red-600'
                                                            }`}>
                                                                {AVAILABILITY_TR[m.availability || 'AVAILABLE'] || '-'}
                                                            </span>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}

                                <div className="flex items-center justify-between pt-2 border-t border-gray-100">
                                    <span className="text-sm text-gray-600">
                                        Seçilen: <strong>{selectedUserIds.size}</strong> / {event.requiredPeople}
                                    </span>
                                    <div className="flex gap-2">
                                        <Button
                                            variant="danger"
                                            onClick={() => rejectMutation.mutate()}
                                            loading={rejectMutation.isPending}
                                        >
                                            Öneriyi Reddet
                                        </Button>
                                        <Button
                                            variant="primary"
                                            disabled={selectedUserIds.size === 0}
                                            loading={approveMutation.isPending}
                                            onClick={() => approveMutation.mutate()}
                                            leftIcon={<Mail className="h-4 w-4" />}
                                        >
                                            Seçilen Kişilere Görev Maili Gönder
                                        </Button>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* ── Dialoglar ─────────────────────────────────────────────────── */}
            <ConfirmationDialog
                isOpen={closeDialogOpen}
                title="Olayı İptal Et"
                message="Bu olayı iptal etmek istediğinize emin misiniz? Atanmış tüm gönüllüler serbest bırakılacak."
                confirmLabel="İptal Et"
                onConfirm={() => closeMutation.mutate()}
                onCancel={() => setCloseDialogOpen(false)}
                isLoading={closeMutation.isPending}
            />

            <ConfirmationDialog
                isOpen={completeDialogOpen}
                title="Olayı Tamamla"
                message="Bu olayı tamamlandı olarak işaretlemek istediğinize emin misiniz?"
                confirmLabel="Tamamlandı"
                onConfirm={() => completeMutation.mutate()}
                onCancel={() => setCompleteDialogOpen(false)}
                isLoading={completeMutation.isPending}
            />

            {editModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
                    <div className="bg-white rounded-xl shadow-xl p-6 w-full max-w-md space-y-4">
                        <h2 className="text-lg font-semibold text-gray-900">Olayı Düzenle</h2>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Başlık</label>
                            <input
                                type="text"
                                value={editForm.title}
                                onChange={(e) => setEditForm(f => ({ ...f, title: e.target.value }))}
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm
                                           focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Açıklama</label>
                            <textarea
                                value={editForm.description}
                                onChange={(e) => setEditForm(f => ({ ...f, description: e.target.value }))}
                                rows={3}
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm
                                           focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Gerekli Kişi</label>
                            <input
                                type="number"
                                min={1}
                                value={editForm.requiredPeople}
                                onChange={(e) => setEditForm(f => ({ ...f, requiredPeople: Number(e.target.value) }))}
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm
                                           focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>

                        <div className="flex justify-end gap-3 pt-2">
                            <Button variant="secondary" onClick={() => setEditModalOpen(false)}>İptal</Button>
                            <Button
                                variant="primary"
                                loading={updateMutation.isPending}
                                onClick={() => updateMutation.mutate()}
                            >
                                Kaydet
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
