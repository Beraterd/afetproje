import React from 'react';
import { X, Navigation, PackageOpen } from 'lucide-react';
import type {
    DistrictCoordinatorMapResponse,
    NeighborhoodCoordinatorMapResponse,
    MapDistrictResponse,
    MapNeighborhoodResponse,
} from '@/types';
import type { DamagePointResponse } from '@/types/damage';
import { googleMapsUrl } from './mapIcons';

export type SelectedMapEntity =
    | { kind: 'DAMAGE'; data: DamagePointResponse }
    | { kind: 'RESOURCE_DISTRICT'; data: MapDistrictResponse }
    | { kind: 'RESOURCE_NEIGHBORHOOD'; data: MapNeighborhoodResponse; districtName?: string }
    | { kind: 'DISTRICT_CENTER'; data: DistrictCoordinatorMapResponse }
    | { kind: 'NEIGHBORHOOD_CENTER'; data: NeighborhoodCoordinatorMapResponse };

interface MapEntityDetailsProps {
    entity: SelectedMapEntity | null;
    onClose: () => void;
}

const StatTile: React.FC<{ value: number; label: string; bg: string; fg: string }> = ({ value, label, bg, fg }) => (
    <div className="text-center rounded-md px-1 py-1.5" style={{ background: bg }}>
        <div className="text-base font-bold" style={{ color: fg }}>{value}</div>
        <div className="text-[10px] text-gray-500">{label}</div>
    </div>
);

const CenterDetails: React.FC<{
    title: string; subtitle?: string; address?: string;
    openEventCount: number; openResourceRequestCount: number; totalDamageCount: number;
    lat: number; lng: number; centerUpdatedAt?: string; accentColor: string;
}> = ({ title, subtitle, address, openEventCount, openResourceRequestCount, totalDamageCount, lat, lng, centerUpdatedAt, accentColor }) => (
    <div>
        <p className="text-sm font-bold" style={{ color: accentColor }}>{title}</p>
        {subtitle && <p className="text-xs text-gray-500 mt-0.5">{subtitle}</p>}
        {address && <p className="text-xs text-gray-600 italic mt-1">{address}</p>}
        <div className="grid grid-cols-3 gap-1.5 mt-3 mb-3">
            <StatTile value={openEventCount} label="Ekip İhtiyacı" bg="#eff6ff" fg="#1d4ed8" />
            <StatTile value={openResourceRequestCount} label="Kaynak Talebi" bg="#fef3c7" fg="#92400e" />
            <StatTile value={totalDamageCount} label="Hasar Tespiti" bg="#fee2e2" fg="#b91c1c" />
        </div>
        <a
            href={googleMapsUrl(lat, lng)}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 text-xs text-blue-700 bg-blue-50 rounded-md px-2.5 py-1.5 w-fit hover:bg-blue-100"
        >
            <Navigation className="h-3.5 w-3.5" aria-hidden="true" />
            Google Harita'da Yol Tarifi
        </a>
        {centerUpdatedAt && (
            <p className="text-[11px] text-gray-400 mt-2">
                Son güncelleme: {new Date(centerUpdatedAt).toLocaleDateString('tr-TR')}
            </p>
        )}
    </div>
);

function renderEntityContent(entity: SelectedMapEntity): React.ReactNode {
    switch (entity.kind) {
        case 'DAMAGE': {
            const point = entity.data;
            const flags: string[] = [];
            if (point.gasLeakRisk) flags.push('Gaz sızıntısı');
            if (point.collapseRisk) flags.push('Çökme riski');
            if (point.emergencyEvacuationNeeded) flags.push('Tahliye gerekli');
            if (point.casualtiesSuspected) flags.push('Kayıp şüphesi');
            return (
                <div>
                    <p className="text-sm font-bold text-gray-900 pb-2 border-b border-gray-100">Hasar Tespiti</p>
                    {point.address && (
                        <p className="text-xs text-gray-700 mt-2"><strong>Adres:</strong> {point.address}</p>
                    )}
                    {(point.districtName || point.neighborhoodName) && (
                        <p className="text-xs text-gray-500 mt-1">
                            {point.districtName}{point.neighborhoodName ? ' / ' + point.neighborhoodName : ''}
                        </p>
                    )}
                    <p className="text-xs text-gray-700 mt-1"><strong>Hasar:</strong> {point.damageLevelLabel}</p>
                    <p className="text-xs text-gray-700 mt-1"><strong>Durum:</strong> {point.verificationStatusLabel}</p>
                    {flags.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-2">
                            {flags.map((f) => (
                                <span key={f} className="text-[11px] bg-red-50 text-red-700 rounded-full px-2 py-0.5">{f}</span>
                            ))}
                        </div>
                    )}
                    <a
                        href={googleMapsUrl(point.latitude, point.longitude)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-1.5 text-xs text-blue-700 mt-3"
                    >
                        <Navigation className="h-3.5 w-3.5" aria-hidden="true" />
                        Google Harita'da Aç
                    </a>
                    {point.note && (
                        <p className="text-xs text-gray-600 mt-2 pt-2 border-t border-gray-100">
                            <strong>Not:</strong> {point.note}
                        </p>
                    )}
                </div>
            );
        }
        case 'RESOURCE_DISTRICT':
            return (
                <div>
                    <p className="text-sm font-bold text-gray-900 pb-2 border-b border-gray-100 flex items-center gap-1.5">
                        <PackageOpen className="h-4 w-4 text-blue-600" aria-hidden="true" /> Kaynak Talebi
                    </p>
                    <p className="text-xs text-gray-700 mt-2"><strong>İlçe:</strong> {entity.data.name}</p>
                    <p className="text-xs text-gray-700 mt-1"><strong>Açık Talep:</strong> {entity.data.openResourceRequestCount}</p>
                    <p className="text-xs text-gray-400 mt-2">Detay için ilçeyi seçin.</p>
                </div>
            );
        case 'RESOURCE_NEIGHBORHOOD':
            return (
                <div>
                    <p className="text-sm font-bold text-gray-900 pb-2 border-b border-gray-100 flex items-center gap-1.5">
                        <PackageOpen className="h-4 w-4 text-blue-600" aria-hidden="true" /> Kaynak Talebi
                    </p>
                    <p className="text-xs text-gray-700 mt-2"><strong>Mahalle:</strong> {entity.data.name}</p>
                    {entity.districtName && <p className="text-xs text-gray-700 mt-1"><strong>İlçe:</strong> {entity.districtName}</p>}
                    <p className="text-xs text-gray-700 mt-1"><strong>Açık Talep:</strong> {entity.data.openResourceRequestCount}</p>
                </div>
            );
        case 'DISTRICT_CENTER':
            return (
                <CenterDetails
                    title={`${entity.data.districtName} Koordinasyon Merkezi`}
                    subtitle="İlçe Koordinasyon Merkezi"
                    address={entity.data.address}
                    openEventCount={entity.data.openEventCount}
                    openResourceRequestCount={entity.data.openResourceRequestCount}
                    totalDamageCount={entity.data.totalDamageCount}
                    lat={entity.data.latitude}
                    lng={entity.data.longitude}
                    centerUpdatedAt={entity.data.centerUpdatedAt}
                    accentColor="#1d4ed8"
                />
            );
        case 'NEIGHBORHOOD_CENTER':
            return (
                <CenterDetails
                    title={`${entity.data.neighborhoodName} Koordinasyon Merkezi`}
                    subtitle={entity.data.districtName ? `${entity.data.districtName} İlçesi` : undefined}
                    address={entity.data.address}
                    openEventCount={entity.data.openEventCount}
                    openResourceRequestCount={entity.data.openResourceRequestCount}
                    totalDamageCount={entity.data.totalDamageCount}
                    lat={entity.data.latitude}
                    lng={entity.data.longitude}
                    centerUpdatedAt={entity.data.centerUpdatedAt}
                    accentColor="#7c3aed"
                />
            );
        default:
            return null;
    }
}

/**
 * Haritada seçilen TEK varlığın (hasar/kaynak/merkez) detayını gösteren panel. Native Leaflet
 * Popup'ların yerini alır — bu sayede aynı anda yalnızca bir detay açık olabilir (state tek bir
 * `entity` değeri) ve stil app'in geri kalanıyla tutarlıdır. Masaüstünde sol-alt köşede kompakt
 * bir kart, mobilde alttan açılan tam genişlik bir sheet.
 */
export const MapEntityDetails: React.FC<MapEntityDetailsProps> = ({ entity, onClose }) => {
    if (!entity) return null;

    return (
        <div
            className="absolute z-[1000] left-1/2 -translate-x-1/2 bottom-3 w-[calc(100%-24px)] max-w-sm
                       sm:left-3 sm:translate-x-0 sm:bottom-3 sm:w-[300px] sm:max-w-none"
        >
            <div className="bg-white rounded-lg shadow-lg border border-gray-200 p-4 max-h-[50vh] overflow-y-auto">
                <button
                    onClick={onClose}
                    aria-label="Detayı kapat"
                    className="float-right text-gray-400 hover:text-gray-600 -mt-1 -mr-1"
                >
                    <X className="h-4 w-4" />
                </button>
                {renderEntityContent(entity)}
            </div>
        </div>
    );
};
