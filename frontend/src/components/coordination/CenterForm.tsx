import React, { useState, useEffect } from 'react';
import { MapPin, Save } from 'lucide-react';
import { LoadingSpinner } from '@/components/ui';
import { CenterLocationPickerMap } from '@/components/map/CenterLocationPickerMap';
import type { CoordinationCenterResponse, UpsertCoordinationCenterRequest } from '@/types/coordinationCenter';
import { CenterFormState, defaultCenterForm, centerFormFromResponse } from './coordinationCenterForm';

interface CenterFormProps {
    polygon: any;
    boundaryLabel: string;
    areaName?: string;
    districtName?: string;
    existingCenter?: CoordinationCenterResponse;
    onSave: (req: UpsertCoordinationCenterRequest) => Promise<CoordinationCenterResponse>;
    saving: boolean;
}

/** İlçe/mahalle koordinasyon merkezi adres + harita konumu formu. Hem DistrictSection hem
 *  NeighborhoodSection tarafından kullanılır — kaydetme mantığı `onSave` ile üst container'a
 *  (query mutation) delege edilir, form kendi API çağrısını yapmaz. */
export const CenterForm: React.FC<CenterFormProps> = ({
    polygon,
    boundaryLabel,
    areaName,
    districtName,
    existingCenter,
    onSave,
    saving,
}) => {
    const [form, setForm] = useState<CenterFormState>(
        existingCenter ? centerFormFromResponse(existingCenter) : defaultCenterForm()
    );

    // Reset form when existingCenter changes (e.g. switching districts)
    useEffect(() => {
        setForm(existingCenter ? centerFormFromResponse(existingCenter) : defaultCenterForm());
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
