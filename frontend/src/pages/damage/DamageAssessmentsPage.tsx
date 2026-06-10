import { useState, useEffect, useCallback, useRef } from 'react';
import { Building2, Plus, MapPin, BadgeCheck, Navigation, X, Eye, Image, UserPlus, Trash2, Brain, RefreshCw } from 'lucide-react';
import { AuthenticatedImage } from '@/components/shared/AuthenticatedImage';
import {
    getDamageAssessments,
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
import { getUsers } from '@/api/users.api';
import {
    DamageAssessmentResponse,
    DamageAssessmentAssignmentInfo,
    EligibleAssigneeResponse,
    CreateDamageAssessmentRequest,
    DAMAGE_LEVELS,
    VERIFICATION_STATUSES,
} from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';
import { LocationPickerMap } from '@/components/map/LocationPickerMap';

const URGENCY_FIELDS = [
    { key: 'collapseRisk', label: 'Çökme Riski' },
    { key: 'emergencyEvacuationNeeded', label: 'Acil Tahliye Gerekli' },
    { key: 'casualtiesSuspected', label: 'Kayıp/Yaralı Şüphesi' },
    { key: 'blockedRoad', label: 'Yol Kapandı' },
    { key: 'gasLeakRisk', label: 'Gaz Kaçağı Riski' },
] as const;

const ACCEPTED_PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

type FormState = Partial<CreateDamageAssessmentRequest>;

function googleMapsUrl(lat: number, lng: number) {
    return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
}

function aiConfidenceBadgeColor(confidence: string) {
    switch (confidence) {
        case 'HIGH': return 'bg-red-100 text-red-800';
        case 'MEDIUM': return 'bg-orange-100 text-orange-800';
        case 'LOW': return 'bg-yellow-100 text-yellow-800';
        default: return 'bg-gray-100 text-gray-600';
    }
}

function verificationStatusColor(status: string) {
    switch (status) {
        case 'INCELEME_GEREKIYOR': return 'bg-yellow-100 text-yellow-800';
        case 'ASSIGNED': return 'bg-purple-100 text-purple-800';
        case 'SAHADA_DOGRULANDI': return 'bg-blue-100 text-blue-800';
        case 'KOORDINATOR_ONAYLADI': return 'bg-green-100 text-green-800';
        default: return 'bg-gray-100 text-gray-600';
    }
}

export function DamageAssessmentsPage() {
    const { success: toastSuccess, error: toastError, warning: toastWarning } = useToast();
    const user = useAuthStore(s => s.user);
    const isAdmin = user?.role === 'ADMIN';
    const isDistrictCoord = user?.role === 'DISTRICT_COORDINATOR';
    const canCreate = !!user;
    const canVerify = user?.role !== 'VOLUNTEER';
    const canApprove = isAdmin || isDistrictCoord;

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
    const [form, setForm] = useState<FormState>({});
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
            toastError(err?.response?.data?.message || 'AI analizi başlatılamadı');
        } finally {
            setAiRefreshing(false);
        }
    };

    const loadAssessments = useCallback(async () => {
        setLoading(true);
        try {
            const res = await getDamageAssessments({ page, size: 15 });
            setAssessments(res.content);
            setTotalPages(res.totalPages);
        } catch {
            toastError('Hasar tespitleri yüklenemedi');
        } finally {
            setLoading(false);
        }
    }, [page, toastError]);

    useEffect(() => { loadAssessments(); }, [loadAssessments]);

    // On first load: ask backend to enqueue any records missing AI analysis (fire-and-forget).
    useEffect(() => {
        enqueueMissingAiAnalysis().catch(() => {});
    }, []);

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
        setForm(f => ({ ...f, neighborhoodId: undefined, latitude: undefined, longitude: undefined }));
        setLocationConfirmed(false);
    };

    const handleNeighborhoodChange = (neighborhoodId: string) => {
        setForm(f => ({ ...f, neighborhoodId, latitude: undefined, longitude: undefined }));
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
        try {
            await createDamageAssessment(
                { ...form as CreateDamageAssessmentRequest, locationSource: 'USER_DRAGGED_PIN', locationVerified: true },
                photos
            );
            toastSuccess('Hasar tespiti başarıyla oluşturuldu');
            handleCloseCreateModal();
            loadAssessments();
        } catch (err: any) {
            toastError(err?.response?.data?.message || 'Oluşturma başarısız');
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
        try {
            await verifyDamageAssessment(showVerifyModal.id, { verificationStatus: verifyStatus, note: verifyNote || undefined });
            toastSuccess('Doğrulama durumu güncellendi');
            setShowVerifyModal(null);
            setVerifyNote('');
            loadAssessments();
        } catch (err: any) {
            toastError(err?.response?.data?.message || 'Güncelleme başarısız');
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
            toastError(err?.response?.data?.message || 'Atama başarısız');
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
            toastError(err?.response?.data?.message || 'Kaldırma başarısız');
        } finally {
            setRemovingId(null);
        }
    };

    const damageLevelColor = (level: string) => {
        switch (level) {
            case 'LIGHT': return 'bg-yellow-100 text-yellow-800';
            case 'MODERATE': return 'bg-orange-100 text-orange-800';
            case 'HEAVY': return 'bg-red-100 text-red-800';
            case 'COLLAPSED': return 'bg-red-200 text-red-900 font-bold';
            default: return 'bg-gray-100 text-gray-600';
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

            {loading ? (
                <p className="text-center text-gray-500 py-10">Yükleniyor...</p>
            ) : assessments.length === 0 ? (
                <div className="text-center py-16">
                    <Building2 className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                    <p className="text-gray-500">Kayıt bulunamadı</p>
                </div>
            ) : (
                <>
                    <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                        <table className="w-full text-sm">
                            <thead className="bg-gray-50 border-b border-gray-200">
                                <tr>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Adres</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Mahalle / İlçe</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Hasar Düzeyi</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Durum</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">AI Durumu</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Raporlayan</th>
                                    <th className="px-4 py-3"></th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {assessments.map(a => (
                                    <tr key={a.id} className="hover:bg-gray-50">
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-1.5">
                                                {a.latitude && a.longitude ? (
                                                    <a
                                                        href={googleMapsUrl(a.latitude, a.longitude)}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        title="Google Maps'te yol tarifi al"
                                                        className="flex-shrink-0 text-blue-500 hover:text-blue-700 transition-colors"
                                                        onClick={e => e.stopPropagation()}
                                                    >
                                                        <Navigation className="h-3.5 w-3.5" />
                                                    </a>
                                                ) : (
                                                    <MapPin className="h-3.5 w-3.5 flex-shrink-0 text-gray-300" />
                                                )}
                                                <span className="text-gray-800 line-clamp-1 max-w-[200px]">{a.address}</span>
                                            </div>
                                        </td>
                                        <td className="px-4 py-3 text-gray-600 text-xs">
                                            {a.neighborhoodName}, {a.districtName}
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className={`px-2 py-0.5 rounded-full text-xs ${damageLevelColor(a.damageLevel)}`}>
                                                {a.damageLevelLabel}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className={`px-2 py-0.5 rounded-full text-xs ${verificationStatusColor(a.verificationStatus)}`}>
                                                {a.verificationStatusLabel}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            {a.aiAnalysisStatus === 'COMPLETED' && (
                                                <span className="px-2 py-0.5 rounded-full text-xs bg-purple-100 text-purple-700">Hazır</span>
                                            )}
                                            {(a.aiAnalysisStatus === 'PROCESSING') && (
                                                <span className="px-2 py-0.5 rounded-full text-xs bg-blue-100 text-blue-700 animate-pulse">Analiz Ediliyor</span>
                                            )}
                                            {(a.aiAnalysisStatus === 'PENDING') && (
                                                <span className="px-2 py-0.5 rounded-full text-xs bg-yellow-100 text-yellow-700">Kuyrukta</span>
                                            )}
                                            {a.aiAnalysisStatus === 'FAILED' && (
                                                <span className="px-2 py-0.5 rounded-full text-xs bg-red-100 text-red-600">Hata</span>
                                            )}
                                            {(!a.aiAnalysisStatus || a.aiAnalysisStatus === 'NOT_STARTED') && (
                                                <span className="px-2 py-0.5 rounded-full text-xs bg-gray-100 text-gray-400">—</span>
                                            )}
                                        </td>
                                        <td className="px-4 py-3 text-gray-500 text-xs">{a.reportedBy || '-'}</td>
                                        <td className="px-4 py-3 flex items-center gap-1">
                                            <button
                                                onClick={() => setShowDetailModal(a)}
                                                className="p-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                                                title="Detayları Gör"
                                            >
                                                <Eye className="h-4 w-4" />
                                            </button>
                                            {canVerify && (
                                                <button
                                                    onClick={() => {
                                                        setShowVerifyModal(a);
                                                        setVerifyStatus(a.verificationStatus);
                                                        setVerifyNote(a.note || '');
                                                    }}
                                                    className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                    title="Durumu Güncelle"
                                                >
                                                    <BadgeCheck className="h-4 w-4" />
                                                </button>
                                            )}
                                            {canVerify && (
                                                <button
                                                    onClick={() => openAssignPanel(a)}
                                                    className="p-1.5 text-gray-400 hover:text-purple-600 hover:bg-purple-50 rounded-lg transition-colors"
                                                    title="Görevli Yönlendir"
                                                >
                                                    <UserPlus className="h-4 w-4" />
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    {totalPages > 1 && (
                        <div className="flex justify-center gap-2">
                            <button disabled={page === 0} onClick={() => setPage(p => p - 1)} className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40">Önceki</button>
                            <span className="px-3 py-1.5 text-sm text-gray-500">{page + 1} / {totalPages}</span>
                            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40">Sonraki</button>
                        </div>
                    )}
                </>
            )}

            {/* ── Detail Modal ── */}
            {showDetailModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 overflow-y-auto">
                    <div className="bg-white rounded-xl w-full max-w-2xl my-6 p-6 space-y-5">
                        <div className="flex items-start justify-between">
                            <h2 className="text-lg font-semibold text-gray-900">Hasar Tespiti Detayı</h2>
                            <button onClick={() => setShowDetailModal(null)} className="text-gray-400 hover:text-gray-600">
                                <X className="h-5 w-5" />
                            </button>
                        </div>

                        {/* Adres + Konum */}
                        <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                            <p className="font-medium text-gray-800">{showDetailModal.address}</p>
                            <p className="text-sm text-gray-500">{showDetailModal.neighborhoodName}, {showDetailModal.districtName}</p>
                            {showDetailModal.latitude && showDetailModal.longitude ? (
                                <a
                                    href={googleMapsUrl(showDetailModal.latitude, showDetailModal.longitude)}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="inline-flex items-center gap-1.5 text-sm text-blue-600 hover:text-blue-800 font-medium"
                                >
                                    <Navigation className="h-4 w-4" />
                                    Google Maps'te Yol Tarifi Al
                                </a>
                            ) : (
                                <span className="text-xs text-gray-400">Koordinat bilgisi yok</span>
                            )}
                        </div>

                        {/* Hasar Bilgileri */}
                        <div className="grid grid-cols-2 gap-4 text-sm">
                            <div>
                                <p className="text-gray-500 mb-1">Hasar Düzeyi</p>
                                <span className={`px-2 py-0.5 rounded-full text-xs ${damageLevelColor(showDetailModal.damageLevel)}`}>
                                    {showDetailModal.damageLevelLabel}
                                </span>
                            </div>
                            <div>
                                <p className="text-gray-500 mb-1">Doğrulama Durumu</p>
                                <span className={`px-2 py-0.5 rounded-full text-xs ${verificationStatusColor(showDetailModal.verificationStatus)}`}>
                                    {showDetailModal.verificationStatusLabel}
                                </span>
                            </div>
                        </div>

                        {/* Kayıt Geçmişi */}
                        <div className="border-t border-gray-100 pt-4 space-y-3 text-sm">
                            <h3 className="font-medium text-gray-700">Kayıt Geçmişi</h3>
                            <div className="space-y-2">
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-36 flex-shrink-0">Oluşturan:</span>
                                    <span className="text-gray-800">{showDetailModal.reportedBy || '-'}</span>
                                </div>
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-36 flex-shrink-0">Oluşturma Tarihi:</span>
                                    <span className="text-gray-800">{new Date(showDetailModal.createdAt).toLocaleString('tr-TR')}</span>
                                </div>
                                {showDetailModal.verifiedBy && (
                                    <>
                                        <div className="flex gap-2">
                                            <span className="text-gray-500 w-36 flex-shrink-0">Sahada Doğrulayan:</span>
                                            <span className="text-gray-800">{showDetailModal.verifiedBy}</span>
                                        </div>
                                        <div className="flex gap-2">
                                            <span className="text-gray-500 w-36 flex-shrink-0">Doğrulama Tarihi:</span>
                                            <span className="text-gray-800">{showDetailModal.verifiedAt ? new Date(showDetailModal.verifiedAt).toLocaleString('tr-TR') : '-'}</span>
                                        </div>
                                    </>
                                )}
                                {showDetailModal.approvedBy && (
                                    <>
                                        <div className="flex gap-2">
                                            <span className="text-gray-500 w-36 flex-shrink-0">Koordinatör Onayı:</span>
                                            <span className="text-gray-800">{showDetailModal.approvedBy}</span>
                                        </div>
                                        <div className="flex gap-2">
                                            <span className="text-gray-500 w-36 flex-shrink-0">Onay Tarihi:</span>
                                            <span className="text-gray-800">{showDetailModal.approvedAt ? new Date(showDetailModal.approvedAt).toLocaleString('tr-TR') : '-'}</span>
                                        </div>
                                    </>
                                )}
                            </div>
                        </div>

                        {/* Atanan Görevliler */}
                        {showDetailModal.assignments && showDetailModal.assignments.length > 0 && (
                            <div className="border-t border-gray-100 pt-4">
                                <p className="text-sm font-medium text-gray-700 mb-2">Atanan Görevliler</p>
                                <div className="space-y-1">
                                    {showDetailModal.assignments.map(a => (
                                        <div key={a.id} className="flex items-center gap-2 text-sm bg-purple-50 rounded-lg px-3 py-1.5">
                                            <UserPlus className="h-3.5 w-3.5 text-purple-500 flex-shrink-0" />
                                            <span className="font-medium text-gray-800">{a.firstName} {a.lastName}</span>
                                            <span className="text-gray-400">·</span>
                                            <span className="text-gray-500 text-xs">{a.email}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Not */}
                        {showDetailModal.note && (
                            <div className="border-t border-gray-100 pt-4">
                                <p className="text-sm text-gray-500 mb-1">Not</p>
                                <p className="text-sm text-gray-800">{showDetailModal.note}</p>
                            </div>
                        )}

                        {/* Bildirim Fotoğrafları */}
                        {showDetailModal.reporterPhotoUrls && showDetailModal.reporterPhotoUrls.length > 0 && (
                            <div className="border-t border-gray-100 pt-4">
                                <p className="text-sm font-medium text-gray-700 mb-3">
                                    Bildirim Fotoğrafları ({showDetailModal.reporterPhotoUrls.length})
                                </p>
                                <div className="grid grid-cols-3 gap-2">
                                    {showDetailModal.reporterPhotoUrls.map((url, i) => (
                                        <AuthenticatedImage
                                            key={i}
                                            photoUrl={url}
                                            alt={`Bildirim fotoğrafı ${i + 1}`}
                                            className="w-full h-24 object-cover rounded-lg border border-gray-200"
                                        />
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Saha Fotoğrafları */}
                        {showDetailModal.fieldPhotoUrls && showDetailModal.fieldPhotoUrls.length > 0 && (
                            <div className="border-t border-gray-100 pt-4">
                                <p className="text-sm font-medium text-gray-700 mb-3">
                                    Saha Fotoğrafları ({showDetailModal.fieldPhotoUrls.length})
                                </p>
                                <div className="grid grid-cols-3 gap-2">
                                    {showDetailModal.fieldPhotoUrls.map((url, i) => (
                                        <AuthenticatedImage
                                            key={i}
                                            photoUrl={url}
                                            alt={`Saha fotoğrafı ${i + 1}`}
                                            className="w-full h-24 object-cover rounded-lg border border-gray-200"
                                        />
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Fallback: show all photos if typed lists unavailable */}
                        {!showDetailModal.reporterPhotoUrls && !showDetailModal.fieldPhotoUrls
                            && showDetailModal.photoUrls && showDetailModal.photoUrls.length > 0 && (
                            <div className="border-t border-gray-100 pt-4">
                                <p className="text-sm font-medium text-gray-700 mb-3">Fotoğraflar ({showDetailModal.photoUrls.length})</p>
                                <div className="grid grid-cols-3 gap-2">
                                    {showDetailModal.photoUrls.map((url, i) => (
                                        <AuthenticatedImage
                                            key={i}
                                            photoUrl={url}
                                            alt={`Fotoğraf ${i + 1}`}
                                            className="w-full h-24 object-cover rounded-lg border border-gray-200"
                                        />
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* AI Ön Değerlendirme */}
                        <div className="border-t border-gray-100 pt-4">
                            <div className="flex items-center justify-between mb-3">
                                <div className="flex items-center gap-2">
                                    <Brain className="h-4 w-4 text-purple-600" />
                                    <h3 className="text-sm font-medium text-gray-700">Yapay Zeka Ön Değerlendirmesi</h3>
                                </div>
                                {canVerify && (
                                    <button
                                        onClick={() => handleAiRefresh(showDetailModal.id)}
                                        disabled={aiRefreshing || showDetailModal.aiAnalysisStatus === 'PROCESSING'}
                                        className="flex items-center gap-1.5 text-xs text-purple-600 hover:text-purple-800 disabled:opacity-50 transition-colors"
                                    >
                                        <RefreshCw className={`h-3.5 w-3.5 ${aiRefreshing ? 'animate-spin' : ''}`} />
                                        {aiRefreshing ? 'Ekleniyor...' : 'AI Yorumu Yenile'}
                                    </button>
                                )}
                            </div>

                            {showDetailModal.aiAnalysisStatus === 'PROCESSING' && (
                                <div className="bg-purple-50 rounded-lg px-4 py-3 text-sm text-purple-700 flex items-center gap-2">
                                    <RefreshCw className="h-3.5 w-3.5 animate-spin flex-shrink-0" />
                                    Analiz ediliyor...
                                </div>
                            )}

                            {showDetailModal.aiAnalysisStatus === 'PENDING' && (
                                <div className="bg-yellow-50 rounded-lg px-4 py-3 text-sm text-yellow-700">
                                    AI analizi kuyrukta, arka planda işlenecek.
                                </div>
                            )}

                            {showDetailModal.aiAnalysisStatus === 'COMPLETED' && showDetailModal.aiComment && (
                                <div className="space-y-3">
                                    <div className="bg-purple-50 rounded-lg p-4 space-y-2">
                                        <p className="text-sm text-gray-800">{showDetailModal.aiComment}</p>
                                        <div className="flex flex-wrap items-center gap-3 mt-2">
                                            {showDetailModal.aiConfidence && (
                                                <div className="flex items-center gap-1.5">
                                                    <span className="text-xs text-gray-500">Güven:</span>
                                                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${aiConfidenceBadgeColor(showDetailModal.aiConfidence)}`}>
                                                        {showDetailModal.aiConfidenceLabel || showDetailModal.aiConfidence}
                                                    </span>
                                                </div>
                                            )}
                                            {showDetailModal.aiRiskScore != null && (
                                                <div className="flex items-center gap-1.5">
                                                    <span className="text-xs text-gray-500">Risk Skoru:</span>
                                                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                                                        showDetailModal.aiRiskScore >= 70 ? 'bg-red-100 text-red-700'
                                                        : showDetailModal.aiRiskScore >= 40 ? 'bg-orange-100 text-orange-700'
                                                        : 'bg-green-100 text-green-700'
                                                    }`}>
                                                        {showDetailModal.aiRiskScore}/100
                                                    </span>
                                                </div>
                                            )}
                                            {showDetailModal.aiPriority != null && (
                                                <div className="flex items-center gap-1.5">
                                                    <span className="text-xs text-gray-500">Öncelik:</span>
                                                    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700">
                                                        {showDetailModal.aiPriority}/5
                                                    </span>
                                                </div>
                                            )}
                                        </div>
                                        {showDetailModal.aiRecommendations && (
                                            <div className="mt-2">
                                                <p className="text-xs text-gray-500 mb-1">Önerilen Aksiyonlar:</p>
                                                <ul className="space-y-0.5">
                                                    {showDetailModal.aiRecommendations.split(',').map((r, i) => (
                                                        <li key={i} className="text-xs text-gray-700 flex items-start gap-1">
                                                            <span className="text-purple-400 flex-shrink-0 mt-0.5">•</span>
                                                            {r.trim()}
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}
                                        {showDetailModal.aiAnalyzedAt && (
                                            <p className="text-xs text-gray-400 mt-1">
                                                {new Date(showDetailModal.aiAnalyzedAt).toLocaleString('tr-TR')}
                                                {showDetailModal.aiModel && ` · ${showDetailModal.aiModel}`}
                                            </p>
                                        )}
                                    </div>
                                    <div className="bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 flex items-start gap-2">
                                        <span className="text-amber-500 text-xs mt-0.5 flex-shrink-0">⚠</span>
                                        <p className="text-xs text-amber-700">
                                            Bu yorum yapay zekâ ön değerlendirmesidir. Resmi saha incelemesi yerine geçmez.
                                        </p>
                                    </div>
                                </div>
                            )}

                            {showDetailModal.aiAnalysisStatus === 'FAILED' && (
                                <div className="bg-red-50 rounded-lg px-4 py-3 text-sm text-red-600">
                                    AI yorumu oluşturulamadı.
                                    {canVerify && ' "AI Yorumu Yenile" ile yeniden deneyebilirsiniz.'}
                                </div>
                            )}

                            {(!showDetailModal.aiAnalysisStatus || showDetailModal.aiAnalysisStatus === 'NOT_STARTED') && (
                                <p className="text-sm text-gray-400 italic">
                                    AI analizi henüz yapılmamış; arka planda kuyruğa alındı.
                                </p>
                            )}
                        </div>

                        <div className="flex justify-end pt-2">
                            <button
                                onClick={() => setShowDetailModal(null)}
                                className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                            >
                                Kapat
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* ── Görevli Yönlendirme Paneli ── */}
            {assignPanelAssessmentId && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl w-full max-w-lg p-6 space-y-5">
                        <div className="flex items-start justify-between">
                            <div>
                                <h2 className="text-lg font-semibold text-gray-900">Görevli Yönlendir</h2>
                                <p className="text-xs text-gray-500 mt-0.5">Hasar Tespit Ekibi üyelerinden atama yapabilirsiniz</p>
                            </div>
                            <button onClick={() => setAssignPanelAssessmentId(null)} className="text-gray-400 hover:text-gray-600">
                                <X className="h-5 w-5" />
                            </button>
                        </div>

                        {/* Mevcut atamalar */}
                        <div>
                            <h3 className="text-sm font-medium text-gray-700 mb-2">Atanan Görevliler</h3>
                            {assignedList.length === 0 ? (
                                <p className="text-sm text-gray-400 italic">Henüz görevli atanmamış</p>
                            ) : (
                                <ul className="space-y-2">
                                    {assignedList.map(a => (
                                        <li key={a.id} className="flex items-center justify-between bg-purple-50 rounded-lg px-3 py-2">
                                            <div>
                                                <span className="text-sm font-medium text-gray-800">{a.firstName} {a.lastName}</span>
                                                <span className="text-xs text-gray-500 ml-2">{a.email}</span>
                                            </div>
                                            <button
                                                onClick={() => handleRemoveAssignment(a.id)}
                                                disabled={removingId === a.id}
                                                className="p-1 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded transition-colors disabled:opacity-50"
                                                title="Kaldır"
                                            >
                                                <Trash2 className="h-3.5 w-3.5" />
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>

                        {/* Yeni atama */}
                        <div className="border-t border-gray-100 pt-4">
                            <h3 className="text-sm font-medium text-gray-700 mb-2">Yeni Görevli Ekle</h3>
                            {eligibleAssignees.length === 0 ? (
                                <p className="text-sm text-yellow-600 bg-yellow-50 rounded-lg px-3 py-2">
                                    Bu bölgede aktif Hasar Tespit Ekibi görevi alan kişi bulunamadı.
                                </p>
                            ) : (
                                <div className="flex gap-2">
                                    <select
                                        value={selectedUserId}
                                        onChange={e => setSelectedUserId(e.target.value)}
                                        className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                                    >
                                        <option value="">Görevli seçin...</option>
                                        {eligibleAssignees
                                            .filter(m => !assignedList.some(a => a.userId === m.userId))
                                            .map(m => (
                                                <option key={m.userId} value={m.userId}>
                                                    {m.firstName} {m.lastName} — {m.eventTitle}
                                                </option>
                                            ))
                                        }
                                    </select>
                                    <button
                                        onClick={handleAssign}
                                        disabled={!selectedUserId || assigning}
                                        className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm font-medium disabled:opacity-50 transition-colors"
                                    >
                                        {assigning ? 'Atanıyor...' : 'Ata'}
                                    </button>
                                </div>
                            )}
                        </div>

                        <div className="flex justify-end pt-1">
                            <button
                                onClick={() => setAssignPanelAssessmentId(null)}
                                className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                            >
                                Kapat
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* ── Create Modal ── */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black/50 flex items-start justify-center z-50 p-4 overflow-y-auto">
                    <div className="bg-white rounded-xl w-full max-w-5xl my-6">
                        <div className="px-6 pt-6 pb-4 border-b border-gray-200">
                            <h2 className="text-lg font-semibold text-gray-900">Yeni Hasar Tespiti</h2>
                            <p className="text-sm text-gray-500 mt-0.5">
                                Binaya ait bilgileri girin, fotoğraf yükleyin ve haritada konumu doğrulayın.
                            </p>
                        </div>

                        <div className="p-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
                            {/* ── Sol: Form Alanları ── */}
                            <div className="space-y-4">
                                {(isAdmin || !user?.districtId) && (
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">İlçe *</label>
                                        <select
                                            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                            value={selectedDistrictId}
                                            onChange={e => handleDistrictChange(e.target.value)}
                                        >
                                            <option value="">Seçin</option>
                                            {districts.map((d: any) => (
                                                <option key={d.id} value={d.id}>{d.name}</option>
                                            ))}
                                        </select>
                                    </div>
                                )}

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Mahalle *</label>
                                    <select
                                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                        value={form.neighborhoodId || ''}
                                        onChange={e => handleNeighborhoodChange(e.target.value)}
                                    >
                                        <option value="">Seçin</option>
                                        {neighborhoods.map((n: any) => (
                                            <option key={n.id} value={n.id}>{n.name}</option>
                                        ))}
                                    </select>
                                </div>

                                <div className="grid grid-cols-3 gap-3">
                                    <div className="col-span-2">
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Sokak / Cadde</label>
                                        <input
                                            type="text"
                                            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                            value={form.streetName || ''}
                                            onChange={e => setForm(f => ({ ...f, streetName: e.target.value }))}
                                            placeholder="Atatürk Cad."
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Bina No</label>
                                        <input
                                            type="text"
                                            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                            value={form.buildingNo || ''}
                                            onChange={e => setForm(f => ({ ...f, buildingNo: e.target.value }))}
                                            placeholder="No: 12"
                                        />
                                    </div>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Açık Adres *</label>
                                    <input
                                        type="text"
                                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                        value={form.address || ''}
                                        onChange={e => setForm(f => ({ ...f, address: e.target.value }))}
                                        placeholder="Tam adres bilgisi"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Hasar Düzeyi</label>
                                    <select
                                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                        value={form.damageLevel || 'UNASSESSED'}
                                        onChange={e => setForm(f => ({ ...f, damageLevel: e.target.value }))}
                                    >
                                        {DAMAGE_LEVELS.map(d => (
                                            <option key={d.value} value={d.value}>{d.label}</option>
                                        ))}
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">Acil Durumlar</label>
                                    <div className="grid grid-cols-1 gap-1.5">
                                        {URGENCY_FIELDS.map(f => (
                                            <label key={f.key} className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={!!(form as any)[f.key]}
                                                    onChange={e => setForm(prev => ({ ...prev, [f.key]: e.target.checked }))}
                                                    className="rounded border-gray-300 text-orange-600 focus:ring-orange-500"
                                                />
                                                {f.label}
                                            </label>
                                        ))}
                                    </div>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Not</label>
                                    <textarea
                                        rows={2}
                                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-orange-500"
                                        value={form.note || ''}
                                        onChange={e => setForm(f => ({ ...f, note: e.target.value }))}
                                        placeholder="Hasar hakkında ek bilgi..."
                                    />
                                </div>

                                {/* Fotoğraf Yükleme */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Fotoğraflar <span className="text-red-500">*</span>
                                        <span className="text-gray-400 font-normal ml-1">(JPEG, PNG, WEBP — maks. 20MB)</span>
                                    </label>
                                    <input
                                        ref={photoInputRef}
                                        type="file"
                                        multiple
                                        accept="image/jpeg,image/jpg,image/png,image/webp"
                                        className="hidden"
                                        onChange={handlePhotoSelect}
                                    />
                                    <button
                                        type="button"
                                        onClick={() => photoInputRef.current?.click()}
                                        className="flex items-center gap-2 border-2 border-dashed border-gray-300 hover:border-orange-400 rounded-lg px-4 py-3 text-sm text-gray-500 hover:text-orange-600 transition-colors w-full justify-center"
                                    >
                                        <Image className="h-4 w-4" />
                                        Fotoğraf Ekle
                                    </button>

                                    {photoErrors.length > 0 && (
                                        <div className="mt-2 space-y-1">
                                            {photoErrors.map((e, i) => (
                                                <p key={i} className="text-xs text-red-600">{e}</p>
                                            ))}
                                        </div>
                                    )}

                                    {photos.length > 0 && (
                                        <div className="mt-2 grid grid-cols-3 gap-2">
                                            {photos.map((photo, i) => (
                                                <div key={i} className="relative group">
                                                    <img
                                                        src={URL.createObjectURL(photo)}
                                                        alt={photo.name}
                                                        className="w-full h-20 object-cover rounded-lg border border-gray-200"
                                                    />
                                                    <button
                                                        type="button"
                                                        onClick={() => removePhoto(i)}
                                                        className="absolute -top-1.5 -right-1.5 bg-red-500 text-white rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity"
                                                    >
                                                        <X className="h-3 w-3" />
                                                    </button>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* ── Sağ: Mini Harita ── */}
                            <div>
                                <p className="block text-sm font-medium text-gray-700 mb-2">Bina Konumu *</p>
                                {form.neighborhoodId && selectedDistrictId ? (
                                    <LocationPickerMap
                                        districtId={selectedDistrictId}
                                        neighborhoodId={form.neighborhoodId}
                                        neighborhoodName={neighborhoods.find(n => n.id === form.neighborhoodId)?.name}
                                        districtName={districts.find(d => d.id === selectedDistrictId)?.name}
                                        streetName={form.streetName}
                                        buildingNo={form.buildingNo}
                                        lat={form.latitude ?? null}
                                        lng={form.longitude ?? null}
                                        locationConfirmed={locationConfirmed}
                                        onChange={handlePositionChange}
                                        onConfirm={() => setLocationConfirmed(true)}
                                    />
                                ) : (
                                    <div className="flex flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-200 bg-gray-50 text-center p-8" style={{ minHeight: '320px' }}>
                                        <MapPin className="h-10 w-10 text-gray-300 mb-3" />
                                        <p className="text-sm text-gray-400 font-medium">Konum seçmek için</p>
                                        <p className="text-xs text-gray-400 mt-1">önce ilçe ve mahalle seçin</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="px-6 pb-6 pt-4 border-t border-gray-200 flex gap-3 justify-end">
                            <button
                                onClick={handleCloseCreateModal}
                                className="px-5 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
                            >
                                İptal
                            </button>
                            <button
                                onClick={handleCreate}
                                disabled={creating}
                                className="px-5 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-lg text-sm font-medium disabled:opacity-60 transition-colors"
                            >
                                {creating ? 'Kaydediliyor...' : 'Kaydet'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* ── Verify Modal ── */}
            {showVerifyModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl w-full max-w-md p-6 space-y-4">
                        <h2 className="text-lg font-semibold text-gray-900">Doğrulama Durumunu Güncelle</h2>
                        <p className="text-sm text-gray-600">{showVerifyModal.address} — {showVerifyModal.neighborhoodName}</p>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Durum</label>
                            <select
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                value={verifyStatus}
                                onChange={e => setVerifyStatus(e.target.value)}
                            >
                                {availableStatuses.map(s => (
                                    <option key={s.value} value={s.value}>{s.label}</option>
                                ))}
                            </select>
                            {verifyStatus === 'KOORDINATOR_ONAYLADI' && !canApprove && (
                                <p className="text-xs text-red-500 mt-1">Bu durum yalnızca ilçe koordinatörü veya admin tarafından atanabilir.</p>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Not (isteğe bağlı)</label>
                            <textarea
                                rows={2}
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none"
                                value={verifyNote}
                                onChange={e => setVerifyNote(e.target.value)}
                            />
                        </div>

                        <div className="flex gap-3">
                            <button
                                onClick={() => setShowVerifyModal(null)}
                                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                            >
                                İptal
                            </button>
                            <button
                                onClick={handleVerify}
                                disabled={verifying}
                                className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium disabled:opacity-60"
                            >
                                {verifying ? 'Güncelleniyor...' : 'Güncelle'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
