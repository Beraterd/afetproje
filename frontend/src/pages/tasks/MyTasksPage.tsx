import React, { useEffect, useRef, useState } from 'react';
import { useToast } from '@/components/shared/ToastProvider';
import { getMyEvents, leaveEvent } from '@/api/events.api';
import {
    getMyDamageAssessmentTasks,
    uploadFieldPhotos,
    markFieldVerified,
    completeAssessmentTask,
} from '@/api/myTasks.api';
import { Badge, LoadingSpinner, ConfirmationDialog } from '@/components/ui';
import { UserEventResponse, DamageAssessmentTaskResponse } from '@/types';
import { ApiError } from '@/utils/errorParser';
import { useOfflineAction } from '@/hooks/useOfflineAction';
import { OFFLINE_SYNC_ITEM_SYNCED_EVENT } from '@/lib/syncService';
import { TaskSection, TaskEmptyRow } from '@/components/tasks/TaskSection';
import { EventTaskRow } from '@/components/tasks/EventTaskRow';
import { DamageTaskRow } from '@/components/tasks/DamageTaskRow';
import { PastDamageTaskRow } from '@/components/tasks/PastDamageTaskRow';
import { ACCEPTED_PHOTO_TYPES } from '@/components/tasks/taskLabels';

// ── Main page ─────────────────────────────────────────────────────────────────

export const MyTasksPage: React.FC = () => {
    const toast = useToast();
    const { execute: executeOffline } = useOfflineAction();

    // Event tasks
    const [isLoadingEvents, setIsLoadingEvents] = useState(true);
    const [myEvents, setMyEvents] = useState<UserEventResponse[]>([]);
    const [leavingEventId, setLeavingEventId] = useState<string | null>(null);
    const [confirmLeaveEventId, setConfirmLeaveEventId] = useState<string | null>(null);

    // Damage assessment tasks
    const [isLoadingDamage, setIsLoadingDamage] = useState(true);
    const [damageTasks, setDamageTasks] = useState<DamageAssessmentTaskResponse[]>([]);

    // Photo upload state (per assignment)
    const [photoPanel, setPhotoPanel] = useState<string | null>(null);
    const [selectedPhotos, setSelectedPhotos] = useState<File[]>([]);
    const [photoErrors, setPhotoErrors] = useState<string[]>([]);
    const [uploading, setUploading] = useState(false);
    const photoInputRef = useRef<HTMLInputElement>(null);

    // Field verify / complete confirm
    const [confirmFieldVerify, setConfirmFieldVerify] = useState<string | null>(null);
    const [confirmComplete, setConfirmComplete] = useState<string | null>(null);
    const [actionLoading, setActionLoading] = useState(false);

    const fetchEvents = async () => {
        try {
            const page = await getMyEvents({ size: 50 });
            setMyEvents(page.content);
        } catch {
            toast.error('Ekip görevleri yüklenemedi');
        } finally {
            setIsLoadingEvents(false);
        }
    };

    const fetchDamageTasks = async () => {
        try {
            const tasks = await getMyDamageAssessmentTasks();
            setDamageTasks(tasks);
        } catch {
            toast.error('Hasar tespiti görevleri yüklenemedi');
        } finally {
            setIsLoadingDamage(false);
        }
    };

    useEffect(() => {
        fetchEvents();
        fetchDamageTasks();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Offline kuyruktaki görev işlemleri arka planda senkronize olduğunda listeyi tazele.
    useEffect(() => {
        const handler = (e: Event) => {
            const type = (e as CustomEvent).detail?.type;
            if (type === 'TASK_FIELD_VERIFIED' || type === 'TASK_COMPLETE' || type === 'TASK_PHOTO_UPLOAD') {
                fetchDamageTasks();
            }
        };
        window.addEventListener(OFFLINE_SYNC_ITEM_SYNCED_EVENT, handler);
        return () => window.removeEventListener(OFFLINE_SYNC_ITEM_SYNCED_EVENT, handler);
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // ── Event task handlers ───────────────────────────────────────────────────

    const handleLeaveConfirm = async () => {
        if (!confirmLeaveEventId) return;
        const eventId = confirmLeaveEventId;
        setConfirmLeaveEventId(null);
        setLeavingEventId(eventId);
        try {
            await leaveEvent(eventId);
            toast.success('Görev bırakıldı.');
            const page = await getMyEvents({ size: 50 });
            setMyEvents(page.content);
        } catch (error) {
            const apiError = error as ApiError;
            toast.error(apiError.message || 'Görev bırakılırken bir hata oluştu');
        } finally {
            setLeavingEventId(null);
        }
    };

    // ── Damage task handlers ──────────────────────────────────────────────────

    const openPhotoPanel = (assignmentId: string) => {
        setPhotoPanel(assignmentId);
        setSelectedPhotos([]);
        setPhotoErrors([]);
    };

    const closePhotoPanel = () => {
        setPhotoPanel(null);
        setSelectedPhotos([]);
        setPhotoErrors([]);
        if (photoInputRef.current) photoInputRef.current.value = '';
    };

    const handlePhotoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = Array.from(e.target.files || []);
        const errors: string[] = [];
        const valid: File[] = [];
        files.forEach(f => {
            if (!ACCEPTED_PHOTO_TYPES.includes(f.type)) {
                errors.push(`"${f.name}" geçersiz format. JPEG, PNG veya WEBP kabul edilir.`);
            } else if (f.size > 20 * 1024 * 1024) {
                errors.push(`"${f.name}" çok büyük. Maks. 20MB.`);
            } else {
                valid.push(f);
            }
        });
        setPhotoErrors(errors);
        setSelectedPhotos(prev => [...prev, ...valid]);
        if (photoInputRef.current) photoInputRef.current.value = '';
    };

    const removeSelectedPhoto = (index: number) => {
        setSelectedPhotos(prev => prev.filter((_, i) => i !== index));
    };

    const handleUploadPhotos = async () => {
        if (!photoPanel || selectedPhotos.length === 0) return;
        setUploading(true);
        const assignmentId = photoPanel;
        const photosToUpload = selectedPhotos;
        try {
            const result = await executeOffline(
                {
                    type: 'TASK_PHOTO_UPLOAD',
                    endpoint: `/my-tasks/damage-assessments/${assignmentId}/photos`,
                    method: 'POST',
                    bodyKind: 'multipart',
                    photos: photosToUpload,
                },
                {},
                async () => { await uploadFieldPhotos(assignmentId, photosToUpload); },
            );
            if (result.queued) {
                toast.info('Çevrimdışısınız. Fotoğraflar cihazda kaydedildi, bağlantı gelince gönderilecek.');
            } else {
                toast.success('Fotoğraflar yüklendi');
                await fetchDamageTasks();
            }
            closePhotoPanel();
        } catch {
            toast.error('Fotoğraf yüklenemedi');
        } finally {
            setUploading(false);
        }
    };

    const handleFieldVerifyConfirm = async () => {
        if (!confirmFieldVerify) return;
        const assignmentId = confirmFieldVerify;
        setConfirmFieldVerify(null);
        setActionLoading(true);
        try {
            const result = await executeOffline(
                {
                    type: 'TASK_FIELD_VERIFIED',
                    endpoint: `/my-tasks/damage-assessments/${assignmentId}/field-verified`,
                    method: 'POST',
                    bodyKind: 'none',
                },
                {},
                async () => {
                    const updated = await markFieldVerified(assignmentId);
                    setDamageTasks(prev => prev.map(t => t.assignmentId === assignmentId ? updated : t));
                },
            );
            if (result.queued) {
                toast.info('Çevrimdışısınız. Doğrulama cihazda kaydedildi, bağlantı gelince gönderilecek.');
            } else {
                toast.success('Sahada doğrulama kaydedildi');
            }
        } catch (error) {
            const apiError = error as ApiError;
            toast.error(apiError.message || 'Doğrulama kaydedilemedi');
        } finally {
            setActionLoading(false);
        }
    };

    const handleCompleteConfirm = async () => {
        if (!confirmComplete) return;
        const assignmentId = confirmComplete;
        setConfirmComplete(null);
        setActionLoading(true);
        try {
            const result = await executeOffline(
                {
                    type: 'TASK_COMPLETE',
                    endpoint: `/my-tasks/damage-assessments/${assignmentId}/complete`,
                    method: 'POST',
                    bodyKind: 'none',
                },
                {},
                async () => {
                    const updated = await completeAssessmentTask(assignmentId);
                    setDamageTasks(prev => prev.map(t => t.assignmentId === assignmentId ? updated : t));
                },
            );
            if (result.queued) {
                toast.info('Çevrimdışısınız. Görev tamamlama cihazda kaydedildi, bağlantı gelince gönderilecek.');
            } else {
                toast.success('Görev tamamlandı');
            }
        } catch (error) {
            const apiError = error as ApiError;
            toast.error(apiError.message || 'Görev tamamlanamadı');
        } finally {
            setActionLoading(false);
        }
    };

    // ── Derived lists ─────────────────────────────────────────────────────────

    const activeEvents = myEvents.filter(e => e.volunteerStatus === 'ASSIGNED');
    const pastEvents = myEvents.filter(e => e.volunteerStatus !== 'ASSIGNED');
    const activeDamageTasks = damageTasks.filter(t => t.assignmentStatus !== 'COMPLETED');
    const pastDamageTasks = damageTasks.filter(t => t.assignmentStatus === 'COMPLETED');

    const isLoading = isLoadingEvents || isLoadingDamage;

    if (isLoading) {
        return (
            <div className="flex justify-center py-12">
                <LoadingSpinner />
            </div>
        );
    }

    return (
        <div className="max-w-3xl mx-auto py-6 space-y-8">
            <div>
                <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:tracking-tight">
                    Görevlerim
                </h2>
                <p className="mt-1 text-sm text-gray-500">Katıldığınız olaylar ve atandığınız hasar tespiti görevleri.</p>
            </div>

            {/* ── Aktif Ekip Görevleri ── */}
            <TaskSection
                title="Aktif Ekip Görevleri"
                subtitle="Şu anda atandığınız olaylar."
                badge={<Badge variant="success">{activeEvents.length} aktif</Badge>}
            >
                {activeEvents.length === 0 ? (
                    <TaskEmptyRow text="Şu anda aktif bir ekip göreviniz bulunmuyor." />
                ) : (
                    activeEvents.map(ev => (
                        <EventTaskRow
                            key={ev.id}
                            ev={ev}
                            leavingEventId={leavingEventId}
                            onLeave={id => setConfirmLeaveEventId(id)}
                            showLeaveButton
                        />
                    ))
                )}
            </TaskSection>

            {/* ── Aktif Hasar Tespiti Görevleri ── */}
            <TaskSection
                title="Aktif Hasar Tespiti Görevleri"
                subtitle="Yönlendirildiğiniz saha görevleri."
                badge={<Badge variant="success">{activeDamageTasks.length} aktif</Badge>}
            >
                {activeDamageTasks.length === 0 ? (
                    <TaskEmptyRow text="Şu anda aktif bir hasar tespiti göreviniz bulunmuyor." />
                ) : (
                    activeDamageTasks.map(task => (
                        <DamageTaskRow
                            key={task.assignmentId}
                            task={task}
                            photoPanel={photoPanel}
                            selectedPhotos={selectedPhotos}
                            photoErrors={photoErrors}
                            uploading={uploading}
                            actionLoading={actionLoading}
                            photoInputRef={photoInputRef}
                            onOpenPhotoPanel={openPhotoPanel}
                            onClosePhotoPanel={closePhotoPanel}
                            onRemovePhoto={removeSelectedPhoto}
                            onUpload={handleUploadPhotos}
                            onFieldVerify={id => setConfirmFieldVerify(id)}
                            onComplete={id => setConfirmComplete(id)}
                        />
                    ))
                )}
            </TaskSection>

            {/* ── Geçmiş Görevler ── */}
            <TaskSection
                title="Geçmiş Görevler"
                subtitle="Tamamladığınız veya bıraktığınız görevler."
            >
                {pastEvents.length === 0 && pastDamageTasks.length === 0 ? (
                    <TaskEmptyRow text="Henüz tamamlanmış göreviniz bulunmuyor." />
                ) : (
                    <>
                        {pastEvents.map(ev => (
                            <EventTaskRow key={ev.id} ev={ev} leavingEventId={leavingEventId} onLeave={() => {}} />
                        ))}
                        {pastDamageTasks.map(task => (
                            <PastDamageTaskRow key={task.assignmentId} task={task} />
                        ))}
                    </>
                )}
            </TaskSection>

            {/* ── Confirmation Dialogs ── */}
            <ConfirmationDialog
                isOpen={confirmLeaveEventId !== null}
                title="Görevi Bırak"
                message="Bu görevi bırakmak istediğinize emin misiniz?"
                confirmLabel="Görevi Bırak"
                onConfirm={handleLeaveConfirm}
                onCancel={() => setConfirmLeaveEventId(null)}
                isLoading={leavingEventId !== null}
            />
            <ConfirmationDialog
                isOpen={confirmFieldVerify !== null}
                title="Sahada Doğrulama"
                message="Binayı yerinde incelediğinizi onaylıyor musunuz? Bu işlem hasar tespitini 'Sahada Doğrulandı' olarak işaretleyecek."
                confirmLabel="Sahada Doğruladım"
                onConfirm={handleFieldVerifyConfirm}
                onCancel={() => setConfirmFieldVerify(null)}
                isLoading={actionLoading}
            />
            <ConfirmationDialog
                isOpen={confirmComplete !== null}
                title="Görevi Tamamla"
                message="Bu görevi tamamlamak istediğinize emin misiniz? Tamamlanan görevler geçmiş görevler listesine taşınır."
                confirmLabel="Görevi Tamamla"
                onConfirm={handleCompleteConfirm}
                onCancel={() => setConfirmComplete(null)}
                isLoading={actionLoading}
            />

            {/* hidden file input */}
            <input
                ref={photoInputRef}
                type="file"
                multiple
                accept="image/jpeg,image/jpg,image/png,image/webp"
                className="hidden"
                onChange={handlePhotoSelect}
            />
        </div>
    );
};
