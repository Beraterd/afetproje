import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { queryKeys } from '@/utils/queryKeys';
import { getMapDistricts, getMapNeighborhoods } from '@/api/map.api';
import { LoadingSpinner } from '@/components/ui';
import { Building2 } from 'lucide-react';
import { DistrictSection } from '@/components/coordination/DistrictSection';
import { NeighborhoodSection } from '@/components/coordination/NeighborhoodSection';

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
