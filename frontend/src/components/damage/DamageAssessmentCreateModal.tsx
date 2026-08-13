import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { MapPin, Image, X } from 'lucide-react';
import type {
    CreateDamageAssessmentRequest,
    DistrictResponse,
    NeighborhoodSummaryResponse,
} from '@/types';
import { DAMAGE_LEVELS } from '@/types';
import { LocationPickerMap } from '@/components/map/LocationPickerMap';
import { BuildingSelector, SelectedBuildingSummary } from '@/components/damage/BuildingSelector';
import { getBuilding3dConfig } from '@/api/buildings.api';
import { queryKeys } from '@/utils/queryKeys';
import { isWebglSupported } from '@/utils/webgl';
import { sortByNameTr } from '@/utils/turkishSort';

export type DamageAssessmentFormState = Partial<CreateDamageAssessmentRequest>;

const URGENCY_FIELDS = [
    { key: 'collapseRisk', label: 'Çökme Riski' },
    { key: 'emergencyEvacuationNeeded', label: 'Acil Tahliye Gerekli' },
    { key: 'casualtiesSuspected', label: 'Kayıp/Yaralı Şüphesi' },
    { key: 'blockedRoad', label: 'Yol Kapandı' },
    { key: 'gasLeakRisk', label: 'Gaz Kaçağı Riski' },
] as const;

interface DamageAssessmentCreateModalProps {
    showDistrictSelect: boolean;
    districts: DistrictResponse[];
    neighborhoods: NeighborhoodSummaryResponse[];
    selectedDistrictId: string;
    onDistrictChange: (districtId: string) => void;
    form: DamageAssessmentFormState;
    setForm: React.Dispatch<React.SetStateAction<DamageAssessmentFormState>>;
    onNeighborhoodChange: (neighborhoodId: string) => void;
    onPositionChange: (lat: number, lng: number) => void;
    locationConfirmed: boolean;
    onConfirmLocation: () => void;
    photos: File[];
    photoErrors: string[];
    photoInputRef: React.RefObject<HTMLInputElement>;
    onPhotoSelect: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onRemovePhoto: (index: number) => void;
    creating: boolean;
    onCancel: () => void;
    onSubmit: () => void;
}

/** Yeni hasar tespiti oluşturma modalı — form alanları + mini harita ile konum doğrulama.
 *  Form state'i (`form`/`setForm`) sahibi hâlâ page'dir (prop olarak geçirilir); bu component
 *  yalnızca sunum ve alan bazlı değişiklik bildirimlerinden sorumludur. */
export const DamageAssessmentCreateModal: React.FC<DamageAssessmentCreateModalProps> = ({
    showDistrictSelect, districts, neighborhoods, selectedDistrictId, onDistrictChange,
    form, setForm, onNeighborhoodChange, onPositionChange, locationConfirmed, onConfirmLocation,
    photos, photoErrors, photoInputRef, onPhotoSelect, onRemovePhoto, creating, onCancel, onSubmit,
}) => {
    // ── 3B bina seçici uygunluğu: yalnızca pilot ilçe (Pendik) + WebGL destekleniyorsa ──
    const building3dConfigQuery = useQuery({ queryKey: queryKeys.buildings.config(), queryFn: getBuilding3dConfig });
    const webglOk = useMemo(() => isWebglSupported(), []);
    const selectedDistrictName = districts.find((d) => d.id === selectedDistrictId)?.name;
    const districtIs3dEligible = !!selectedDistrictName
        && !!building3dConfigQuery.data?.enabledDistricts.includes(selectedDistrictName);
    const show3dSelector = districtIs3dEligible && webglOk;
    const show3dFallbackNotice = districtIs3dEligible && !webglOk;

    const buildingSelectorValue: SelectedBuildingSummary | null = form.buildingId
        ? {
            id: form.buildingId,
            displayAddress: form.address ?? null,
            streetName: form.streetName ?? null,
            buildingNumber: form.buildingNo ?? null,
            neighborhoodId: form.neighborhoodId ?? '',
            neighborhoodName: neighborhoods.find((n) => n.id === form.neighborhoodId)?.name ?? '',
            latitude: form.latitude ?? 0,
            longitude: form.longitude ?? 0,
        }
        : null;

    const handleBuildingChange = (value: SelectedBuildingSummary | null) => {
        if (value) {
            setForm((f) => ({
                ...f,
                buildingId: value.id,
                neighborhoodId: value.neighborhoodId,
                streetName: value.streetName ?? undefined,
                buildingNo: value.buildingNumber ?? undefined,
                address: value.displayAddress ?? '',
                latitude: value.latitude,
                longitude: value.longitude,
            }));
            onConfirmLocation();
        } else {
            setForm((f) => ({ ...f, buildingId: undefined }));
        }
    };

    return (
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
                        {showDistrictSelect && (
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">İlçe *</label>
                                <select
                                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                    value={selectedDistrictId}
                                    onChange={(e) => onDistrictChange(e.target.value)}
                                >
                                    <option value="">Seçin</option>
                                    {sortByNameTr(districts).map((d) => (
                                        <option key={d.id} value={d.id}>{d.name}</option>
                                    ))}
                                </select>
                            </div>
                        )}

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Mahalle *</label>
                            {show3dSelector ? (
                                <div className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-gray-50 text-gray-600">
                                    {form.neighborhoodId
                                        ? neighborhoods.find((n) => n.id === form.neighborhoodId)?.name
                                        : 'Haritadan bina seçtiğinizde otomatik doldurulur'}
                                </div>
                            ) : (
                                <select
                                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                    value={form.neighborhoodId || ''}
                                    onChange={(e) => onNeighborhoodChange(e.target.value)}
                                >
                                    <option value="">Seçin</option>
                                    {sortByNameTr(neighborhoods).map((n) => (
                                        <option key={n.id} value={n.id}>{n.name}</option>
                                    ))}
                                </select>
                            )}
                        </div>

                        <div className="grid grid-cols-3 gap-3">
                            <div className="col-span-2">
                                <label className="block text-sm font-medium text-gray-700 mb-1">Sokak / Cadde</label>
                                <input
                                    type="text"
                                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                    value={form.streetName || ''}
                                    onChange={(e) => setForm((f) => ({ ...f, streetName: e.target.value }))}
                                    placeholder="Atatürk Cad."
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Bina No</label>
                                <input
                                    type="text"
                                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                    value={form.buildingNo || ''}
                                    onChange={(e) => setForm((f) => ({ ...f, buildingNo: e.target.value }))}
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
                                onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))}
                                placeholder="Tam adres bilgisi"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Hasar Düzeyi</label>
                            <select
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                                value={form.damageLevel || 'UNASSESSED'}
                                onChange={(e) => setForm((f) => ({ ...f, damageLevel: e.target.value }))}
                            >
                                {DAMAGE_LEVELS.map((d) => (
                                    <option key={d.value} value={d.value}>{d.label}</option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Acil Durumlar</label>
                            <div className="grid grid-cols-1 gap-1.5">
                                {URGENCY_FIELDS.map((f) => (
                                    <label key={f.key} className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                                        <input
                                            type="checkbox"
                                            checked={!!(form as any)[f.key]}
                                            onChange={(e) => setForm((prev) => ({ ...prev, [f.key]: e.target.checked }))}
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
                                onChange={(e) => setForm((f) => ({ ...f, note: e.target.value }))}
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
                                onChange={onPhotoSelect}
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
                                                onClick={() => onRemovePhoto(i)}
                                                aria-label="Fotoğrafı kaldır"
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

                    {/* ── Sağ: Bina Konumu ── */}
                    <div>
                        <p className="block text-sm font-medium text-gray-700 mb-2">Bina Konumu *</p>

                        {show3dFallbackNotice && (
                            <div className="mb-2 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 text-xs text-amber-800">
                                3B bina görünümü bu cihazda kullanılamıyor. Konumu haritadan seçerek devam edebilirsiniz.
                            </div>
                        )}

                        {show3dSelector && selectedDistrictId ? (
                            <BuildingSelector
                                districtId={selectedDistrictId}
                                value={buildingSelectorValue}
                                onChange={handleBuildingChange}
                            />
                        ) : form.neighborhoodId && selectedDistrictId ? (
                            <LocationPickerMap
                                districtId={selectedDistrictId}
                                neighborhoodId={form.neighborhoodId}
                                neighborhoodName={neighborhoods.find((n) => n.id === form.neighborhoodId)?.name}
                                districtName={districts.find((d) => d.id === selectedDistrictId)?.name}
                                streetName={form.streetName}
                                buildingNo={form.buildingNo}
                                lat={form.latitude ?? null}
                                lng={form.longitude ?? null}
                                locationConfirmed={locationConfirmed}
                                onChange={onPositionChange}
                                onConfirm={onConfirmLocation}
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
                        onClick={onCancel}
                        className="px-5 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
                    >
                        İptal
                    </button>
                    <button
                        onClick={onSubmit}
                        disabled={creating}
                        className="px-5 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-lg text-sm font-medium disabled:opacity-60 transition-colors"
                    >
                        {creating ? 'Kaydediliyor...' : 'Kaydet'}
                    </button>
                </div>
            </div>
        </div>
    );
};
