import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    listNeighborhoodCoordinators,
    assignNeighborhoodCoordinator,
    listDistrictCoordinators,
    assignDistrictCoordinator,
    listEligibleUsers,
    updateDistrictCoordinatorLocation,
    updateNeighborhoodCoordinatorLocation,
    CoordinatorNeighborhoodResponse,
    CoordinatorDistrictResponse,
} from '@/api/coordinator.api';
import { getDistricts } from '@/api/districts.api';
import { Button, Modal, FormField, LoadingSpinner, Badge } from '@/components/ui';
import { useToast } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';
import { MapPin, UserCheck, Users, Building2, Navigation } from 'lucide-react';
import { DistrictResponse } from '@/types';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix leaflet icon
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

type AssignModalState = {
    neighborhoodId: string;
    neighborhoodName: string;
    districtId: string;
} | null;

type DCAssignModalState = {
    districtId: string;
    districtName: string;
} | null;

type LocationModalState = {
    type: 'district' | 'neighborhood';
    id: string;
    name: string;
    currentLat?: number;
    currentLng?: number;
} | null;

const ROLE_TR: Record<string, string> = {
    DISTRICT_COORDINATOR: 'İlçe Koordinatörü',
    NEIGHBORHOOD_COORDINATOR: 'Mahalle Koordinatörü',
    VOLUNTEER: 'Gönüllü',
    ADMIN: 'Yönetici',
};

// ─── Map click handler for location picker ────────────────────────────────────
const MapClickHandler: React.FC<{
    onLocationSelected: (lat: number, lng: number) => void;
}> = ({ onLocationSelected }) => {
    useMapEvents({
        click(e) {
            onLocationSelected(e.latlng.lat, e.latlng.lng);
        },
    });
    return null;
};

// ─── Location picker modal ────────────────────────────────────────────────────
const LocationPickerModal: React.FC<{
    isOpen: boolean;
    onClose: () => void;
    title: string;
    currentLat?: number;
    currentLng?: number;
    onSave: (lat: number, lng: number) => void;
    isLoading: boolean;
}> = ({ isOpen, onClose, title, currentLat, currentLng, onSave, isLoading }) => {
    const [pickedLat, setPickedLat] = useState<number | null>(currentLat ?? null);
    const [pickedLng, setPickedLng] = useState<number | null>(currentLng ?? null);

    const handleLocationSelected = (lat: number, lng: number) => {
        setPickedLat(lat);
        setPickedLng(lng);
    };

    const center: [number, number] =
        pickedLat !== null && pickedLng !== null
            ? [pickedLat, pickedLng]
            : [41.0082, 28.9784];

    if (!isOpen) return null;

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={title}>
            <div className="space-y-3">
                <p className="text-sm text-gray-500">
                    Koordinatörün saha konumunu haritadan seçin. Konum, belirlenen bölge sınırları içinde olmalıdır.
                </p>
                <div style={{ height: 360, borderRadius: 8, overflow: 'hidden', border: '1px solid #e5e7eb', cursor: 'crosshair' }}>
                    <MapContainer
                        center={center}
                        zoom={pickedLat !== null ? 14 : 10}
                        className="w-full h-full"
                    >
                        <TileLayer
                            attribution="&copy; OpenStreetMap contributors"
                            url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
                        />
                        <MapClickHandler onLocationSelected={handleLocationSelected} />
                        {pickedLat !== null && pickedLng !== null && (
                            <Marker position={[pickedLat, pickedLng]} />
                        )}
                    </MapContainer>
                </div>
                {pickedLat !== null && pickedLng !== null ? (
                    <p className="text-xs text-gray-500">
                        Seçilen konum: {pickedLat.toFixed(5)}, {pickedLng.toFixed(5)}
                    </p>
                ) : (
                    <p className="text-xs text-amber-600">Haritaya tıklayarak konum seçin.</p>
                )}
                <div className="flex justify-end gap-3 pt-1">
                    <Button variant="ghost" onClick={onClose} disabled={isLoading}>İptal</Button>
                    <Button
                        loading={isLoading}
                        disabled={pickedLat === null || pickedLng === null}
                        onClick={() => pickedLat !== null && pickedLng !== null && onSave(pickedLat, pickedLng)}
                    >
                        Konumu Kaydet
                    </Button>
                </div>
            </div>
        </Modal>
    );
};

// ─── Main page ────────────────────────────────────────────────────────────────
export const CoordinatorAssignmentPage: React.FC = () => {
    const user = useAuthStore((s) => s.user);
    const isAdmin = user?.role === 'ADMIN';
    const isDistrictCoord = user?.role === 'DISTRICT_COORDINATOR';
    const toast = useToast();
    const queryClient = useQueryClient();

    // For DISTRICT_COORDINATOR the coordinating district differs from the residence district
    // stored in the token. Let the backend resolve it via findByCoordinatorId.
    const [selectedDistrictId, setSelectedDistrictId] = useState<string>('');
    const [assignModal, setAssignModal]         = useState<AssignModalState>(null);
    const [dcAssignModal, setDCAssignModal]     = useState<DCAssignModalState>(null);
    const [locationModal, setLocationModal]     = useState<LocationModalState>(null);

    const { data: districts = [] } = useQuery<DistrictResponse[]>({
        queryKey: ['districts'],
        queryFn:  getDistricts,
        enabled:  isAdmin,
    });

    const { data: districtCoords = [], isLoading: dcLoading } = useQuery<CoordinatorDistrictResponse[]>({
        queryKey: ['coordinators', 'districts'],
        queryFn:  listDistrictCoordinators,
        enabled:  isAdmin && !!selectedDistrictId,
    });

    const { data: neighborhoodCoords = [], isLoading: nbLoading } = useQuery<CoordinatorNeighborhoodResponse[]>({
        // DISTRICT_COORDINATOR: backend resolves their coordinating district via findByCoordinatorId.
        // Pass undefined so backend uses the correct FK instead of residence district.
        queryKey: ['coordinators', 'neighborhoods', isDistrictCoord ? 'mine' : selectedDistrictId],
        queryFn:  () => listNeighborhoodCoordinators(isDistrictCoord ? undefined : selectedDistrictId || undefined),
        enabled:  isDistrictCoord || !!selectedDistrictId,
    });

    // For DISTRICT_COORDINATOR derive district name from the neighborhood results,
    // since the coordinating district is not stored in the auth token.
    const displayDistrictName = isDistrictCoord
        ? (neighborhoodCoords[0]?.districtName ?? (nbLoading ? 'Yükleniyor...' : '—'))
        : (districts.find((d) => d.id === selectedDistrictId)?.name ?? '');

    const showContent = isDistrictCoord || !!selectedDistrictId;

    const selectedDistrictCoord = districtCoords.find((d) => d.districtId === selectedDistrictId) ?? null;

    // ── Assign mutations ──
    const districtMutation = useMutation({
        mutationFn: ({ districtId, userId }: { districtId: string; userId: string }) =>
            assignDistrictCoordinator(districtId, userId),
        onSuccess: () => {
            toast.success('İlçe koordinatörü başarıyla atandı');
            queryClient.invalidateQueries({ queryKey: ['coordinators', 'districts'] });
            setDCAssignModal(null);
        },
        onError: (err: any) =>
            toast.error(err.response?.data?.message || err.message || 'Atama başarısız'),
    });

    const neighborhoodMutation = useMutation({
        mutationFn: ({ neighborhoodId, userId }: { neighborhoodId: string; userId: string }) =>
            assignNeighborhoodCoordinator(neighborhoodId, userId),
        onSuccess: () => {
            toast.success('Mahalle koordinatörü başarıyla atandı');
            queryClient.invalidateQueries({ queryKey: ['coordinators', 'neighborhoods', selectedDistrictId] });
            setAssignModal(null);
        },
        onError: (err: any) =>
            toast.error(err.response?.data?.message || err.message || 'Atama başarısız'),
    });

    // ── Location update mutations ──
    const districtLocationMutation = useMutation({
        mutationFn: ({ id, lat, lng }: { id: string; lat: number; lng: number }) =>
            updateDistrictCoordinatorLocation(id, lat, lng),
        onSuccess: () => {
            toast.success('İlçe koordinatörü konumu güncellendi');
            queryClient.invalidateQueries({ queryKey: ['coordinators', 'districts'] });
            queryClient.invalidateQueries({ queryKey: ['map', 'district-coordinators'] });
            setLocationModal(null);
        },
        onError: (err: any) =>
            toast.error(err.response?.data?.message || err.message || 'Konum güncellenemedi'),
    });

    const neighborhoodLocationMutation = useMutation({
        mutationFn: ({ id, lat, lng }: { id: string; lat: number; lng: number }) =>
            updateNeighborhoodCoordinatorLocation(id, lat, lng),
        onSuccess: () => {
            toast.success('Mahalle koordinatörü konumu güncellendi');
            queryClient.invalidateQueries({ queryKey: ['coordinators', 'neighborhoods', selectedDistrictId] });
            queryClient.invalidateQueries({ queryKey: ['map', 'neighborhood-coordinators'] });
            setLocationModal(null);
        },
        onError: (err: any) =>
            toast.error(err.response?.data?.message || err.message || 'Konum güncellenemedi'),
    });

    const handleSaveLocation = (lat: number, lng: number) => {
        if (!locationModal) return;
        if (locationModal.type === 'district') {
            districtLocationMutation.mutate({ id: locationModal.id, lat, lng });
        } else {
            neighborhoodLocationMutation.mutate({ id: locationModal.id, lat, lng });
        }
    };

    const isLocationSaving =
        districtLocationMutation.isPending || neighborhoodLocationMutation.isPending;

    const inputCls =
        'block w-full rounded-md border-0 py-1.5 pl-3 pr-10 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm sm:leading-6';

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:tracking-tight">
                    Koordinatör Atamaları
                </h1>
                <p className="mt-1 text-sm text-gray-500">
                    {isAdmin
                        ? 'İl ve ilçe seçerek ilçe ve mahalle koordinatörlerini atayın. Saha konumlarını haritadan güncelleyin.'
                        : 'Kendi ilçenizdeki mahalle koordinatörlerini atayın.'}
                </p>
            </div>

            {/* District selector */}
            <div className="bg-white shadow sm:rounded-lg border border-gray-200 p-5">
                <div className="flex items-center gap-2 mb-4">
                    <MapPin className="h-4 w-4 text-blue-600" />
                    <span className="text-sm font-semibold text-gray-700">Konum Seçimi</span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <FormField label="İl">
                        <input
                            type="text"
                            disabled
                            value="İstanbul"
                            className="block w-full rounded-md border-0 py-1.5 text-gray-500 bg-gray-50 ring-1 ring-inset ring-gray-300 sm:text-sm sm:leading-6"
                        />
                    </FormField>
                    <FormField label="İlçe Seçin">
                        {isAdmin ? (
                            <select
                                value={selectedDistrictId}
                                onChange={(e) => setSelectedDistrictId(e.target.value)}
                                className={inputCls}
                            >
                                <option value="">— İlçe Seçin —</option>
                                {districts.map((d) => (
                                    <option key={d.id} value={d.id}>{d.name}</option>
                                ))}
                            </select>
                        ) : (
                            <input
                                type="text"
                                disabled
                                value={displayDistrictName}
                                className="block w-full rounded-md border-0 py-1.5 text-gray-500 bg-gray-50 ring-1 ring-inset ring-gray-300 sm:text-sm sm:leading-6"
                            />
                        )}
                    </FormField>
                </div>
            </div>

            {!showContent && (
                <EmptyHint message="Koordinatör atamak için lütfen yukarıdan bir ilçe seçin." />
            )}

            {showContent && (
                <>
                    <div className="flex items-center gap-2 px-1">
                        <span className="text-sm text-gray-500">Seçilen İlçe:</span>
                        <span className="text-sm font-semibold text-gray-800">{displayDistrictName}</span>
                    </div>

                    {/* District coordinator card — admin only */}
                    {isAdmin && (
                        <div className="bg-white shadow sm:rounded-lg border border-gray-200 p-5">
                            <div className="flex items-center gap-2 mb-4">
                                <Building2 className="h-4 w-4 text-indigo-600" />
                                <span className="text-sm font-semibold text-gray-700">İlçe Koordinatörü Ataması</span>
                                <span className="text-sm text-gray-400">— {displayDistrictName}</span>
                            </div>
                            {dcLoading ? (
                                <div className="flex justify-center py-4"><LoadingSpinner /></div>
                            ) : (
                                <div className="flex items-center justify-between flex-wrap gap-3">
                                    <div>
                                        {selectedDistrictCoord?.coordinatorId ? (
                                            <div>
                                                <p className="text-sm font-medium text-gray-900">
                                                    {selectedDistrictCoord.coordinatorFirstName}{' '}
                                                    {selectedDistrictCoord.coordinatorLastName}
                                                </p>
                                                <p className="text-xs text-gray-500 mt-0.5">
                                                    {selectedDistrictCoord.coordinatorEmail}
                                                </p>
                                                <Badge variant="success" className="mt-2">Atanmış</Badge>
                                            </div>
                                        ) : (
                                            <div>
                                                <p className="text-sm text-gray-400 italic">Henüz koordinatör atanmamış</p>
                                                <Badge variant="warning" className="mt-2">Boş</Badge>
                                            </div>
                                        )}
                                    </div>
                                    <div className="flex gap-2">
                                        {selectedDistrictCoord?.coordinatorId && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                leftIcon={<Navigation className="h-4 w-4" />}
                                                onClick={() =>
                                                    setLocationModal({
                                                        type: 'district',
                                                        id:   selectedDistrictId,
                                                        name: displayDistrictName,
                                                    })
                                                }
                                            >
                                                Konumu Güncelle
                                            </Button>
                                        )}
                                        <Button
                                            variant="secondary"
                                            size="sm"
                                            leftIcon={<UserCheck className="h-4 w-4" />}
                                            onClick={() =>
                                                setDCAssignModal({
                                                    districtId:   selectedDistrictId,
                                                    districtName: displayDistrictName,
                                                })
                                            }
                                        >
                                            {selectedDistrictCoord?.coordinatorId ? 'Değiştir' : 'Ata'}
                                        </Button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Neighborhood coordinators table */}
                    <div className="bg-white shadow sm:rounded-lg border border-gray-200 overflow-hidden">
                        <div className="px-5 py-4 border-b border-gray-200 flex items-center gap-2">
                            <Users className="h-4 w-4 text-blue-600" />
                            <span className="text-sm font-semibold text-gray-700">Mahalle Koordinatörleri</span>
                            <span className="text-sm text-gray-400">— {displayDistrictName}</span>
                        </div>
                        {nbLoading ? (
                            <div className="p-8 flex justify-center"><LoadingSpinner /></div>
                        ) : neighborhoodCoords.length === 0 ? (
                            <EmptyHint message="Bu ilçede mahalle bulunamadı." />
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="min-w-full divide-y divide-gray-200">
                                    <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Mahalle</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Mevcut Koordinatör</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Durum</th>
                                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">İşlemler</th>
                                        </tr>
                                    </thead>
                                    <tbody className="bg-white divide-y divide-gray-200">
                                        {neighborhoodCoords.map((n) => (
                                            <tr key={n.neighborhoodId} className="hover:bg-gray-50">
                                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                                    {n.neighborhoodName}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap">
                                                    {n.coordinatorId ? (
                                                        <div>
                                                            <p className="text-sm font-medium text-gray-900">
                                                                {n.coordinatorFirstName} {n.coordinatorLastName}
                                                            </p>
                                                            <p className="text-xs text-gray-500">{n.coordinatorEmail}</p>
                                                        </div>
                                                    ) : (
                                                        <span className="text-sm text-gray-400 italic">Atanmamış</span>
                                                    )}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap">
                                                    <Badge variant={n.coordinatorId ? 'success' : 'warning'}>
                                                        {n.coordinatorId ? 'Atanmış' : 'Boş'}
                                                    </Badge>
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-right">
                                                    <div className="flex justify-end gap-2">
                                                        {n.coordinatorId && (
                                                            <Button
                                                                variant="ghost"
                                                                size="sm"
                                                                leftIcon={<Navigation className="h-3.5 w-3.5" />}
                                                                onClick={() =>
                                                                    setLocationModal({
                                                                        type: 'neighborhood',
                                                                        id:   n.neighborhoodId,
                                                                        name: n.neighborhoodName,
                                                                    })
                                                                }
                                                            >
                                                                Konum
                                                            </Button>
                                                        )}
                                                        <Button
                                                            variant="secondary"
                                                            size="sm"
                                                            leftIcon={<UserCheck className="h-4 w-4" />}
                                                            onClick={() =>
                                                                setAssignModal({
                                                                    neighborhoodId:   n.neighborhoodId,
                                                                    neighborhoodName: n.neighborhoodName,
                                                                    districtId:       n.districtId ?? selectedDistrictId,
                                                                })
                                                            }
                                                        >
                                                            {n.coordinatorId ? 'Değiştir' : 'Ata'}
                                                        </Button>
                                                    </div>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </>
            )}

            {/* Neighborhood coordinator assign modal */}
            {assignModal && (
                <UserSelectModal
                    title={`${assignModal.neighborhoodName} — Mahalle Koordinatörü Ata`}
                    isOpen
                    onClose={() => setAssignModal(null)}
                    onSelect={(userId) =>
                        neighborhoodMutation.mutate({ neighborhoodId: assignModal.neighborhoodId, userId })
                    }
                    isLoading={neighborhoodMutation.isPending}
                    districtId={assignModal.districtId}
                    assignmentType="NEIGHBORHOOD_COORDINATOR"
                />
            )}

            {/* District coordinator assign modal */}
            {dcAssignModal && (
                <UserSelectModal
                    title={`${dcAssignModal.districtName} — İlçe Koordinatörü Ata`}
                    isOpen
                    onClose={() => setDCAssignModal(null)}
                    onSelect={(userId) =>
                        districtMutation.mutate({ districtId: dcAssignModal.districtId, userId })
                    }
                    isLoading={districtMutation.isPending}
                    assignmentType="DISTRICT_COORDINATOR"
                />
            )}

            {/* Location picker modal */}
            {locationModal && (
                <LocationPickerModal
                    isOpen
                    onClose={() => setLocationModal(null)}
                    title={`${locationModal.name} — Koordinatör Konumu`}
                    currentLat={locationModal.currentLat}
                    currentLng={locationModal.currentLng}
                    onSave={handleSaveLocation}
                    isLoading={isLocationSaving}
                />
            )}
        </div>
    );
};

/* ─── Alt bileşenler ─── */

const EmptyHint: React.FC<{ message: string }> = ({ message }) => (
    <div className="bg-gray-50 rounded-lg border border-dashed border-gray-300 p-8 text-center text-gray-500 text-sm">
        {message}
    </div>
);

const UserSelectModal: React.FC<{
    title: string;
    isOpen: boolean;
    onClose: () => void;
    onSelect: (userId: string) => void;
    isLoading: boolean;
    districtId?: string;
    assignmentType?: string;
}> = ({ title, isOpen, onClose, onSelect, isLoading, districtId, assignmentType = 'NEIGHBORHOOD_COORDINATOR' }) => {
    const [search, setSearch]             = useState('');
    const [selectedUserId, setSelectedUserId] = useState('');

    const { data: users = [], isLoading: usersLoading } = useQuery({
        queryKey: ['coordinators', 'eligible-users', assignmentType, districtId],
        queryFn:  () => listEligibleUsers(assignmentType, districtId),
        enabled:  isOpen,
    });

    const filtered = users.filter((u: any) => {
        const q = search.toLowerCase();
        return (
            u.firstName.toLowerCase().includes(q) ||
            u.lastName.toLowerCase().includes(q) ||
            u.email.toLowerCase().includes(q)
        );
    });

    const isCoordinator = (role: string) =>
        role === 'DISTRICT_COORDINATOR' || role === 'NEIGHBORHOOD_COORDINATOR';

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={title}>
            <div className="space-y-4">
                <FormField label="Kullanıcı Ara">
                    <input
                        type="text"
                        placeholder="Ad, soyad veya e-posta..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                    />
                </FormField>
                <div className="max-h-72 overflow-y-auto border rounded-md divide-y">
                    {usersLoading ? (
                        <div className="p-4 flex justify-center"><LoadingSpinner /></div>
                    ) : filtered.length === 0 ? (
                        <div className="p-4 text-center text-gray-500 text-sm">
                            {users.length === 0 ? 'Uygun kullanıcı bulunamadı' : 'Arama sonucu bulunamadı'}
                        </div>
                    ) : (
                        filtered.map((u: any) => (
                            <div
                                key={u.id}
                                onClick={() => setSelectedUserId(u.id)}
                                className={`p-3 cursor-pointer hover:bg-blue-50 transition-colors ${
                                    selectedUserId === u.id ? 'bg-blue-50 border-l-4 border-l-blue-600' : ''
                                }`}
                            >
                                <div className="font-medium text-gray-900">
                                    {u.firstName} {u.lastName}
                                    {isCoordinator(u.role) && (
                                        <span className="ml-2 text-xs font-normal text-orange-600">(Zaten koordinatör)</span>
                                    )}
                                </div>
                                <div className="text-xs text-gray-500 flex items-center gap-2 mt-0.5">
                                    <span>{u.email}</span>
                                    <span className="text-gray-400">—</span>
                                    <span className={isCoordinator(u.role) ? 'text-orange-500 font-medium' : 'text-gray-400'}>
                                        {ROLE_TR[u.role] ?? u.role}
                                    </span>
                                    {u.district?.name && (
                                        <span className="text-gray-400">({u.district.name})</span>
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                </div>
                <div className="flex justify-end space-x-3 pt-2">
                    <Button variant="ghost" onClick={onClose} disabled={isLoading}>İptal</Button>
                    <Button
                        loading={isLoading}
                        disabled={!selectedUserId}
                        onClick={() => selectedUserId && onSelect(selectedUserId)}
                    >
                        Ata
                    </Button>
                </div>
            </div>
        </Modal>
    );
};
