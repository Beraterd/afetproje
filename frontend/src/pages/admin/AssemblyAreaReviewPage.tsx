import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    getAssemblyAreasForReview,
    getAssemblyAreaStats,
    updateAssemblyArea,
    bulkUpdateAssemblyAreas,
    AssemblyAreaFilters,
    UpdateAssemblyAreaRequest,
} from '@/api/assemblyAreas.api';
import { getDistricts } from '@/api/districts.api';
import { getNeighborhoods } from '@/api/neighborhoods.api';
import { queryKeys } from '@/utils/queryKeys';
import { AdminAssemblyAreaResponse, DistrictResponse, NeighborhoodSummaryResponse } from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { Modal } from '@/components/ui/Modal';
import { Button, Badge } from '@/components/ui';
import { LocationPickerMap } from '@/components/map/LocationPickerMap';
import {
    MapPin,
    Search,
    Filter,
    CheckCircle,
    XCircle,
    AlertCircle,
    ExternalLink,
    Edit3,
    Map,
    Eye,
    EyeOff,
} from 'lucide-react';

// ─── helpers ─────────────────────────────────────────────────────────────────

function buildMapsUrl(lat: number, lng: number) {
    return `https://www.google.com/maps?q=${lat},${lng}`;
}

const SOURCE_OPTIONS = [
    'Kadıköy Belediyesi PDF',
    'Manuel Giriş',
    'Diğer',
];

// ─── sub-components ───────────────────────────────────────────────────────────

const CoordBadge: React.FC<{ lat: number | null; lng: number | null }> = ({ lat, lng }) => {
    if (lat !== null && lng !== null) {
        return (
            <span className="inline-flex items-center gap-1 text-xs text-green-700 bg-green-50 px-2 py-0.5 rounded-full font-mono">
                <CheckCircle className="h-3 w-3" />
                {lat.toFixed(5)}, {lng.toFixed(5)}
            </span>
        );
    }
    return (
        <span className="inline-flex items-center gap-1 text-xs text-red-600 bg-red-50 px-2 py-0.5 rounded-full">
            <XCircle className="h-3 w-3" />
            Koordinatsız
        </span>
    );
};

// ─── edit modal ───────────────────────────────────────────────────────────────

interface EditModalProps {
    area: AdminAssemblyAreaResponse | null;
    onClose: () => void;
    onSaved: () => void;
}

const EditModal: React.FC<EditModalProps> = ({ area, onClose, onSaved }) => {
    const toast = useToast();
    const queryClient = useQueryClient();

    const [form, setForm] = useState<UpdateAssemblyAreaRequest>({});
    const [showMap, setShowMap] = useState(false);
    const [mapLat, setMapLat] = useState<number | null>(null);
    const [mapLng, setMapLng] = useState<number | null>(null);
    const [locationConfirmed, setLocationConfirmed] = useState(false);
    const [autoMapsUrl, setAutoMapsUrl] = useState<string | null>(null);

    useEffect(() => {
        if (!area) return;
        setForm({
            name: area.name,
            address: area.address ?? '',
            latitude: area.latitude ?? undefined,
            longitude: area.longitude ?? undefined,
            googleMapsUrl: area.googleMapsUrl ?? '',
            needsReview: area.needsReview,
            active: area.active,
        });
        setMapLat(area.latitude);
        setMapLng(area.longitude);
        setLocationConfirmed(false);
        setAutoMapsUrl(null);
        setShowMap(false);
    }, [area]);

    // Auto-generate maps URL whenever lat/lng change
    useEffect(() => {
        if (form.latitude != null && form.longitude != null) {
            setAutoMapsUrl(buildMapsUrl(form.latitude, form.longitude));
        } else {
            setAutoMapsUrl(null);
        }
    }, [form.latitude, form.longitude]);

    const mutation = useMutation({
        mutationFn: (data: UpdateAssemblyAreaRequest) => updateAssemblyArea(area!.id, data),
        onSuccess: () => {
            toast.success('Toplanma alanı güncellendi');
            queryClient.invalidateQueries({ queryKey: ['assemblyAreas'] });
            onSaved();
        },
        onError: (err: any) => toast.error(err.message || 'Güncelleme başarısız'),
    });

    const handleMapPick = (lat: number, lng: number) => {
        setMapLat(lat);
        setMapLng(lng);
        setLocationConfirmed(false);
        setForm((f) => ({ ...f, latitude: lat, longitude: lng }));
    };

    const handleMapConfirm = () => {
        setLocationConfirmed(true);
    };

    const handleSave = () => {
        const payload: UpdateAssemblyAreaRequest = {
            ...form,
            // If user didn't explicitly set googleMapsUrl, let backend auto-generate from coords
            googleMapsUrl: form.googleMapsUrl || undefined,
        };
        mutation.mutate(payload);
    };

    const districtId = area?.district?.id ?? '';
    const neighborhoodId = area?.neighborhood?.id ?? '';

    if (!area) return null;

    return (
        <Modal
            isOpen={!!area}
            onClose={onClose}
            title="Toplanma Alanı Düzenle"
            size="lg"
            footer={
                <div className="flex justify-end gap-3">
                    <Button variant="ghost" onClick={onClose} disabled={mutation.isPending}>
                        İptal
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleSave}
                        loading={mutation.isPending}
                        leftIcon={<CheckCircle className="h-4 w-4" />}
                    >
                        Kaydet
                    </Button>
                </div>
            }
        >
            <div className="space-y-5">
                {/* Meta info */}
                <div className="bg-gray-50 rounded-lg p-3 text-xs text-gray-500 space-y-1">
                    <div><span className="font-medium text-gray-700">İlçe:</span> {area.district?.name ?? '—'}</div>
                    <div><span className="font-medium text-gray-700">Mahalle:</span> {area.neighborhood?.name ?? '—'}</div>
                    <div><span className="font-medium text-gray-700">Kaynak:</span> {area.sourceName ?? '—'}</div>
                    {area.sourceReference && (
                        <div><span className="font-medium text-gray-700">Referans:</span> {area.sourceReference}</div>
                    )}
                </div>

                {/* Name */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Alan Adı</label>
                    <input
                        type="text"
                        value={form.name ?? ''}
                        onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                        className="block w-full rounded-md border-0 py-1.5 px-3 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm"
                    />
                </div>

                {/* Address */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Adres</label>
                    <textarea
                        rows={2}
                        value={form.address ?? ''}
                        onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))}
                        className="block w-full rounded-md border-0 py-1.5 px-3 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm resize-none"
                    />
                </div>

                {/* Coordinates */}
                <div>
                    <div className="flex items-center justify-between mb-2">
                        <label className="block text-sm font-medium text-gray-700">Koordinatlar</label>
                        <button
                            type="button"
                            onClick={() => setShowMap((v) => !v)}
                            className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-md border border-blue-300 text-blue-600 hover:bg-blue-50 transition-colors"
                        >
                            <Map className="h-3.5 w-3.5" />
                            {showMap ? 'Haritayı Gizle' : 'Haritadan Seç'}
                        </button>
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs text-gray-500 mb-1">Enlem (Latitude)</label>
                            <input
                                type="number"
                                step="0.0000001"
                                placeholder="ör. 40.993456"
                                value={form.latitude ?? ''}
                                onChange={(e) => {
                                    const v = e.target.value === '' ? undefined : parseFloat(e.target.value);
                                    setForm((f) => ({ ...f, latitude: v }));
                                    if (v != null) setMapLat(v);
                                }}
                                className="block w-full rounded-md border-0 py-1.5 px-3 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm font-mono"
                            />
                        </div>
                        <div>
                            <label className="block text-xs text-gray-500 mb-1">Boylam (Longitude)</label>
                            <input
                                type="number"
                                step="0.0000001"
                                placeholder="ör. 29.034567"
                                value={form.longitude ?? ''}
                                onChange={(e) => {
                                    const v = e.target.value === '' ? undefined : parseFloat(e.target.value);
                                    setForm((f) => ({ ...f, longitude: v }));
                                    if (v != null) setMapLng(v);
                                }}
                                className="block w-full rounded-md border-0 py-1.5 px-3 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm font-mono"
                            />
                        </div>
                    </div>

                    {/* Google Maps URL preview */}
                    {autoMapsUrl && (
                        <div className="mt-2 flex items-center gap-2 text-xs text-gray-500">
                            <CheckCircle className="h-3.5 w-3.5 text-green-500 shrink-0" />
                            <span className="text-gray-600">Google Maps URL otomatik oluşacak:</span>
                            <a
                                href={autoMapsUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-blue-600 hover:underline truncate flex items-center gap-1"
                            >
                                {autoMapsUrl}
                                <ExternalLink className="h-3 w-3 shrink-0" />
                            </a>
                        </div>
                    )}

                    {/* Map picker */}
                    {showMap && (
                        <div className="mt-3">
                            <LocationPickerMap
                                districtId={districtId}
                                neighborhoodId={neighborhoodId}
                                lat={mapLat}
                                lng={mapLng}
                                locationConfirmed={locationConfirmed}
                                onChange={handleMapPick}
                                onConfirm={handleMapConfirm}
                            />
                        </div>
                    )}
                </div>

                {/* Status toggles */}
                <div className="flex gap-6 pt-1 border-t border-gray-100">
                    <label className="flex items-center gap-3 cursor-pointer">
                        <div className="relative">
                            <input
                                type="checkbox"
                                className="sr-only"
                                checked={!(form.needsReview ?? true)}
                                onChange={(e) => setForm((f) => ({ ...f, needsReview: !e.target.checked }))}
                            />
                            <div
                                onClick={() => setForm((f) => ({ ...f, needsReview: !f.needsReview }))}
                                className={`w-10 h-6 rounded-full cursor-pointer transition-colors ${
                                    !form.needsReview ? 'bg-green-500' : 'bg-gray-300'
                                }`}
                            >
                                <div
                                    className={`absolute top-1 left-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${
                                        !form.needsReview ? 'translate-x-4' : 'translate-x-0'
                                    }`}
                                />
                            </div>
                        </div>
                        <div>
                            <div className="text-sm font-medium text-gray-900">Doğrulandı</div>
                            <div className="text-xs text-gray-500">
                                {form.needsReview ? 'Doğrulama bekleniyor' : 'Simülasyon mailine dahil edilecek'}
                            </div>
                        </div>
                    </label>

                    <label className="flex items-center gap-3 cursor-pointer">
                        <div className="relative">
                            <div
                                onClick={() => setForm((f) => ({ ...f, active: !f.active }))}
                                className={`w-10 h-6 rounded-full cursor-pointer transition-colors ${
                                    form.active ? 'bg-blue-500' : 'bg-gray-300'
                                }`}
                            >
                                <div
                                    className={`absolute top-1 left-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${
                                        form.active ? 'translate-x-4' : 'translate-x-0'
                                    }`}
                                />
                            </div>
                        </div>
                        <div>
                            <div className="text-sm font-medium text-gray-900">Aktif</div>
                            <div className="text-xs text-gray-500">
                                {form.active ? 'Kayıt aktif' : 'Kayıt pasif'}
                            </div>
                        </div>
                    </label>
                </div>
            </div>
        </Modal>
    );
};

// ─── main page ────────────────────────────────────────────────────────────────

export const AssemblyAreaReviewPage: React.FC = () => {
    const toast = useToast();
    const queryClient = useQueryClient();

    // Filters
    const [districtId, setDistrictId] = useState('');
    const [neighborhoodId, setNeighborhoodId] = useState('');
    const [sourceName, setSourceName] = useState('');
    const [needsReviewFilter, setNeedsReviewFilter] = useState<'all' | 'true' | 'false'>('true');
    const [activeFilter, setActiveFilter] = useState<'all' | 'true' | 'false'>('all');
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');

    // Pagination
    const [page, setPage] = useState(0);
    const pageSize = 20;

    // Selection
    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

    // Edit modal
    const [editingArea, setEditingArea] = useState<AdminAssemblyAreaResponse | null>(null);

    // Districts / neighborhoods for filter dropdowns
    const [districts, setDistricts] = useState<DistrictResponse[]>([]);
    const [filterNeighborhoods, setFilterNeighborhoods] = useState<NeighborhoodSummaryResponse[]>([]);

    // Debounce search
    useEffect(() => {
        const t = setTimeout(() => setDebouncedSearch(search), 400);
        return () => clearTimeout(t);
    }, [search]);

    // Reset page on filter change
    useEffect(() => { setPage(0); setSelectedIds(new Set()); }, [
        districtId, neighborhoodId, sourceName, needsReviewFilter, activeFilter, debouncedSearch
    ]);

    // Load districts for filter
    useEffect(() => {
        getDistricts().then(setDistricts).catch(() => {});
    }, []);

    // Load neighborhoods when district changes
    useEffect(() => {
        if (districtId) {
            getNeighborhoods(districtId).then(setFilterNeighborhoods).catch(() => {});
            setNeighborhoodId('');
        } else {
            setFilterNeighborhoods([]);
            setNeighborhoodId('');
        }
    }, [districtId]);

    const filters: AssemblyAreaFilters = {
        districtId: districtId || undefined,
        neighborhoodId: neighborhoodId || undefined,
        sourceName: sourceName || undefined,
        needsReview: needsReviewFilter === 'all' ? undefined : needsReviewFilter === 'true',
        active: activeFilter === 'all' ? undefined : activeFilter === 'true',
        search: debouncedSearch || undefined,
        page,
        size: pageSize,
    };

    const { data, isLoading } = useQuery({
        queryKey: queryKeys.assemblyAreas.list(filters),
        queryFn: () => getAssemblyAreasForReview(filters),
    });

    const { data: stats } = useQuery({
        queryKey: queryKeys.assemblyAreas.stats(),
        queryFn: getAssemblyAreaStats,
    });

    const bulkMutation = useMutation({
        mutationFn: bulkUpdateAssemblyAreas,
        onSuccess: (res) => {
            toast.success(`${res.updated} kayıt güncellendi`);
            setSelectedIds(new Set());
            queryClient.invalidateQueries({ queryKey: ['assemblyAreas'] });
        },
        onError: (err: any) => toast.error(err.message || 'Toplu işlem başarısız'),
    });

    const rows = data?.content ?? [];
    const totalPages = data?.totalPages ?? 0;

    const allPageSelected = rows.length > 0 && rows.every((r) => selectedIds.has(r.id));
    const someSelected = selectedIds.size > 0;

    const toggleAll = () => {
        if (allPageSelected) {
            const next = new Set(selectedIds);
            rows.forEach((r) => next.delete(r.id));
            setSelectedIds(next);
        } else {
            const next = new Set(selectedIds);
            rows.forEach((r) => next.add(r.id));
            setSelectedIds(next);
        }
    };

    const toggleOne = (id: string) => {
        const next = new Set(selectedIds);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        setSelectedIds(next);
    };

    const handleBulk = (action: 'approve' | 'deactivate' | 'activate') => {
        const ids = Array.from(selectedIds);
        if (action === 'approve') bulkMutation.mutate({ ids, needsReview: false });
        else if (action === 'deactivate') bulkMutation.mutate({ ids, active: false });
        else if (action === 'activate') bulkMutation.mutate({ ids, active: true });
    };

    return (
        <div className="space-y-5">
            {/* Header */}
            <div className="sm:flex sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-bold leading-7 text-gray-900 flex items-center gap-2">
                        <MapPin className="h-6 w-6 text-blue-600" />
                        Toplanma Alanı Doğrulama
                    </h1>
                    <p className="mt-1 text-sm text-gray-500">
                        PDF'den içe alınan veya doğrulanmamış toplanma alanlarını gözden geçirin ve koordinat ekleyin.
                    </p>
                </div>
                {stats && (
                    <div className="mt-3 sm:mt-0 flex items-center gap-3">
                        <div className="text-center px-4 py-2 bg-amber-50 border border-amber-200 rounded-lg">
                            <div className="text-2xl font-bold text-amber-700">{stats.pendingReview}</div>
                            <div className="text-xs text-amber-600">Doğrulama Bekliyor</div>
                        </div>
                        <div className="text-center px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg">
                            <div className="text-2xl font-bold text-gray-700">{stats.total}</div>
                            <div className="text-xs text-gray-500">Toplam Kayıt</div>
                        </div>
                    </div>
                )}
            </div>

            {/* Filters */}
            <div className="bg-white shadow rounded-lg p-4 space-y-3">
                <div className="flex items-center gap-2 text-sm font-medium text-gray-700">
                    <Filter className="h-4 w-4" />
                    Filtreler
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                    {/* Search */}
                    <div className="relative sm:col-span-2 lg:col-span-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                        <input
                            type="text"
                            placeholder="Alan adı veya adres ara..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="block w-full rounded-md border-0 py-1.5 pl-9 pr-3 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm"
                        />
                    </div>

                    {/* District */}
                    <select
                        value={districtId}
                        onChange={(e) => setDistrictId(e.target.value)}
                        className="block w-full rounded-md border-0 py-1.5 pl-3 pr-8 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm"
                    >
                        <option value="">Tüm İlçeler</option>
                        {districts.map((d) => (
                            <option key={d.id} value={d.id}>{d.name}</option>
                        ))}
                    </select>

                    {/* Neighborhood */}
                    <select
                        value={neighborhoodId}
                        onChange={(e) => setNeighborhoodId(e.target.value)}
                        disabled={!districtId}
                        className="block w-full rounded-md border-0 py-1.5 pl-3 pr-8 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm disabled:opacity-50"
                    >
                        <option value="">Tüm Mahalleler</option>
                        {filterNeighborhoods.map((n) => (
                            <option key={n.id} value={n.id}>{n.name}</option>
                        ))}
                    </select>

                    {/* Source */}
                    <select
                        value={sourceName}
                        onChange={(e) => setSourceName(e.target.value)}
                        className="block w-full rounded-md border-0 py-1.5 pl-3 pr-8 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm"
                    >
                        <option value="">Tüm Kaynaklar</option>
                        {SOURCE_OPTIONS.map((s) => (
                            <option key={s} value={s}>{s}</option>
                        ))}
                    </select>

                    {/* Needs review */}
                    <select
                        value={needsReviewFilter}
                        onChange={(e) => setNeedsReviewFilter(e.target.value as any)}
                        className="block w-full rounded-md border-0 py-1.5 pl-3 pr-8 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm"
                    >
                        <option value="true">Doğrulama Bekleyenler</option>
                        <option value="false">Doğrulanmışlar</option>
                        <option value="all">Tümü</option>
                    </select>

                    {/* Active */}
                    <select
                        value={activeFilter}
                        onChange={(e) => setActiveFilter(e.target.value as any)}
                        className="block w-full rounded-md border-0 py-1.5 pl-3 pr-8 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm"
                    >
                        <option value="all">Aktif + Pasif</option>
                        <option value="true">Yalnızca Aktif</option>
                        <option value="false">Yalnızca Pasif</option>
                    </select>
                </div>
            </div>

            {/* Bulk action bar */}
            {someSelected && (
                <div className="bg-blue-50 border border-blue-200 rounded-lg px-4 py-3 flex items-center gap-4">
                    <span className="text-sm font-medium text-blue-800">
                        {selectedIds.size} kayıt seçildi
                    </span>
                    <div className="flex gap-2 flex-wrap">
                        <Button
                            size="sm"
                            variant="primary"
                            onClick={() => handleBulk('approve')}
                            loading={bulkMutation.isPending}
                            leftIcon={<CheckCircle className="h-3.5 w-3.5" />}
                        >
                            Doğrulandı İşaretle
                        </Button>
                        <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => handleBulk('deactivate')}
                            loading={bulkMutation.isPending}
                            leftIcon={<EyeOff className="h-3.5 w-3.5" />}
                        >
                            Pasif Yap
                        </Button>
                        <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => handleBulk('activate')}
                            loading={bulkMutation.isPending}
                            leftIcon={<Eye className="h-3.5 w-3.5" />}
                        >
                            Aktif Yap
                        </Button>
                        <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => setSelectedIds(new Set())}
                        >
                            Seçimi Temizle
                        </Button>
                    </div>
                </div>
            )}

            {/* Table */}
            <div className="bg-white shadow rounded-lg overflow-hidden">
                {isLoading ? (
                    <div className="flex items-center justify-center py-16 text-gray-400">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
                    </div>
                ) : rows.length === 0 ? (
                    <div className="text-center py-16">
                        <MapPin className="mx-auto h-10 w-10 text-gray-300 mb-3" />
                        <p className="text-gray-500 text-sm">Bu filtrelere uyan kayıt bulunamadı.</p>
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200 text-sm">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-4 py-3 text-left">
                                        <input
                                            type="checkbox"
                                            checked={allPageSelected}
                                            onChange={toggleAll}
                                            className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                        />
                                    </th>
                                    <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-xs">Alan Adı</th>
                                    <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-xs">Mahalle / İlçe</th>
                                    <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-xs">Adres</th>
                                    <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-xs">Kaynak</th>
                                    <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-xs">Koordinat</th>
                                    <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-xs">Durum</th>
                                    <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-xs">Maps</th>
                                    <th className="px-4 py-3" />
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100 bg-white">
                                {rows.map((row) => (
                                    <tr
                                        key={row.id}
                                        className={`hover:bg-gray-50 transition-colors ${
                                            selectedIds.has(row.id) ? 'bg-blue-50' : ''
                                        }`}
                                    >
                                        <td className="px-4 py-3">
                                            <input
                                                type="checkbox"
                                                checked={selectedIds.has(row.id)}
                                                onChange={() => toggleOne(row.id)}
                                                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                            />
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="font-medium text-gray-900 max-w-[200px]">{row.name}</div>
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="text-gray-900">{row.neighborhood?.name ?? '—'}</div>
                                            <div className="text-xs text-gray-400">{row.district?.name ?? '—'}</div>
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="text-gray-600 text-xs max-w-[180px] line-clamp-2">
                                                {row.address ?? <span className="text-gray-300 italic">Adres yok</span>}
                                            </div>
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded">
                                                {row.sourceName ?? '—'}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <CoordBadge lat={row.latitude} lng={row.longitude} />
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="flex flex-col gap-1">
                                                {row.needsReview ? (
                                                    <span className="inline-flex items-center gap-1 text-xs text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full">
                                                        <AlertCircle className="h-3 w-3" />
                                                        İnceleme bekliyor
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center gap-1 text-xs text-green-700 bg-green-50 px-2 py-0.5 rounded-full">
                                                        <CheckCircle className="h-3 w-3" />
                                                        Doğrulandı
                                                    </span>
                                                )}
                                                {!row.active && (
                                                    <span className="inline-flex items-center gap-1 text-xs text-red-600 bg-red-50 px-2 py-0.5 rounded-full">
                                                        <XCircle className="h-3 w-3" />
                                                        Pasif
                                                    </span>
                                                )}
                                            </div>
                                        </td>
                                        <td className="px-4 py-3">
                                            {row.googleMapsUrl ? (
                                                <a
                                                    href={row.googleMapsUrl}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="inline-flex items-center gap-1 text-xs text-blue-600 hover:underline"
                                                >
                                                    <ExternalLink className="h-3 w-3" />
                                                    Harita
                                                </a>
                                            ) : (
                                                <span className="text-xs text-gray-300">—</span>
                                            )}
                                        </td>
                                        <td className="px-4 py-3">
                                            <button
                                                onClick={() => setEditingArea(row)}
                                                className="inline-flex items-center gap-1 text-xs px-2 py-1 rounded border border-gray-200 text-gray-600 hover:bg-gray-50 hover:border-gray-300 transition-colors"
                                            >
                                                <Edit3 className="h-3.5 w-3.5" />
                                                Düzenle
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* Pagination */}
                {totalPages > 1 && (
                    <div className="border-t border-gray-200 px-4 py-3 flex items-center justify-between bg-gray-50">
                        <span className="text-xs text-gray-500">
                            Sayfa {page + 1} / {totalPages}
                            {data && <> &nbsp;·&nbsp; {data.totalElements} kayıt</>}
                        </span>
                        <div className="flex gap-2">
                            <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => setPage((p) => p - 1)}
                                disabled={page === 0}
                            >
                                ← Önceki
                            </Button>
                            <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => setPage((p) => p + 1)}
                                disabled={page >= totalPages - 1}
                            >
                                Sonraki →
                            </Button>
                        </div>
                    </div>
                )}
            </div>

            {/* Edit Modal */}
            <EditModal
                area={editingArea}
                onClose={() => setEditingArea(null)}
                onSaved={() => setEditingArea(null)}
            />
        </div>
    );
};
