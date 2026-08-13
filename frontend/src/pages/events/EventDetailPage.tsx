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
import { LoadingSpinner, ConfirmationDialog } from '@/components/ui';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/components/shared/ToastProvider';
import { getApiErrorMessage } from '@/utils/errorParser';
import type { RecommendedMemberResponse, TeamRecommendationResponse, EventParticipantResponse } from '@/types';
import { isAlreadyJoinedError } from '@/components/events/eventDetailLabels';
import { EventHeaderCard } from '@/components/events/EventHeaderCard';
import { EventParticipantsTable } from '@/components/events/EventParticipantsTable';
import { EventTimeline } from '@/components/events/EventTimeline';
import { EventAiRecommendationPanel } from '@/components/events/EventAiRecommendationPanel';
import { ErrorBoundary } from '@/components/shared/ErrorBoundary';
import { EventEditModal, EventEditFormState } from '@/components/events/EventEditModal';

export const EventDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const queryClient = useQueryClient();
    const toast = useToast();
    const user = useAuthStore((s) => s.user);

    const [closeDialogOpen,    setCloseDialogOpen]    = useState(false);
    const [completeDialogOpen, setCompleteDialogOpen] = useState(false);
    const [editModalOpen,      setEditModalOpen]      = useState(false);
    const [editForm, setEditForm] = useState<EventEditFormState>({ title: '', description: '', requiredPeople: 1 });

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
            const msg: string = getApiErrorMessage(err, '');
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
        onError: (err: any) => toast.error(getApiErrorMessage(err, 'Olaydan ayrılınamadı')),
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
            toast.error(getApiErrorMessage(err, 'Olay kapatılamadı'));
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
            toast.error(getApiErrorMessage(err, 'Tamamlama başarısız'));
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
        onError: (err: any) => toast.error(getApiErrorMessage(err, 'Güncelleme başarısız')),
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
        onError: (err: any) => toast.error(getApiErrorMessage(err, 'AI öneri oluşturulamadı')),
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
        onError: (err: any) => toast.error(getApiErrorMessage(err, 'Onay başarısız')),
    });

    const rejectMutation = useMutation({
        mutationFn: () => rejectTeamRecommendation(recommendation!.id),
        onSuccess: () => {
            toast.success('Öneri reddedildi');
            setRecommendation(null);
            setSelectedUserIds(new Set());
        },
        onError: (err: any) => toast.error(getApiErrorMessage(err, 'Red başarısız')),
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

    return (
        <div className="max-w-4xl mx-auto space-y-6">
            <EventHeaderCard
                event={event}
                canManage={!!canManage}
                isVolunteer={isVolunteer}
                isActiveEvent={isActiveEvent}
                isJoined={isJoined}
                acceptedCount={acceptedCount}
                onEdit={openEdit}
                onComplete={() => setCompleteDialogOpen(true)}
                onClose={() => setCloseDialogOpen(true)}
                onJoin={() => joinMutation.mutate()}
                onLeave={() => leaveMutation.mutate()}
                joinPending={joinMutation.isPending}
                leavePending={leaveMutation.isPending}
            />

            <EventParticipantsTable participants={participants} />

            <ErrorBoundary level="section" sectionName="Olay Zaman Çizelgesi">
                <EventTimeline eventId={id!} />
            </ErrorBoundary>

            {isActiveEvent && canManage && (
                <ErrorBoundary level="section" sectionName="AI Ekip Önerisi">
                    <EventAiRecommendationPanel
                        recommendation={recommendation}
                        autoFetching={autoRecQuery.isFetching && !autoRecQuery.data}
                        recommendPending={recommendMutation.isPending}
                        approvePending={approveMutation.isPending}
                        rejectPending={rejectMutation.isPending}
                        selectedUserIds={selectedUserIds}
                        requiredPeople={event.requiredPeople}
                        onToggleSelect={toggleSelect}
                        onRequestRecommendation={() => recommendMutation.mutate()}
                        onClearRecommendation={() => { setRecommendation(null); setSelectedUserIds(new Set()); }}
                        onApprove={() => approveMutation.mutate()}
                        onReject={() => rejectMutation.mutate()}
                    />
                </ErrorBoundary>
            )}

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
                <EventEditModal
                    form={editForm}
                    onChange={setEditForm}
                    saving={updateMutation.isPending}
                    onCancel={() => setEditModalOpen(false)}
                    onSave={() => updateMutation.mutate()}
                />
            )}
        </div>
    );
};
