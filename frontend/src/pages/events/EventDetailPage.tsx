import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { getEventById, joinEvent, leaveEvent, closeEvent, completeEvent, updateEvent } from '@/api/events.api';
import { queryKeys } from '@/utils/queryKeys';
import { Button, Badge, LoadingSpinner, ConfirmationDialog } from '@/components/ui';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/components/shared/ToastProvider';
import { MapPin, Users, Calendar, AlertTriangle, Edit2, CheckCircle } from 'lucide-react';
import { format } from 'date-fns';
import { EventStatus } from '@/types';

const TEAM_TR: Record<string, string> = {
    SEARCH_RESCUE: 'Arama Kurtarma Ekibi',
    FOOD_WATER: 'Yemek ve İçme Suyu Dağıtım Ekibi',
    EVACUATION: 'Tahliye Ekibi',
    COMMUNICATION: 'İletişim Ekibi',
    PSYCHOSOCIAL: 'Psikososyal Destek Ekibi',
    HASAR_TESPIT_EKIBI: 'Hasar Tespit Ekibi',
    OTHER: 'Diğer',
};

function statusBadge(status: EventStatus) {
    switch (status) {
        case 'OPEN':
        case 'IN_PROGRESS': return <Badge variant="warning">Devam Ediyor</Badge>;
        case 'COMPLETED': return <Badge variant="info">Tamamlandı</Badge>;
        case 'CLOSED': return <Badge variant="neutral">İptal Edildi</Badge>;
        default: return <Badge variant="neutral">{status}</Badge>;
    }
}

export const EventDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const toast = useToast();
    const user = useAuthStore((s) => s.user);

    const [closeDialogOpen, setCloseDialogOpen] = useState(false);
    const [completeDialogOpen, setCompleteDialogOpen] = useState(false);
    const [editModalOpen, setEditModalOpen] = useState(false);
    const [editForm, setEditForm] = useState({ title: '', description: '', requiredPeople: 1 });

    const { data: event, isLoading, isError } = useQuery({
        queryKey: queryKeys.events.detail(id!),
        queryFn: () => getEventById(id!),
        enabled: !!id,
    });

    // Seed edit form when modal opens
    const openEdit = () => {
        if (!event) return;
        setEditForm({
            title: event.title,
            description: event.description || '',
            requiredPeople: event.requiredPeople,
        });
        setEditModalOpen(true);
    };

    const joinMutation = useMutation({
        mutationFn: () => joinEvent(id!),
        onSuccess: () => {
            toast.success('Olaya başarıyla katıldınız');
            queryClient.invalidateQueries({ queryKey: queryKeys.events.detail(id!) });
        },
        onError: (err: any) => toast.error(err.message || 'Olaya katılınamadı'),
    });

    const leaveMutation = useMutation({
        mutationFn: () => leaveEvent(id!),
        onSuccess: () => {
            toast.success('Olaydan ayrıldınız');
            queryClient.invalidateQueries({ queryKey: queryKeys.events.detail(id!) });
        },
        onError: (err: any) => toast.error(err.message || 'Olaydan ayrılınamadı'),
    });

    const invalidateAll = () => {
        queryClient.invalidateQueries({ queryKey: queryKeys.events.all });
        queryClient.invalidateQueries({ queryKey: ['map'], exact: false });
    };

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
            title: editForm.title || undefined,
            description: editForm.description || undefined,
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

    if (isLoading) return <LoadingSpinner label="Olay yükleniyor..." />;
    if (isError || !event) return <div className="text-red-600">Olay detayları yüklenemedi.</div>;

    const canManage = user && ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'].includes(user.role);
    const isVolunteer = user?.role === 'VOLUNTEER';
    // OPEN: legacy kayıtlar için fallback (V56 migration sonrası IN_PROGRESS olacak)
    const isActiveEvent = event.status === 'IN_PROGRESS' || event.status === 'OPEN';

    return (
        <div className="max-w-4xl mx-auto space-y-6">
            <div className="bg-white shadow sm:rounded-lg overflow-hidden border border-gray-200">
                <div className="px-4 py-5 sm:px-6 flex justify-between items-start">
                    <div>
                        <div className="flex items-center space-x-3">
                            <h3 className="text-2xl leading-6 font-bold text-gray-900">{event.title}</h3>
                            {statusBadge(event.status)}
                        </div>
                        {event.description && (
                            <p className="mt-2 max-w-2xl text-sm text-gray-500 whitespace-pre-wrap">
                                {event.description}
                            </p>
                        )}
                    </div>

                    <div className="flex flex-wrap gap-2 justify-end">
                        {/* Edit button — available while event is active */}
                        {canManage && isActiveEvent && (
                            <Button variant="secondary" onClick={openEdit} leftIcon={<Edit2 className="h-4 w-4" />}>
                                Düzenle
                            </Button>
                        )}

                        {/* Complete button: IN_PROGRESS → COMPLETED */}
                        {canManage && isActiveEvent && (
                            <Button
                                variant="primary"
                                onClick={() => setCompleteDialogOpen(true)}
                                leftIcon={<CheckCircle className="h-4 w-4" />}
                            >
                                Tamamlandı
                            </Button>
                        )}

                        {/* Close button: IN_PROGRESS → CLOSED (iptal) */}
                        {canManage && isActiveEvent && (
                            <Button variant="danger" onClick={() => setCloseDialogOpen(true)}>
                                İptal Et
                            </Button>
                        )}

                        {/* Volunteer join/leave */}
                        {isActiveEvent && isVolunteer && !event.isParticipating && (
                            <Button
                                variant="primary"
                                onClick={() => joinMutation.mutate()}
                                loading={joinMutation.isPending}
                            >
                                Katıl
                            </Button>
                        )}
                        {isActiveEvent && isVolunteer && event.isParticipating && (
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
                                <Users className="mr-2 h-4 w-4" /> Gerekli Kişi Sayısı
                            </dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                {event.assignedVolunteers} / {event.requiredPeople} kişi
                            </dd>
                        </div>
                        <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500 flex items-center">
                                <AlertTriangle className="mr-2 h-4 w-4" /> Görevlendirilen Ekip
                            </dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                {event.team ? (TEAM_TR[event.team.name] || event.team.name) : '-'}
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

            {/* Close dialog */}
            <ConfirmationDialog
                isOpen={closeDialogOpen}
                title="Olayı İptal Et"
                message="Bu olayı iptal etmek istediğinize emin misiniz? Atanmış tüm gönüllüler serbest bırakılacak."
                confirmLabel="İptal Et"
                onConfirm={() => closeMutation.mutate()}
                onCancel={() => setCloseDialogOpen(false)}
                isLoading={closeMutation.isPending}
            />

            {/* Complete dialog */}
            <ConfirmationDialog
                isOpen={completeDialogOpen}
                title="Olayı Tamamla"
                message="Bu olayı tamamlandı olarak işaretlemek istediğinize emin misiniz?"
                confirmLabel="Tamamlandı"
                onConfirm={() => completeMutation.mutate()}
                onCancel={() => setCompleteDialogOpen(false)}
                isLoading={completeMutation.isPending}
            />

            {/* Edit modal */}
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
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Açıklama</label>
                            <textarea
                                value={editForm.description}
                                onChange={(e) => setEditForm(f => ({ ...f, description: e.target.value }))}
                                rows={3}
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Gerekli Kişi</label>
                            <input
                                type="number"
                                min={1}
                                value={editForm.requiredPeople}
                                onChange={(e) => setEditForm(f => ({ ...f, requiredPeople: Number(e.target.value) }))}
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
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
