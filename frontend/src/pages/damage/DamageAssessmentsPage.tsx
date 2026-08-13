import { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Building2, Plus } from 'lucide-react';
import { queryKeys } from '@/utils/queryKeys';
import {
    getDamageAssessments,
    getDamageAssessmentById,
    createDamageAssessment,
    verifyDamageAssessment,
    assignDamageAssessment,
    removeDamageAssignment,
    getEligibleAssignees,
    triggerAiAnalysis,
    enqueueMissingAiAnalysis,
} from '@/api/damageAssessments.api';
import { getDistricts } from '@/api/districts.api';
import { getNeighborhoods } from '@/api/neighborhoods.api';
import {
    DamageAssessmentResponse,
    DamageAssessmentAssignmentInfo,
    EligibleAssigneeResponse,
    CreateDamageAssessmentRequest,
    VERIFICATION_STATUSES,
} from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';
import { getApiErrorMessage } from '@/utils/errorParser';
import { useOfflineAction } from '@/hooks/useOfflineAction';
import { OFFLINE_SYNC_ITEM_SYNCED_EVENT } from '@/lib/syncService';
import { DamageAssessmentFilters } from '@/components/damage/DamageAssessmentFilters';
import { DamageAssessmentList } from '@/components/damage/DamageAssessmentList';
import { DamageAssessmentDetail } from '@/components/damage/DamageAssessmentDetail';
import { DamageAssessmentVerification } from '@/components/damage/DamageAssessmentVerification';
import { DamageAssessmentAssignment } from '@/components/damage/DamageAssessmentAssignment';
import {
    DamageAssessmentCreateModal,
    DamageAssessmentFormState,
} from '@/components/damage/DamageAssessmentCreateModal';
import { ACCEPTED_PHOTO_TYPES } from '@/components/damage/damageDisplay';

export function DamageAssessmentsPage() {
    const { success: toastSuccess, error: toastError, warning: toastWarning, info: toastInfo } = useToast();
    const { execute: executeOffline } = useOfflineAction();
    const queryClient = useQueryClient();

    /** Bir binaya hasar tespiti kaydedildikten/durumu değiştikten sonra Operasyon Haritası'ndaki
     *  bina rengi güncel kalsın diye ilgili query'leri invalidate eder (item 23) — bina detayı,
     *  ilçe/mahalle hasar özeti, ve o binayı içerebilecek TÜM bbox viewport'ları (prefix match). */
    const invalidateBuildingRelatedQueries = useCallback((buildingId?: string) => {
        if (buildingId) {
            queryClient.invalidateQueries({ queryKey: queryKeys.buildings.detail(buildingId) });
        }
        queryClient.invalidateQueries({ queryKey: queryKeys.map.damageSummary() });
        queryClient.invalidateQueries({ queryKey: ['buildings', 'bbox'] });
    }, [queryClient]);
    const user = useAuthStore(s => s.user);
    const isAdmin = user?.role === 'ADMIN';
    const isDistrictCoord = user?.role === 'DISTRICT_COORDINATOR';
    const canCreate = !!user;
    const canVerify = user?.role !== 'VOLUNTEER';
    const canApprove = isAdmin || isDistrictCoord;

    // Bildirim/"Dikkat Gerektirenler" deep-link'leri buradan gelir: ?selected={id} tek bir
    // kaydı otomatik açar, ?damageLevel=&verificationStatus= listeyi filtreler.
    const [searchParams, setSearchParams] = useSearchParams();
    const damageLevelFilter = searchParams.get('damageLevel') ?? undefined;
    const verificationStatusFilter = searchParams.get('verificationStatus') ?? undefined;

    const [assessments, setAssessments] = useState<DamageAssessmentResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const [aiRefreshing, setAiRefreshing] = useState(false);

    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showVerifyModal, setShowVerifyModal] = useState<DamageAssessmentResponse | null>(null);
    const [showDetailModal, setShowDetailModal] = useState<DamageAssessmentResponse | null>(null);

    // Assignment panel
    const [assignPanelAssessmentId, setAssignPanelAssessmentId] = useState<string | null>(null);
    const [assignedList, setAssignedList] = useState<DamageAssessmentAssignmentInfo[]>([]);
    const [eligibleAssignees, setEligibleAssignees] = useState<EligibleAssigneeResponse[]>([]);
    const [selectedUserId, setSelectedUserId] = useState('');
    const [assigning, setAssigning] = useState(false);
    const [removingId, setRemovingId] = useState<string | null>(null);

    // Create form state
    const [districts, setDistricts] = useState<any[]>([]);
    const [neighborhoods, setNeighborhoods] = useState<any[]>([]);
    const [selectedDistrictId, setSelectedDistrictId] = useState('');
    const [form, setForm] = useState<DamageAssessmentFormState>({});
    const [locationConfirmed, setLocationConfirmed] = useState(false);
    const [photos, setPhotos] = useState<File[]>([]);
    const [photoErrors, setPhotoErrors] = useState<string[]>([]);
    const [creating, setCreating] = useState(false);
    const photoInputRef = useRef<HTMLInputElement>(null);

    // Verify form
    const [verifyStatus, setVerifyStatus] = useState('SAHADA_DOGRULANDI');
    const [verifyNote, setVerifyNote] = useState('');
    const [verifying, setVerifying] = useState(false);

    // Manuel "AI Yorumu Yenile" — kuyruğa ekler, sonuç arka planda gelir.
    const handleAiRefresh = async (assessmentId: string) => {
        setAiRefreshing(true);
        try {
            await triggerAiAnalysis(assessmentId);
            setShowDetailModal(prev => prev ? { ...prev, aiAnalysisStatus: 'PENDING' } : null);
            toastSuccess('AI analizi kuyruğa eklendi. Sonuç arka planda işlenecek.');
        } catch (err: any) {
            toastError(getApiErrorMessage(err, 'AI analizi başlatılamadı'));
        } finally {
            setAiRefreshing(false);
        }
    };

    const loadAssessments = useCallback(async () => {
        setLoading(true);
        try {
            const res = await getDamageAssessments({
                page,
                size: 15,
                damageLevel: damageLevelFilter,
                verificationStatus: verificationStatusFilter,
            });
            setAssessments(res.content);
            setTotalPages(res.totalPages);
        } catch {
            toastError('Hasar tespitleri yüklenemedi');
        } finally {
            setLoading(false);
        }
    }, [page, damageLevelFilter, verificationStatusFilter, toastError]);

    useEffect(() => { loadAssessments(); }, [loadAssessments]);

    // On first load: ask backend to enqueue any records missing AI analysis (fire-and-forget).
    useEffect(() => {
        enqueueMissingAiAnalysis().catch(() => {});
    }, []);

    // Bildirimden/Dikkat Gerektirenler'den ?selected={id} ile gelindiyse ilgili kaydı doğrudan aç.
    useEffect(() => {
        const selectedId = searchParams.get('selected');
        if (!selectedId) return;
        getDamageAssessmentById(selectedId)
            .then((a) => setShowDetailModal(a))
            .catch(() => toastError('Bağlantı verilen hasar tespiti kaydı bulunamadı'));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const clearFilters = () => {
        const next = new URLSearchParams(searchParams);
        next.delete('damageLevel');
        next.delete('verificationStatus');
        setSearchParams(next, { replace: true });
    };

    // Offline kuyruktaki hasar tespiti işlemleri arka planda senkronize olduğunda listeyi tazele.
    useEffect(() => {
        const handler = (e: Event) => {
            const type = (e as CustomEvent).detail?.type;
            if (type === 'DAMAGE_ASSESSMENT_CREATE' || type === 'DAMAGE_ASSESSMENT_VERIFY') {
                loadAssessments();
            }
        };
        window.addEventListener(OFFLINE_SYNC_ITEM_SYNCED_EVENT, handler);
        return () => window.removeEventListener(OFFLINE_SYNC_ITEM_SYNCED_EVENT, handler);
    }, [loadAssessments]);

    useEffect(() => {
        if (!showCreateModal) return;
        if (isAdmin || !user?.districtId) {
            getDistricts().then(d => setDistricts(d)).catch(() => {});
        } else if (user?.districtId) {
            // Coordinators and volunteers: auto-set their assigned district
            setSelectedDistrictId(user.districtId);
        }
        // Neighborhood coordinators: pre-select their neighborhood
        if (user?.role === 'NEIGHBORHOOD_COORDINATOR' && user?.neighborhoodId) {
            setForm(f => ({ ...f, neighborhoodId: user.neighborhoodId }));
        }
    }, [showCreateModal, isAdmin, user?.districtId, user?.neighborhoodId, user?.role]);

    useEffect(() => {
        if (selectedDistrictId) {
            getNeighborhoods(selectedDistrictId).then(n => setNeighborhoods(n)).catch(() => {});
        } else {
            setNeighborhoods([]);
        }
    }, [selectedDistrictId]);

    const handleDistrictChange = (districtId: string) => {
        setSelectedDistrictId(districtId);
        setForm(f => ({ ...f, neighborhoodId: undefined, latitude: undefined, longitude: undefined, buildingId: undefined }));
        setLocationConfirmed(false);
    };

    const handleNeighborhoodChange = (neighborhoodId: string) => {
        setForm(f => ({ ...f, neighborhoodId, latitude: undefined, longitude: undefined, buildingId: undefined }));
        setLocationConfirmed(false);
    };

    const handlePositionChange = (lat: number, lng: number) => {
        setForm(f => ({ ...f, latitude: lat, longitude: lng }));
        setLocationConfirmed(false);
    };

    const handlePhotoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = Array.from(e.target.files || []);
        const errors: string[] = [];
        const valid: File[] = [];

        files.forEach(f => {
            if (!ACCEPTED_PHOTO_TYPES.includes(f.type)) {
                errors.push(`"${f.name}" geçersiz format. Yalnızca JPEG, PNG veya WEBP kabul edilir.`);
            } else if (f.size > 20 * 1024 * 1024) {
                errors.push(`"${f.name}" çok büyük. Maks. 20MB.`);
            } else {
                valid.push(f);
            }
        });

        setPhotoErrors(errors);
        setPhotos(prev => [...prev, ...valid]);
        // Input'u sıfırla (aynı dosya tekrar seçilebilsin)
        if (photoInputRef.current) photoInputRef.current.value = '';
    };

    const removePhoto = (index: number) => {
        setPhotos(prev => prev.filter((_, i) => i !== index));
    };

    const handleCreate = async () => {
        if (!form.neighborhoodId) { toastWarning('Mahalle seçimi zorunludur'); return; }
        if (!form.address) { toastWarning('Adres zorunludur'); return; }
        if (form.latitude == null || form.longitude == null) {
            toastWarning('Haritada bina konumu zorunludur');
            return;
        }
        if (!locationConfirmed) {
            toastWarning('Lütfen "Konumu Onayla" butonuna tıklayın');
            return;
        }
        if (photos.length === 0) {
            toastWarning('En az 1 fotoğraf yüklenmesi zorunludur');
            return;
        }

        setCreating(true);
        const clientGeneratedId = crypto.randomUUID();
        const baseData = {
            ...form as CreateDamageAssessmentRequest,
            locationSource: form.buildingId ? 'MAP_SELECTED' : 'USER_DRAGGED_PIN',
            locationVerified: true,
        };
        try {
            const result = await executeOffline(
                {
                    type: 'DAMAGE_ASSESSMENT_CREATE',
                    endpoint: '/damage-assessments',
                    method: 'POST',
                    bodyKind: 'multipart',
                    photos,
                    entityRef: { kind: 'DAMAGE_ASSESSMENT', localId: `local-${clientGeneratedId}` },
                },
                { ...baseData },
                async (cgId) => { await createDamageAssessment({ ...baseData, clientGeneratedId: cgId }, photos); },
                clientGeneratedId,
            );
            if (result.queued) {
                toastInfo('Çevrimdışısınız. Hasar tespiti cihazda kaydedildi, bağlantı gelince gönderilecek.');
            } else {
                toastSuccess('Hasar tespiti başarıyla oluşturuldu');
                invalidateBuildingRelatedQueries(baseData.buildingId);
            }
            handleCloseCreateModal();
            loadAssessments();
        } catch (err: any) {
            toastError(getApiErrorMessage(err, 'Oluşturma başarısız'));
        } finally {
            setCreating(false);
        }
    };

    const handleCloseCreateModal = () => {
        setShowCreateModal(false);
        setForm({});
        setSelectedDistrictId('');
        setLocationConfirmed(false);
        setPhotos([]);
        setPhotoErrors([]);
    };

    const handleVerify = async () => {
        if (!showVerifyModal) return;
        setVerifying(true);
        const assessmentId = showVerifyModal.id;
        const verifyBody = { verificationStatus: verifyStatus, note: verifyNote || undefined };
        try {
            const result = await executeOffline(
                {
                    type: 'DAMAGE_ASSESSMENT_VERIFY',
                    endpoint: `/damage-assessments/${assessmentId}/verify`,
                    method: 'PATCH',
                    bodyKind: 'json',
                },
                verifyBody,
                async () => { await verifyDamageAssessment(assessmentId, verifyBody); },
            );
            if (result.queued) {
                toastInfo('Çevrimdışısınız. Doğrulama cihazda kaydedildi, bağlantı gelince gönderilecek.');
            } else {
                toastSuccess('Doğrulama durumu güncellendi');
                invalidateBuildingRelatedQueries(showVerifyModal.buildingId);
            }
            setShowVerifyModal(null);
            setVerifyNote('');
            loadAssessments();
        } catch (err: any) {
            toastError(getApiErrorMessage(err, 'Güncelleme başarısız'));
        } finally {
            setVerifying(false);
        }
    };

    const openAssignPanel = async (a: DamageAssessmentResponse) => {
        setAssignPanelAssessmentId(a.id);
        setAssignedList(a.assignments || []);
        setSelectedUserId('');
        try {
            const assignees = await getEligibleAssignees(a.id);
            setEligibleAssignees(assignees);
        } catch {
            setEligibleAssignees([]);
        }
    };

    const handleAssign = async () => {
        if (!assignPanelAssessmentId || !selectedUserId) return;
        setAssigning(true);
        try {
            const newAssignment = await assignDamageAssessment(assignPanelAssessmentId, { userId: selectedUserId });
            setAssignedList(prev => [...prev, newAssignment]);
            setSelectedUserId('');
            toastSuccess('Görevli atandı');
            loadAssessments();
        } catch (err: any) {
            toastError(getApiErrorMessage(err, 'Atama başarısız'));
        } finally {
            setAssigning(false);
        }
    };

    const handleRemoveAssignment = async (assignmentId: string) => {
        if (!assignPanelAssessmentId) return;
        setRemovingId(assignmentId);
        try {
            await removeDamageAssignment(assignPanelAssessmentId, assignmentId);
            setAssignedList(prev => prev.filter(a => a.id !== assignmentId));
            toastSuccess('Görevli kaldırıldı');
            loadAssessments();
        } catch (err: any) {
            toastError(getApiErrorMessage(err, 'Kaldırma başarısız'));
        } finally {
            setRemovingId(null);
        }
    };

    // Koordinatör onayı için status seçeneklerini role'a göre filtrele
    const availableStatuses = VERIFICATION_STATUSES.filter(s => {
        if (s.value === 'KOORDINATOR_ONAYLADI') return canApprove;
        return true;
    });

    return (
        <div className="space-y-5">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <Building2 className="h-7 w-7 text-orange-600" />
                    <h1 className="text-2xl font-bold text-gray-900">Hasar Tespiti</h1>
                </div>
                {canCreate && (
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="flex items-center gap-2 bg-orange-600 hover:bg-orange-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                    >
                        <Plus className="h-4 w-4" />
                        Yeni Tespit
                    </button>
                )}
            </div>

            <DamageAssessmentFilters
                damageLevelFilter={damageLevelFilter}
                verificationStatusFilter={verificationStatusFilter}
                onClear={clearFilters}
            />

            <DamageAssessmentList
                assessments={assessments}
                loading={loading}
                page={page}
                totalPages={totalPages}
                canVerify={canVerify}
                onPageChange={setPage}
                onViewDetail={setShowDetailModal}
                onVerify={(a) => {
                    setShowVerifyModal(a);
                    setVerifyStatus(a.verificationStatus);
                    setVerifyNote(a.note || '');
                }}
                onAssign={openAssignPanel}
            />

            {showDetailModal && (
                <DamageAssessmentDetail
                    assessment={showDetailModal}
                    onClose={() => setShowDetailModal(null)}
                    canRefreshAi={canVerify}
                    aiRefreshing={aiRefreshing}
                    onAiRefresh={handleAiRefresh}
                />
            )}

            {assignPanelAssessmentId && (
                <DamageAssessmentAssignment
                    assignedList={assignedList}
                    eligibleAssignees={eligibleAssignees}
                    selectedUserId={selectedUserId}
                    assigning={assigning}
                    removingId={removingId}
                    onSelectUser={setSelectedUserId}
                    onAssign={handleAssign}
                    onRemove={handleRemoveAssignment}
                    onClose={() => setAssignPanelAssessmentId(null)}
                />
            )}

            {showCreateModal && (
                <DamageAssessmentCreateModal
                    showDistrictSelect={isAdmin || !user?.districtId}
                    districts={districts}
                    neighborhoods={neighborhoods}
                    selectedDistrictId={selectedDistrictId}
                    onDistrictChange={handleDistrictChange}
                    form={form}
                    setForm={setForm}
                    onNeighborhoodChange={handleNeighborhoodChange}
                    onPositionChange={handlePositionChange}
                    locationConfirmed={locationConfirmed}
                    onConfirmLocation={() => setLocationConfirmed(true)}
                    photos={photos}
                    photoErrors={photoErrors}
                    photoInputRef={photoInputRef}
                    onPhotoSelect={handlePhotoSelect}
                    onRemovePhoto={removePhoto}
                    creating={creating}
                    onCancel={handleCloseCreateModal}
                    onSubmit={handleCreate}
                />
            )}

            {showVerifyModal && (
                <DamageAssessmentVerification
                    assessment={showVerifyModal}
                    verifyStatus={verifyStatus}
                    verifyNote={verifyNote}
                    verifying={verifying}
                    canApprove={canApprove}
                    availableStatuses={availableStatuses}
                    onStatusChange={setVerifyStatus}
                    onNoteChange={setVerifyNote}
                    onCancel={() => setShowVerifyModal(null)}
                    onSubmit={handleVerify}
                />
            )}
        </div>
    );
}
