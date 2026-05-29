import React, { useEffect, useRef, useState } from 'react';
import { useToast } from '@/components/shared/ToastProvider';
import { getMyEvents, leaveEvent } from '@/api/events.api';
import {
    getMyDamageAssessmentTasks,
    uploadFieldPhotos,
    markFieldVerified,
    completeAssessmentTask,
} from '@/api/myTasks.api';
import { Button, Badge, LoadingSpinner, ConfirmationDialog } from '@/components/ui';
import { UserEventResponse, DamageAssessmentTaskResponse } from '@/types';
import { ApiError } from '@/utils/errorParser';
import { Navigation, Camera, X, CheckCircle, MapPin, Image, Brain } from 'lucide-react';
import { AuthenticatedImage } from '@/components/shared/AuthenticatedImage';

// ── Translation maps ──────────────────────────────────────────────────────────

const TEAM_TR: Record<string, string> = {
    SEARCH_RESCUE: 'Arama Kurtarma Ekibi',
    FOOD_WATER: 'Yemek ve İçme Suyu Dağıtım Ekibi',
    EVACUATION: 'Tahliye Ekibi',
    COMMUNICATION: 'İletişim Ekibi',
    PSYCHOSOCIAL: 'Psikososyal Destek Ekibi',
    HASAR_TESPIT_EKIBI: 'Hasar Tespit Ekibi',
    OTHER: 'Diğer',
};

const VOLUNTEER_STATUS_TR: Record<string, string> = {
    ASSIGNED: 'Aktif',
    COMPLETED: 'Tamamlandı',
    WITHDRAWN: 'Bırakıldı',
};

const VOLUNTEER_STATUS_VARIANT: Record<string, 'success' | 'neutral' | 'warning'> = {
    ASSIGNED: 'success',
    COMPLETED: 'neutral',
    WITHDRAWN: 'warning',
};

const ASSIGNMENT_STATUS_VARIANT: Record<string, 'success' | 'info' | 'neutral'> = {
    ACTIVE: 'success',
    FIELD_VERIFIED: 'info',
    COMPLETED: 'neutral',
};

const DAMAGE_LEVEL_COLOR: Record<string, string> = {
    UNASSESSED: 'bg-gray-100 text-gray-600',
    LIGHT: 'bg-yellow-100 text-yellow-800',
    MODERATE: 'bg-orange-100 text-orange-800',
    HEAVY: 'bg-red-100 text-red-800',
    COLLAPSED: 'bg-red-200 text-red-900 font-bold',
};

const ACCEPTED_PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

// ── Utility functions ─────────────────────────────────────────────────────────

function mapsUrl(lat?: number, lng?: number, address?: string): string {
    if (lat && lng) {
        return `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
    }
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address || '')}`;
}

function eventStatusBadge(status: string) {
    switch (status) {
        case 'OPEN':
        case 'IN_PROGRESS': return <Badge variant="warning">Devam Ediyor</Badge>;
        case 'COMPLETED': return <Badge variant="info">Tamamlandı</Badge>;
        case 'CLOSED': return <Badge variant="neutral">İptal Edildi</Badge>;
        default: return <Badge variant="neutral">{status}</Badge>;
    }
}

// ── Main page ─────────────────────────────────────────────────────────────────

export const MyTasksPage: React.FC = () => {
    const toast = useToast();

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
        try {
            await uploadFieldPhotos(photoPanel, selectedPhotos);
            toast.success('Fotoğraflar yüklendi');
            await fetchDamageTasks();
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
            const updated = await markFieldVerified(assignmentId);
            setDamageTasks(prev => prev.map(t => t.assignmentId === assignmentId ? updated : t));
            toast.success('Sahada doğrulama kaydedildi');
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
            const updated = await completeAssessmentTask(assignmentId);
            setDamageTasks(prev => prev.map(t => t.assignmentId === assignmentId ? updated : t));
            toast.success('Görev tamamlandı');
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
            <Section
                title="Aktif Ekip Görevleri"
                subtitle="Şu anda atandığınız olaylar."
                badge={<Badge variant="success">{activeEvents.length} aktif</Badge>}
            >
                {activeEvents.length === 0 ? (
                    <EmptyRow text="Şu anda aktif bir ekip göreviniz bulunmuyor." />
                ) : (
                    activeEvents.map(ev => (
                        <EventRow
                            key={ev.id}
                            ev={ev}
                            leavingEventId={leavingEventId}
                            onLeave={id => setConfirmLeaveEventId(id)}
                            showLeaveButton
                        />
                    ))
                )}
            </Section>

            {/* ── Aktif Hasar Tespiti Görevleri ── */}
            <Section
                title="Aktif Hasar Tespiti Görevleri"
                subtitle="Yönlendirildiğiniz saha görevleri."
                badge={<Badge variant="success">{activeDamageTasks.length} aktif</Badge>}
            >
                {activeDamageTasks.length === 0 ? (
                    <EmptyRow text="Şu anda aktif bir hasar tespiti göreviniz bulunmuyor." />
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
                            onPhotoSelect={handlePhotoSelect}
                            onRemovePhoto={removeSelectedPhoto}
                            onUpload={handleUploadPhotos}
                            onFieldVerify={id => setConfirmFieldVerify(id)}
                            onComplete={id => setConfirmComplete(id)}
                        />
                    ))
                )}
            </Section>

            {/* ── Geçmiş Görevler ── */}
            <Section
                title="Geçmiş Görevler"
                subtitle="Tamamladığınız veya bıraktığınız görevler."
            >
                {pastEvents.length === 0 && pastDamageTasks.length === 0 ? (
                    <EmptyRow text="Henüz tamamlanmış göreviniz bulunmuyor." />
                ) : (
                    <>
                        {pastEvents.map(ev => (
                            <EventRow key={ev.id} ev={ev} leavingEventId={leavingEventId} onLeave={() => {}} />
                        ))}
                        {pastDamageTasks.map(task => (
                            <PastDamageTaskRow key={task.assignmentId} task={task} />
                        ))}
                    </>
                )}
            </Section>

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

// ── Section wrapper ───────────────────────────────────────────────────────────

const Section: React.FC<{
    title: string;
    subtitle: string;
    badge?: React.ReactNode;
    children: React.ReactNode;
}> = ({ title, subtitle, badge, children }) => (
    <div className="bg-white shadow sm:rounded-lg overflow-hidden border border-gray-200">
        <div className="px-4 py-5 sm:px-6 border-b border-gray-200 flex items-center justify-between">
            <div>
                <h3 className="text-lg leading-6 font-medium text-gray-900">{title}</h3>
                <p className="mt-1 text-sm text-gray-500">{subtitle}</p>
            </div>
            {badge}
        </div>
        <div className="divide-y divide-gray-200">{children}</div>
    </div>
);

const EmptyRow: React.FC<{ text: string }> = ({ text }) => (
    <p className="px-4 py-6 text-sm text-gray-500 text-center">{text}</p>
);

// ── Event task row ────────────────────────────────────────────────────────────

const EventRow: React.FC<{
    ev: UserEventResponse;
    leavingEventId: string | null;
    onLeave: (id: string) => void;
    showLeaveButton?: boolean;
}> = ({ ev, leavingEventId, onLeave, showLeaveButton }) => {
    const canLeave = showLeaveButton
        && ev.volunteerStatus === 'ASSIGNED'
        && (ev.status === 'IN_PROGRESS' || ev.status === 'OPEN');

    return (
        <div className="px-4 py-4 sm:px-6 flex items-center justify-between gap-4">
            <div className="min-w-0">
                <p className="text-sm font-medium text-gray-900">{ev.title}</p>
                <p className="text-xs text-gray-500 mt-0.5">
                    {ev.neighborhood?.districtName} / {ev.neighborhood?.name}
                    {ev.team ? ` — ${TEAM_TR[ev.team.name] || ev.team.name}` : ''}
                </p>
            </div>
            <div className="flex items-center gap-2 flex-shrink-0">
                {eventStatusBadge(ev.status)}
                <Badge variant={VOLUNTEER_STATUS_VARIANT[ev.volunteerStatus] || 'neutral'}>
                    {VOLUNTEER_STATUS_TR[ev.volunteerStatus] || ev.volunteerStatus}
                </Badge>
                {canLeave && (
                    <Button
                        variant="danger"
                        size="sm"
                        loading={leavingEventId === ev.id}
                        disabled={leavingEventId !== null}
                        onClick={() => onLeave(ev.id)}
                    >
                        Görevi Bırak
                    </Button>
                )}
            </div>
        </div>
    );
};

// ── Active damage assessment task row ─────────────────────────────────────────

const DamageTaskRow: React.FC<{
    task: DamageAssessmentTaskResponse;
    photoPanel: string | null;
    selectedPhotos: File[];
    photoErrors: string[];
    uploading: boolean;
    actionLoading: boolean;
    photoInputRef: React.RefObject<HTMLInputElement>;
    onOpenPhotoPanel: (id: string) => void;
    onClosePhotoPanel: () => void;
    onPhotoSelect: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onRemovePhoto: (i: number) => void;
    onUpload: () => void;
    onFieldVerify: (id: string) => void;
    onComplete: (id: string) => void;
}> = ({
    task, photoPanel, selectedPhotos, photoErrors, uploading, actionLoading,
    photoInputRef, onOpenPhotoPanel, onClosePhotoPanel,
    onPhotoSelect, onRemovePhoto, onUpload, onFieldVerify, onComplete,
}) => {
    const isPanelOpen = photoPanel === task.assignmentId;
    const canVerify = task.assignmentStatus === 'ACTIVE';
    const canComplete = task.assignmentStatus === 'ACTIVE' || task.assignmentStatus === 'FIELD_VERIFIED';

    const urgencyFlags = [
        task.collapseRisk && 'Çökme Riski',
        task.emergencyEvacuationNeeded && 'Acil Tahliye',
        task.casualtiesSuspected && 'Kayıp/Yaralı',
        task.gasLeakRisk && 'Gaz Kaçağı',
    ].filter(Boolean) as string[];

    return (
        <div className="px-4 py-4 sm:px-6 space-y-3">
            {/* Header row */}
            <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-900 flex items-center gap-1.5">
                        <MapPin className="h-3.5 w-3.5 text-orange-500 flex-shrink-0" />
                        <span className="truncate">{task.address}</span>
                    </p>
                    <p className="text-xs text-gray-500 mt-0.5">
                        {task.districtName} / {task.neighborhoodName}
                    </p>
                    {urgencyFlags.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-1">
                            {urgencyFlags.map(f => (
                                <span key={f} className="text-xs bg-red-50 text-red-700 rounded px-1.5 py-0.5">{f}</span>
                            ))}
                        </div>
                    )}
                    <p className="text-xs text-gray-400 mt-1">
                        Atayan: {task.assignedByName} · {new Date(task.assignedAt).toLocaleDateString('tr-TR')}
                    </p>
                </div>
                <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${DAMAGE_LEVEL_COLOR[task.damageLevel] || 'bg-gray-100 text-gray-600'}`}>
                        {task.damageLevelLabel}
                    </span>
                    <Badge variant={ASSIGNMENT_STATUS_VARIANT[task.assignmentStatus] || 'neutral'}>
                        {task.assignmentStatusLabel}
                    </Badge>
                </div>
            </div>

            {/* AI Ön Değerlendirme */}
            {task.aiAnalysisStatus === 'PENDING' && (
                <div className="bg-purple-50 rounded-lg px-3 py-2 flex items-center gap-2">
                    <Brain className="h-4 w-4 text-purple-500 animate-pulse flex-shrink-0" />
                    <p className="text-xs text-purple-700">Yapay zeka analiz ediliyor...</p>
                </div>
            )}
            {task.aiAnalysisStatus === 'COMPLETED' && task.aiComment && (
                <div className="space-y-2">
                    <div className="bg-purple-50 rounded-lg p-3 space-y-1.5">
                        <div className="flex items-center gap-1.5">
                            <Brain className="h-3.5 w-3.5 text-purple-500 flex-shrink-0" />
                            <span className="text-xs font-medium text-purple-700">Yapay Zeka Ön Değerlendirmesi</span>
                            {task.aiConfidence && (
                                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ml-auto ${
                                    task.aiConfidence === 'HIGH' ? 'bg-red-100 text-red-800' :
                                    task.aiConfidence === 'MEDIUM' ? 'bg-orange-100 text-orange-800' :
                                    'bg-yellow-100 text-yellow-800'
                                }`}>
                                    {task.aiConfidenceLabel || task.aiConfidence}
                                </span>
                            )}
                        </div>
                        <p className="text-xs text-gray-700">{task.aiComment}</p>
                    </div>
                    <div className="bg-amber-50 border border-amber-200 rounded-lg px-3 py-1.5 flex items-start gap-1.5">
                        <span className="text-amber-500 text-xs flex-shrink-0">⚠</span>
                        <p className="text-xs text-amber-700">
                            Bu yorum yapay zekâ ön değerlendirmesidir. Resmi saha incelemesi yerine geçmez.
                        </p>
                    </div>
                </div>
            )}

            {/* Action buttons */}
            <div className="flex flex-wrap gap-2">
                <a
                    href={mapsUrl(task.latitude, task.longitude, task.address)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 border border-blue-300 rounded-lg text-xs font-medium text-blue-700 hover:bg-blue-50 transition-colors"
                >
                    <Navigation className="h-3.5 w-3.5" />
                    Konuma Git
                </a>
                <button
                    onClick={() => isPanelOpen ? onClosePhotoPanel() : onOpenPhotoPanel(task.assignmentId)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 border border-gray-300 rounded-lg text-xs font-medium text-gray-700 hover:bg-gray-50 transition-colors"
                >
                    <Camera className="h-3.5 w-3.5" />
                    Fotoğraf Yükle
                    {task.fieldPhotoUrls.length > 0 && (
                        <span className="bg-gray-200 text-gray-600 rounded-full px-1.5 py-0.5 text-xs">
                            {task.fieldPhotoUrls.length}
                        </span>
                    )}
                </button>
                {canVerify && (
                    <button
                        onClick={() => onFieldVerify(task.assignmentId)}
                        disabled={actionLoading}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-medium disabled:opacity-50 transition-colors"
                    >
                        <CheckCircle className="h-3.5 w-3.5" />
                        Sahada Doğruladım
                    </button>
                )}
                {canComplete && (
                    <button
                        onClick={() => onComplete(task.assignmentId)}
                        disabled={actionLoading}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white rounded-lg text-xs font-medium disabled:opacity-50 transition-colors"
                    >
                        Görevi Tamamla
                    </button>
                )}
            </div>

            {/* Photo upload panel */}
            {isPanelOpen && (
                <div className="border border-gray-200 rounded-lg p-3 bg-gray-50 space-y-3">
                    <div className="flex items-center justify-between">
                        <p className="text-xs font-medium text-gray-700">Saha Fotoğrafı Yükle</p>
                        <button onClick={onClosePhotoPanel} className="text-gray-400 hover:text-gray-600">
                            <X className="h-4 w-4" />
                        </button>
                    </div>

                    <button
                        type="button"
                        onClick={() => photoInputRef.current?.click()}
                        className="flex items-center gap-2 border-2 border-dashed border-gray-300 hover:border-orange-400 rounded-lg px-4 py-3 text-xs text-gray-500 hover:text-orange-600 transition-colors w-full justify-center"
                    >
                        <Image className="h-4 w-4" />
                        Fotoğraf Seç
                    </button>

                    {photoErrors.length > 0 && (
                        <div className="space-y-1">
                            {photoErrors.map((e, i) => (
                                <p key={i} className="text-xs text-red-600">{e}</p>
                            ))}
                        </div>
                    )}

                    {selectedPhotos.length > 0 && (
                        <div className="grid grid-cols-3 gap-2">
                            {selectedPhotos.map((photo, i) => (
                                <div key={i} className="relative group">
                                    <img
                                        src={URL.createObjectURL(photo)}
                                        alt={photo.name}
                                        className="w-full h-20 object-cover rounded-lg border border-gray-200"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => onRemovePhoto(i)}
                                        className="absolute -top-1.5 -right-1.5 bg-red-500 text-white rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity"
                                    >
                                        <X className="h-3 w-3" />
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}

                    {selectedPhotos.length > 0 && (
                        <button
                            onClick={onUpload}
                            disabled={uploading}
                            className="w-full px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-lg text-xs font-medium disabled:opacity-60 transition-colors"
                        >
                            {uploading ? 'Yükleniyor...' : `${selectedPhotos.length} Fotoğraf Yükle`}
                        </button>
                    )}

                    {task.reporterPhotoUrls.length > 0 && (
                        <div>
                            <p className="text-xs text-gray-500 mb-2">Bildirim fotoğrafları ({task.reporterPhotoUrls.length})</p>
                            <div className="grid grid-cols-4 gap-1.5">
                                {task.reporterPhotoUrls.map((url, i) => (
                                    <AuthenticatedImage
                                        key={i}
                                        photoUrl={url}
                                        alt={`Bildirim fotoğrafı ${i + 1}`}
                                        className="w-full h-16 object-cover rounded border border-gray-200"
                                    />
                                ))}
                            </div>
                        </div>
                    )}

                    {task.fieldPhotoUrls.length > 0 && (
                        <div>
                            <p className="text-xs text-gray-500 mb-2">Saha fotoğrafları ({task.fieldPhotoUrls.length})</p>
                            <div className="grid grid-cols-4 gap-1.5">
                                {task.fieldPhotoUrls.map((url, i) => (
                                    <AuthenticatedImage
                                        key={i}
                                        photoUrl={url}
                                        alt={`Saha fotoğrafı ${i + 1}`}
                                        className="w-full h-16 object-cover rounded border border-gray-200"
                                    />
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

// ── Past damage assessment task row ──────────────────────────────────────────

const PastDamageTaskRow: React.FC<{ task: DamageAssessmentTaskResponse }> = ({ task }) => (
    <div className="px-4 py-4 sm:px-6 flex items-center justify-between gap-4">
        <div className="min-w-0">
            <p className="text-sm font-medium text-gray-900 flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5 text-gray-400 flex-shrink-0" />
                <span className="truncate">{task.address}</span>
            </p>
            <p className="text-xs text-gray-500 mt-0.5">
                {task.districtName} / {task.neighborhoodName}
            </p>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
            <span className={`text-xs px-2 py-0.5 rounded-full ${DAMAGE_LEVEL_COLOR[task.damageLevel] || 'bg-gray-100 text-gray-600'}`}>
                {task.damageLevelLabel}
            </span>
            <Badge variant="neutral">{task.assignmentStatusLabel}</Badge>
        </div>
    </div>
);
