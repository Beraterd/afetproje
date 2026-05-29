import { useState, useEffect, useCallback } from 'react';
import { Archive, Plus, RefreshCw, Eye, X } from 'lucide-react';
import { getResourceRequests, createResourceRequest, updateResourceRequestStatus } from '@/api/resourceRequests.api';
import { getDistricts } from '@/api/districts.api';
import { getNeighborhoods } from '@/api/neighborhoods.api';
import {
    ResourceRequestResponse,
    CreateResourceRequestRequest,
    RESOURCE_TYPES,
    RESOURCE_STATUSES,
} from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';

export function ResourceRequestsPage() {
    const { success: toastSuccess, error: toastError, warning: toastWarning } = useToast();
    const user = useAuthStore(s => s.user);
    const canCreate = user?.role !== 'VOLUNTEER';

    const isAdmin = user?.role === 'ADMIN';
    const isDistrictCoord = user?.role === 'DISTRICT_COORDINATOR';
    const isNeighborhoodCoord = user?.role === 'NEIGHBORHOOD_COORDINATOR';
    const districtLocked = !isAdmin;
    const neighborhoodLocked = isNeighborhoodCoord;

    const [requests, setRequests] = useState<ResourceRequestResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [filterStatus, setFilterStatus] = useState('');

    const [showCreateModal, setShowCreateModal] = useState(false);
    const [districts, setDistricts] = useState<any[]>([]);
    const [neighborhoods, setNeighborhoods] = useState<any[]>([]);
    const [loadingNeighborhoods, setLoadingNeighborhoods] = useState(false);
    const [selectedDistrictId, setSelectedDistrictId] = useState('');
    const [form, setForm] = useState<Partial<CreateResourceRequestRequest>>({});
    const [creating, setCreating] = useState(false);

    const [showStatusModal, setShowStatusModal] = useState<ResourceRequestResponse | null>(null);
    const [newStatus, setNewStatus] = useState('');
    const [updatingStatus, setUpdatingStatus] = useState(false);

    const [showDetailModal, setShowDetailModal] = useState<ResourceRequestResponse | null>(null);

    const loadRequests = useCallback(async () => {
        setLoading(true);
        try {
            const res = await getResourceRequests({ page, size: 15, status: filterStatus || undefined });
            setRequests(res.content);
            setTotalPages(res.totalPages);
        } catch {
            toastError('Talepler yüklenemedi');
        } finally {
            setLoading(false);
        }
    }, [page, filterStatus, toastError]);

    useEffect(() => { loadRequests(); }, [loadRequests]);

    // Modal açıldığında ilçe listesini yükle ve role göre formu hazırla
    useEffect(() => {
        if (!showCreateModal) return;
        getDistricts().then(d => setDistricts(d)).catch(() => {});
        if (!isAdmin && user?.districtId) {
            setSelectedDistrictId(user.districtId);
            setForm({
                districtId: user.districtId,
                neighborhoodId: isNeighborhoodCoord ? (user.neighborhoodId ?? undefined) : undefined,
            });
        }
    }, [showCreateModal]); // eslint-disable-line react-hooks/exhaustive-deps

    // Seçili ilçeye göre mahalle listesini yükle
    useEffect(() => {
        if (!selectedDistrictId) {
            setNeighborhoods([]);
            return;
        }
        setLoadingNeighborhoods(true);
        getNeighborhoods(selectedDistrictId)
            .then(n => setNeighborhoods(n))
            .catch(() => {})
            .finally(() => setLoadingNeighborhoods(false));
    }, [selectedDistrictId]);

    const closeCreateModal = () => {
        setShowCreateModal(false);
        setForm({});
        setSelectedDistrictId('');
    };

    const handleCreate = async () => {
        const effectiveDistrictId = isAdmin ? form.districtId : user?.districtId;
        const effectiveNeighborhoodId = isNeighborhoodCoord
            ? (user?.neighborhoodId ?? undefined)
            : (form.neighborhoodId || undefined);

        if (!effectiveDistrictId || !form.resourceType) {
            toastWarning('İlçe ve kaynak türü zorunludur');
            return;
        }
        setCreating(true);
        try {
            const payload: CreateResourceRequestRequest = {
                districtId: effectiveDistrictId,
                neighborhoodId: effectiveNeighborhoodId,
                resourceType: form.resourceType!,
                quantity: form.quantity,
                description: form.description,
            };
            await createResourceRequest(payload);
            toastSuccess('Kaynak talebi başarıyla oluşturuldu');
            closeCreateModal();
            loadRequests();
        } catch (err: any) {
            toastError(err?.response?.data?.message || 'Oluşturma başarısız');
        } finally {
            setCreating(false);
        }
    };

    const handleUpdateStatus = async () => {
        if (!showStatusModal || !newStatus) return;
        setUpdatingStatus(true);
        try {
            await updateResourceRequestStatus(showStatusModal.id, { status: newStatus });
            toastSuccess('Durum güncellendi');
            setShowStatusModal(null);
            loadRequests();
        } catch (err: any) {
            toastError(err?.response?.data?.message || 'Güncelleme başarısız');
        } finally {
            setUpdatingStatus(false);
        }
    };

    const statusColor = (status: string) => {
        switch (status) {
            case 'OPEN': return 'bg-blue-100 text-blue-800';
            case 'IN_PROGRESS': return 'bg-yellow-100 text-yellow-800';
            case 'FULFILLED': return 'bg-green-100 text-green-800';
            case 'CANCELLED': return 'bg-gray-100 text-gray-600';
            default: return 'bg-gray-100 text-gray-600';
        }
    };

    return (
        <div className="space-y-5">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <Archive className="h-7 w-7 text-indigo-600" />
                    <h1 className="text-2xl font-bold text-gray-900">Kaynak ve Ekipman Talepleri</h1>
                </div>
                {canCreate && (
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                    >
                        <Plus className="h-4 w-4" />
                        Yeni Talep
                    </button>
                )}
            </div>

            {/* Filter */}
            <div className="flex gap-2 flex-wrap">
                {['', 'OPEN', 'IN_PROGRESS', 'FULFILLED', 'CANCELLED'].map(s => (
                    <button
                        key={s}
                        onClick={() => { setFilterStatus(s); setPage(0); }}
                        className={`px-3 py-1.5 text-sm rounded-lg border transition-colors ${filterStatus === s ? 'bg-indigo-600 text-white border-indigo-600' : 'border-gray-300 text-gray-600 hover:bg-gray-50'}`}
                    >
                        {s === '' ? 'Tümü' : RESOURCE_STATUSES.find(x => x.value === s)?.label || s}
                    </button>
                ))}
            </div>

            {loading ? (
                <p className="text-center text-gray-500 py-10">Yükleniyor...</p>
            ) : requests.length === 0 ? (
                <div className="text-center py-16">
                    <Archive className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                    <p className="text-gray-500">Kayıt bulunamadı</p>
                </div>
            ) : (
                <>
                    <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                        <table className="w-full text-sm">
                            <thead className="bg-gray-50 border-b border-gray-200">
                                <tr>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Kaynak</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Miktar</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Konum</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Oluşturan</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Durum</th>
                                    <th className="px-4 py-3"></th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {requests.map(r => (
                                    <tr key={r.id} className="hover:bg-gray-50">
                                        <td className="px-4 py-3">
                                            <p className="font-medium text-gray-800">{r.resourceTypeLabel}</p>
                                            {r.description && (
                                                <p className="text-xs text-gray-400 line-clamp-1 mt-0.5">{r.description}</p>
                                            )}
                                        </td>
                                        <td className="px-4 py-3 text-gray-600">
                                            {r.quantity ? `${r.quantity} adet` : '-'}
                                        </td>
                                        <td className="px-4 py-3 text-gray-600 text-xs">
                                            {r.neighborhoodName ? `${r.neighborhoodName}, ` : ''}{r.districtName}
                                        </td>
                                        <td className="px-4 py-3 text-gray-500 text-xs">
                                            {r.createdBy || '-'}
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className={`px-2 py-0.5 rounded-full text-xs ${statusColor(r.status)}`}>{r.statusLabel}</span>
                                            {r.closedBy && (
                                                <p className="text-xs text-gray-400 mt-0.5">{r.closedBy}</p>
                                            )}
                                        </td>
                                        <td className="px-4 py-3 flex items-center gap-1">
                                            <button
                                                onClick={() => setShowDetailModal(r)}
                                                className="p-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                                                title="Detayları Gör"
                                            >
                                                <Eye className="h-4 w-4" />
                                            </button>
                                            {canCreate && r.status !== 'FULFILLED' && r.status !== 'CANCELLED' && (
                                                <button
                                                    onClick={() => { setShowStatusModal(r); setNewStatus(r.status); }}
                                                    className="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                                                    title="Durumu Güncelle"
                                                >
                                                    <RefreshCw className="h-4 w-4" />
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

            {/* Detail Modal */}
            {showDetailModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl w-full max-w-md p-6 space-y-4">
                        <div className="flex items-start justify-between">
                            <h2 className="text-lg font-semibold text-gray-900">Kaynak Talebi Detayı</h2>
                            <button onClick={() => setShowDetailModal(null)} className="text-gray-400 hover:text-gray-600">
                                <X className="h-5 w-5" />
                            </button>
                        </div>

                        <div className="space-y-3 text-sm">
                            <div className="flex gap-2">
                                <span className="text-gray-500 w-32 flex-shrink-0">Kaynak Türü:</span>
                                <span className="text-gray-800 font-medium">{showDetailModal.resourceTypeLabel}</span>
                            </div>
                            {showDetailModal.quantity && (
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-32 flex-shrink-0">Miktar:</span>
                                    <span className="text-gray-800">{showDetailModal.quantity} adet</span>
                                </div>
                            )}
                            <div className="flex gap-2">
                                <span className="text-gray-500 w-32 flex-shrink-0">Konum:</span>
                                <span className="text-gray-800">
                                    {showDetailModal.neighborhoodName ? `${showDetailModal.neighborhoodName}, ` : ''}{showDetailModal.districtName}
                                </span>
                            </div>
                            {showDetailModal.description && (
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-32 flex-shrink-0">Açıklama:</span>
                                    <span className="text-gray-800">{showDetailModal.description}</span>
                                </div>
                            )}
                            <div className="flex gap-2">
                                <span className="text-gray-500 w-32 flex-shrink-0">Durum:</span>
                                <span className={`px-2 py-0.5 rounded-full text-xs ${statusColor(showDetailModal.status)}`}>
                                    {showDetailModal.statusLabel}
                                </span>
                            </div>
                            <div className="border-t border-gray-100 pt-3 space-y-2">
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-32 flex-shrink-0">Oluşturan:</span>
                                    <span className="text-gray-800">{showDetailModal.createdBy || '-'}</span>
                                </div>
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-32 flex-shrink-0">Oluşturma Tarihi:</span>
                                    <span className="text-gray-800">{new Date(showDetailModal.createdAt).toLocaleString('tr-TR')}</span>
                                </div>
                                {showDetailModal.closedBy && (
                                    <div className="flex gap-2">
                                        <span className="text-gray-500 w-32 flex-shrink-0">Kapatan:</span>
                                        <span className="text-gray-800">{showDetailModal.closedBy}</span>
                                    </div>
                                )}
                                {showDetailModal.closedAt && (
                                    <div className="flex gap-2">
                                        <span className="text-gray-500 w-32 flex-shrink-0">Kapanış Tarihi:</span>
                                        <span className="text-gray-800">{new Date(showDetailModal.closedAt).toLocaleString('tr-TR')}</span>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="flex justify-end">
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

            {/* Create Modal */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6 space-y-4">
                        <h2 className="text-lg font-semibold text-gray-900">Yeni Kaynak Talebi</h2>

                        {/* İlçe */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">İlçe *</label>
                            <select
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm disabled:bg-gray-50 disabled:text-gray-500 disabled:cursor-not-allowed"
                                value={selectedDistrictId}
                                disabled={districtLocked}
                                onChange={e => {
                                    setSelectedDistrictId(e.target.value);
                                    setForm(f => ({ ...f, districtId: e.target.value, neighborhoodId: undefined }));
                                }}
                            >
                                <option value="">Seçin</option>
                                {districts.map((d: any) => <option key={d.id} value={d.id}>{d.name}</option>)}
                            </select>
                            {districtLocked && isDistrictCoord && (
                                <p className="mt-1 text-xs text-gray-400">
                                    İlçeniz otomatik seçilmiştir. Bu ilçe içindeki mahalleler için talep oluşturabilirsiniz.
                                </p>
                            )}
                            {districtLocked && isNeighborhoodCoord && (
                                <p className="mt-1 text-xs text-gray-400">
                                    Bu alan, hesabınıza tanımlı bölgeye göre otomatik seçilmiştir.
                                </p>
                            )}
                        </div>

                        {/* Mahalle — ilçe seçilince göster */}
                        {selectedDistrictId && (
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Mahalle {neighborhoodLocked ? '' : '(isteğe bağlı)'}
                                </label>
                                <select
                                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm disabled:bg-gray-50 disabled:text-gray-500 disabled:cursor-not-allowed"
                                    value={form.neighborhoodId || ''}
                                    disabled={neighborhoodLocked || loadingNeighborhoods}
                                    onChange={e => setForm(f => ({ ...f, neighborhoodId: e.target.value || undefined }))}
                                >
                                    {loadingNeighborhoods ? (
                                        <option value="">Yükleniyor...</option>
                                    ) : (
                                        <>
                                            {!neighborhoodLocked && <option value="">İlçe geneli</option>}
                                            {neighborhoods.map((n: any) => (
                                                <option key={n.id} value={n.id}>{n.name}</option>
                                            ))}
                                        </>
                                    )}
                                </select>
                                {neighborhoodLocked && (
                                    <p className="mt-1 text-xs text-gray-400">
                                        Bu alan, hesabınıza tanımlı bölgeye göre otomatik seçilmiştir.
                                    </p>
                                )}
                            </div>
                        )}

                        {/* Kaynak Türü */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Kaynak Türü *</label>
                            <select className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                value={form.resourceType || ''}
                                onChange={e => setForm(f => ({ ...f, resourceType: e.target.value }))}>
                                <option value="">Seçin</option>
                                {RESOURCE_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                            </select>
                        </div>

                        {/* Miktar */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Miktar</label>
                            <input type="number" min="1" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                value={form.quantity ?? ''}
                                onChange={e => setForm(f => ({ ...f, quantity: e.target.value ? parseInt(e.target.value) : undefined }))} />
                        </div>

                        {/* Açıklama */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Açıklama</label>
                            <textarea rows={3} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none"
                                value={form.description || ''}
                                onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                                placeholder="Talep detaylarını açıklayın..." />
                        </div>

                        <div className="flex gap-3 pt-2">
                            <button onClick={closeCreateModal} className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50">İptal</button>
                            <button onClick={handleCreate} disabled={creating} className="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium disabled:opacity-60">
                                {creating ? 'Kaydediliyor...' : 'Kaydet'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Status Update Modal */}
            {showStatusModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl w-full max-w-sm p-6 space-y-4">
                        <h2 className="text-lg font-semibold text-gray-900">Durum Güncelle</h2>
                        <p className="text-sm text-gray-600">{showStatusModal.resourceTypeLabel} — {showStatusModal.districtName}</p>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Yeni Durum</label>
                            <select className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                value={newStatus} onChange={e => setNewStatus(e.target.value)}>
                                {RESOURCE_STATUSES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
                            </select>
                        </div>
                        <div className="flex gap-3">
                            <button onClick={() => setShowStatusModal(null)} className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50">İptal</button>
                            <button onClick={handleUpdateStatus} disabled={updatingStatus} className="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium disabled:opacity-60">
                                {updatingStatus ? 'Güncelleniyor...' : 'Güncelle'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
