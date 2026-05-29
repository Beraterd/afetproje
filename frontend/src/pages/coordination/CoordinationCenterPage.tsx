import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/components/shared/ToastProvider';
import { queryKeys } from '@/utils/queryKeys';
import { getMapDistricts, getMapNeighborhoods } from '@/api/map.api';
import {
    getDistrictCenter,
    getNeighborhoodCenter,
    upsertDistrictCenter,
    upsertNeighborhoodCenter,
} from '@/api/coordinationCenters.api';
import { MapDistrictResponse, MapNeighborhoodResponse } from '@/types';
import { CoordinationCenterResponse, UpsertCoordinationCenterRequest } from '@/types/coordinationCenter';
import { CenterLocationPickerMap } from '@/components/map/CenterLocationPickerMap';
import { LoadingSpinner } from '@/components/ui';
import { Building2, CheckCircle, MapPin, Save } from 'lucide-react';

// ── Shared form state ─────────────────────────────────────────────────────────
interface CenterFormState {
    address: string;
    streetName: string;
    buildingNo: string;
    lat: number | null;
    lng: number | null;
    locationConfirmed: boolean;
}

const defaultForm = (): CenterFormState => ({
    address: '',
    streetName: '',
    buildingNo: '',
    lat: null,
    lng: null,
    locationConfirmed: false,
});

function formFromResponse(center: CoordinationCenterResponse): CenterFormState {
    return {
        address: center.address,
        streetName: center.streetName ?? '',
        buildingNo: center.buildingNo ?? '',
        lat: center.latitude,
        lng: center.longitude,
        locationConfirmed: center.locationVerified,
    };
}

// ── CenterForm component ──────────────────────────────────────────────────────
interface CenterFormProps {
    polygon: any;
    boundaryLabel: string;
    areaName?: string;
    districtName?: string;
    existingCenter?: CoordinationCenterResponse;
    onSave: (req: UpsertCoordinationCenterRequest) => Promise<CoordinationCenterResponse>;
    saving: boolean;
}

const CenterForm: React.FC<CenterFormProps> = ({
    polygon,
    boundaryLabel,
    areaName,
    districtName,
    existingCenter,
    onSave,
    saving,
}) => {
    const [form, setForm] = useState<CenterFormState>(
        existingCenter ? formFromResponse(existingCenter) : defaultForm()
    );

    // Reset form when existingCenter changes (e.g. switching districts)
    React.useEffect(() => {
        setForm(existingCenter ? formFromResponse(existingCenter) : defaultForm());
    }, [existingCenter]);

    const setField = <K extends keyof CenterFormState>(key: K, value: CenterFormState[K]) =>
        setForm(prev => ({ ...prev, [key]: value }));

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.lat || !form.lng) return;
        if (!form.locationConfirmed) return;
        await onSave({
            address: form.address.trim(),
            streetName: form.streetName.trim() || undefined,
            buildingNo: form.buildingNo.trim() || undefined,
            latitude: form.lat,
            longitude: form.lng,
            locationVerified: form.locationConfirmed,
        });
    };

    const isValid = form.address.trim() && form.lat !== null && form.lng !== null && form.locationConfirmed;

    return (
        <form onSubmit={handleSubmit} className="space-y-5">
            {/* Adres bilgileri */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="sm:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                        Adres <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="text"
                        value={form.address}
                        onChange={e => setField('address', e.target.value)}
                        placeholder="Açık adres giriniz"
                        required
                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Cadde / Sokak</label>
                    <input
                        type="text"
                        value={form.streetName}
                        onChange={e => {
                            setField('streetName', e.target.value);
                            setField('locationConfirmed', false);
                        }}
                        placeholder="Cadde veya sokak adı"
                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Bina No</label>
                    <input
                        type="text"
                        value={form.buildingNo}
                        onChange={e => {
                            setField('buildingNo', e.target.value);
                            setField('locationConfirmed', false);
                        }}
                        placeholder="Kapı / bina numarası"
                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                </div>
            </div>

            {/* Harita */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                    Harita Konumu <span className="text-red-500">*</span>
                </label>
                <CenterLocationPickerMap
                    polygon={polygon}
                    boundaryLabel={boundaryLabel}
                    streetName={form.streetName}
                    buildingNo={form.buildingNo}
                    areaName={areaName}
                    districtName={districtName}
                    lat={form.lat}
                    lng={form.lng}
                    locationConfirmed={form.locationConfirmed}
                    onChange={(lat, lng) => {
                        setField('lat', lat);
                        setField('lng', lng);
                        setField('locationConfirmed', false);
                    }}
                    onConfirm={() => setField('locationConfirmed', true)}
                />
            </div>

            {/* Doğrulama uyarısı */}
            {form.lat !== null && !form.locationConfirmed && (
                <p className="text-xs text-amber-600 flex items-center gap-1.5">
                    <MapPin className="h-3.5 w-3.5" />
                    Kaydetmeden önce konumu onaylamanız gerekiyor.
                </p>
            )}

            {/* Kaydet butonu */}
            <div className="flex justify-end pt-2">
                <button
                    type="submit"
                    disabled={!isValid || saving}
                    className="flex items-center gap-2 px-5 py-2.5 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg transition-colors"
                >
                    {saving ? (
                        <LoadingSpinner size="sm" />
                    ) : (
                        <Save className="h-4 w-4" />
                    )}
                    {saving ? 'Kaydediliyor...' : 'Kaydet'}
                </button>
            </div>
        </form>
    );
};

// ── District section ──────────────────────────────────────────────────────────
const DistrictSection: React.FC<{
    districtId: string;
    district?: MapDistrictResponse;
}> = ({ districtId, district }) => {
    const queryClient = useQueryClient();
    const { success, error } = useToast();

    const { data: center, isLoading } = useQuery({
        queryKey: queryKeys.coordinationCenters.district(districtId),
        queryFn: () => getDistrictCenter(districtId),
        retry: (failCount, err: any) => err?.response?.status !== 404 && failCount < 2,
    });

    const { mutateAsync: save, isPending: saving } = useMutation({
        mutationFn: (req: UpsertCoordinationCenterRequest) => upsertDistrictCenter(districtId, req),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.coordinationCenters.district(districtId) });
            queryClient.invalidateQueries({ queryKey: queryKeys.coordinationCenters.myStatus() });
            success('Koordinatörlük merkezi başarıyla kaydedildi.');
        },
        onError: (err: any) => {
            const msg = err?.response?.data?.message ?? 'Kaydetme sırasında bir hata oluştu.';
            error(msg);
        },
    });

    if (isLoading) {
        return (
            <div className="flex justify-center py-12">
                <LoadingSpinner size="lg" />
            </div>
        );
    }

    return (
        <div>
            {center ? (
                <div className="flex items-center gap-2 mb-4 text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg px-4 py-2.5">
                    <CheckCircle className="h-4 w-4 flex-shrink-0" />
                    <span>Merkez tanımlı — güncelleme yapabilirsiniz.</span>
                </div>
            ) : (
                <div className="flex items-center gap-2 mb-4 text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-4 py-2.5">
                    <MapPin className="h-4 w-4 flex-shrink-0" />
                    <span>Bu ilçe için henüz koordinatörlük merkezi tanımlanmamış.</span>
                </div>
            )}
            <CenterForm
                polygon={district?.polygon ?? null}
                boundaryLabel="ilçe"
                areaName={district?.name}
                districtName={district?.name}
                existingCenter={center}
                onSave={save}
                saving={saving}
            />
        </div>
    );
};

// ── Neighborhood section ──────────────────────────────────────────────────────
const NeighborhoodSection: React.FC<{
    neighborhoodId: string;
    neighborhood?: MapNeighborhoodResponse;
    districtName?: string;
}> = ({ neighborhoodId, neighborhood, districtName }) => {
    const queryClient = useQueryClient();
    const { success, error } = useToast();

    const { data: center, isLoading } = useQuery({
        queryKey: queryKeys.coordinationCenters.neighborhood(neighborhoodId),
        queryFn: () => getNeighborhoodCenter(neighborhoodId),
        retry: (failCount, err: any) => err?.response?.status !== 404 && failCount < 2,
    });

    const { mutateAsync: save, isPending: saving } = useMutation({
        mutationFn: (req: UpsertCoordinationCenterRequest) => upsertNeighborhoodCenter(neighborhoodId, req),
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: queryKeys.coordinationCenters.neighborhood(neighborhoodId),
            });
            queryClient.invalidateQueries({ queryKey: queryKeys.coordinationCenters.myStatus() });
            success('Koordinatörlük merkezi başarıyla kaydedildi.');
        },
        onError: (err: any) => {
            const msg = err?.response?.data?.message ?? 'Kaydetme sırasında bir hata oluştu.';
            error(msg);
        },
    });

    if (isLoading) {
        return (
            <div className="flex justify-center py-12">
                <LoadingSpinner size="lg" />
            </div>
        );
    }

    return (
        <div>
            {center ? (
                <div className="flex items-center gap-2 mb-4 text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg px-4 py-2.5">
                    <CheckCircle className="h-4 w-4 flex-shrink-0" />
                    <span>Merkez tanımlı — güncelleme yapabilirsiniz.</span>
                </div>
            ) : (
                <div className="flex items-center gap-2 mb-4 text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-4 py-2.5">
                    <MapPin className="h-4 w-4 flex-shrink-0" />
                    <span>Bu mahalle için henüz koordinatörlük merkezi tanımlanmamış.</span>
                </div>
            )}
            <CenterForm
                polygon={neighborhood?.polygon ?? null}
                boundaryLabel="mahalle"
                areaName={neighborhood?.name}
                districtName={districtName}
                existingCenter={center}
                onSave={save}
                saving={saving}
            />
        </div>
    );
};

// ── Main page ─────────────────────────────────────────────────────────────────
export const CoordinationCenterPage: React.FC = () => {
    const { user } = useAuthStore();
    const role = user?.role;

    const [activeTab, setActiveTab] = useState<'district' | 'neighborhood'>('district');
    const [adminDistrictId, setAdminDistrictId] = useState<string>('');
    const [adminNeighborhoodDistrictId, setAdminNeighborhoodDistrictId] = useState<string>('');
    const [adminNeighborhoodId, setAdminNeighborhoodId] = useState<string>('');

    // DC-specific tab state
    const [dcTab, setDcTab] = useState<'district' | 'neighborhood'>('district');
    const [dcNeighborhoodId, setDcNeighborhoodId] = useState<string>('');

    // Fetch district list (for all roles — used for map polygons)
    const { data: allDistricts = [], isLoading: loadingDistricts } = useQuery({
        queryKey: queryKeys.map.districts(),
        queryFn: getMapDistricts,
    });

    // Fetch neighborhoods for admin neighborhood tab
    const { data: adminNeighborhoods = [], isLoading: loadingAdminNeighborhoods } = useQuery({
        queryKey: queryKeys.map.neighborhoods(adminNeighborhoodDistrictId),
        queryFn: () => getMapNeighborhoods(adminNeighborhoodDistrictId),
        enabled: !!adminNeighborhoodDistrictId,
    });

    // Coordinator-aware IDs from auth store.
    // Backend enriches login/me response so that for DC: districtId = assigned district,
    // for NC: neighborhoodId = assigned neighborhood AND districtId = containing district.
    const dcDistrictId = user?.districtId ?? '';
    const ncNeighborhoodId = user?.neighborhoodId ?? '';
    const ncDistrictId = user?.districtId ?? ''; // For NC: backend sets districtId = neighborhood's district

    // NC: fetch neighborhoods in the district that contains the assigned neighborhood
    const { data: ncNeighborhoods = [] } = useQuery({
        queryKey: queryKeys.map.neighborhoods(ncDistrictId),
        queryFn: () => getMapNeighborhoods(ncDistrictId),
        enabled: role === 'NEIGHBORHOOD_COORDINATOR' && !!ncDistrictId,
    });

    // DC: fetch neighborhoods in their assigned district for neighborhood center management
    const { data: dcNeighborhoods = [], isLoading: loadingDcNeighborhoods } = useQuery({
        queryKey: queryKeys.map.neighborhoods(dcDistrictId),
        queryFn: () => getMapNeighborhoods(dcDistrictId),
        enabled: role === 'DISTRICT_COORDINATOR' && !!dcDistrictId,
    });

    const dcDistrict = allDistricts.find(d => d.id === dcDistrictId);
    const ncNeighborhood = ncNeighborhoods.find(n => n.id === ncNeighborhoodId);
    const ncDistrictObj = allDistricts.find(d => d.id === ncDistrictId);
    const adminSelectedDistrict = allDistricts.find(d => d.id === adminDistrictId);
    const adminSelectedNeighborhoodDistrict = allDistricts.find(d => d.id === adminNeighborhoodDistrictId);
    const adminSelectedNeighborhood = adminNeighborhoods.find(n => n.id === adminNeighborhoodId);

    // ── DISTRICT COORDINATOR VIEW ─────────────────────────────────────────────
    if (role === 'DISTRICT_COORDINATOR') {
        const dcSelectedNeighborhood = dcNeighborhoods.find(n => n.id === dcNeighborhoodId);

        return (
            <div className="max-w-2xl mx-auto">
                <div className="mb-6">
                    <div className="flex items-center gap-3 mb-1">
                        <Building2 className="h-6 w-6 text-blue-600" />
                        <h1 className="text-2xl font-bold text-gray-900">Koordinatörlük Merkezleri</h1>
                    </div>
                    <p className="text-sm text-gray-500">
                        {dcDistrict?.name ? `${dcDistrict.name} İlçesi` : 'İlçeniz'} — İlçe ve mahalle operasyon merkezi konumları
                    </p>
                </div>

                {loadingDistricts ? (
                    <div className="flex justify-center py-12"><LoadingSpinner size="lg" /></div>
                ) : !dcDistrictId ? (
                    <div className="text-center py-12 text-gray-500">
                        Hesabınıza henüz bir ilçe atanmamış. Lütfen yöneticinizle iletişime geçin.
                    </div>
                ) : (
                    <>
                        {/* Tabs */}
                        <div className="flex gap-1 bg-gray-100 rounded-lg p-1 mb-6 w-fit">
                            <button
                                onClick={() => setDcTab('district')}
                                className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                                    dcTab === 'district'
                                        ? 'bg-white text-gray-900 shadow-sm'
                                        : 'text-gray-600 hover:text-gray-900'
                                }`}
                            >
                                İlçe Merkezim
                            </button>
                            <button
                                onClick={() => setDcTab('neighborhood')}
                                className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                                    dcTab === 'neighborhood'
                                        ? 'bg-white text-gray-900 shadow-sm'
                                        : 'text-gray-600 hover:text-gray-900'
                                }`}
                            >
                                Mahalle Merkezleri
                            </button>
                        </div>

                        {dcTab === 'district' && (
                            <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                                <DistrictSection districtId={dcDistrictId} district={dcDistrict} />
                            </div>
                        )}

                        {dcTab === 'neighborhood' && (
                            <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                                <div className="mb-4">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Mahalle Seç</label>
                                    <select
                                        value={dcNeighborhoodId}
                                        onChange={e => setDcNeighborhoodId(e.target.value)}
                                        disabled={loadingDcNeighborhoods}
                                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
                                    >
                                        <option value="">
                                            {loadingDcNeighborhoods ? 'Yükleniyor...' : '-- Mahalle seçiniz --'}
                                        </option>
                                        {[...dcNeighborhoods]
                                            .sort((a, b) => a.name.localeCompare(b.name, 'tr'))
                                            .map(n => (
                                                <option key={n.id} value={n.id}>{n.name}</option>
                                            ))}
                                    </select>
                                </div>

                                {dcNeighborhoodId ? (
                                    <NeighborhoodSection
                                        key={dcNeighborhoodId}
                                        neighborhoodId={dcNeighborhoodId}
                                        neighborhood={dcSelectedNeighborhood}
                                        districtName={dcDistrict?.name}
                                    />
                                ) : (
                                    <p className="text-sm text-gray-400 text-center py-8">
                                        Merkezi yönetmek istediğiniz mahalleyi seçin.
                                    </p>
                                )}
                            </div>
                        )}
                    </>
                )}
            </div>
        );
    }

    // ── NEIGHBORHOOD COORDINATOR VIEW ─────────────────────────────────────────
    if (role === 'NEIGHBORHOOD_COORDINATOR') {
        return (
            <div className="max-w-2xl mx-auto">
                <div className="mb-6">
                    <div className="flex items-center gap-3 mb-1">
                        <Building2 className="h-6 w-6 text-violet-600" />
                        <h1 className="text-2xl font-bold text-gray-900">Koordinatörlük Merkezim</h1>
                    </div>
                    <p className="text-sm text-gray-500">
                        {ncNeighborhood?.name ? `${ncNeighborhood.name} Mahallesi` : 'Mahalleniz'}{' '}
                        {ncDistrictObj?.name ? `(${ncDistrictObj.name})` : ''} — Operasyon merkezi konumu
                    </p>
                </div>

                {!ncNeighborhoodId ? (
                    <div className="text-center py-12 text-gray-500">
                        <p className="font-medium">Hesabınıza henüz bir mahalle atanmamış.</p>
                        <p className="text-sm mt-1">Lütfen yöneticinizle iletişime geçin.</p>
                    </div>
                ) : (
                    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                        <NeighborhoodSection
                            neighborhoodId={ncNeighborhoodId}
                            neighborhood={ncNeighborhood}
                            districtName={ncDistrictObj?.name}
                        />
                    </div>
                )}
            </div>
        );
    }

    // ── ADMIN VIEW ────────────────────────────────────────────────────────────
    return (
        <div className="max-w-3xl mx-auto">
            <div className="mb-6">
                <div className="flex items-center gap-3 mb-1">
                    <Building2 className="h-6 w-6 text-blue-600" />
                    <h1 className="text-2xl font-bold text-gray-900">Koordinatörlük Merkezleri</h1>
                </div>
                <p className="text-sm text-gray-500">İlçe ve mahalle operasyon merkezi konumlarını yönetin.</p>
            </div>

            {/* Tabs */}
            <div className="flex gap-1 bg-gray-100 rounded-lg p-1 mb-6 w-fit">
                <button
                    onClick={() => setActiveTab('district')}
                    className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                        activeTab === 'district'
                            ? 'bg-white text-gray-900 shadow-sm'
                            : 'text-gray-600 hover:text-gray-900'
                    }`}
                >
                    İlçe Merkezleri
                </button>
                <button
                    onClick={() => setActiveTab('neighborhood')}
                    className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                        activeTab === 'neighborhood'
                            ? 'bg-white text-gray-900 shadow-sm'
                            : 'text-gray-600 hover:text-gray-900'
                    }`}
                >
                    Mahalle Merkezleri
                </button>
            </div>

            {/* District tab */}
            {activeTab === 'district' && (
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                    <div className="mb-4">
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            İlçe Seç
                        </label>
                        <select
                            value={adminDistrictId}
                            onChange={e => setAdminDistrictId(e.target.value)}
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                            <option value="">-- İlçe seçiniz --</option>
                            {[...allDistricts]
                                .sort((a, b) => a.name.localeCompare(b.name, 'tr'))
                                .map(d => (
                                    <option key={d.id} value={d.id}>
                                        {d.name}
                                    </option>
                                ))}
                        </select>
                    </div>

                    {adminDistrictId && (
                        <DistrictSection
                            key={adminDistrictId}
                            districtId={adminDistrictId}
                            district={adminSelectedDistrict}
                        />
                    )}
                    {!adminDistrictId && (
                        <p className="text-sm text-gray-400 text-center py-8">
                            Yönetmek istediğiniz ilçeyi seçin.
                        </p>
                    )}
                </div>
            )}

            {/* Neighborhood tab */}
            {activeTab === 'neighborhood' && (
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">İlçe Seç</label>
                            <select
                                value={adminNeighborhoodDistrictId}
                                onChange={e => {
                                    setAdminNeighborhoodDistrictId(e.target.value);
                                    setAdminNeighborhoodId('');
                                }}
                                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                            >
                                <option value="">-- İlçe seçiniz --</option>
                                {[...allDistricts]
                                    .sort((a, b) => a.name.localeCompare(b.name, 'tr'))
                                    .map(d => (
                                        <option key={d.id} value={d.id}>
                                            {d.name}
                                        </option>
                                    ))}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Mahalle Seç</label>
                            <select
                                value={adminNeighborhoodId}
                                onChange={e => setAdminNeighborhoodId(e.target.value)}
                                disabled={!adminNeighborhoodDistrictId || loadingAdminNeighborhoods}
                                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
                            >
                                <option value="">
                                    {loadingAdminNeighborhoods
                                        ? 'Yükleniyor...'
                                        : adminNeighborhoodDistrictId
                                        ? '-- Mahalle seçiniz --'
                                        : '-- Önce ilçe seçin --'}
                                </option>
                                {[...adminNeighborhoods]
                                    .sort((a, b) => a.name.localeCompare(b.name, 'tr'))
                                    .map(n => (
                                        <option key={n.id} value={n.id}>
                                            {n.name}
                                        </option>
                                    ))}
                            </select>
                        </div>
                    </div>

                    {adminNeighborhoodId ? (
                        <NeighborhoodSection
                            key={adminNeighborhoodId}
                            neighborhoodId={adminNeighborhoodId}
                            neighborhood={adminSelectedNeighborhood}
                            districtName={adminSelectedNeighborhoodDistrict?.name}
                        />
                    ) : (
                        <p className="text-sm text-gray-400 text-center py-8">
                            Yönetmek istediğiniz ilçe ve mahalleyi seçin.
                        </p>
                    )}
                </div>
            )}
        </div>
    );
};
