import { lazy, Suspense, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { MapPin, Check, X, Pencil, Building2 } from 'lucide-react';
import type { FlyToTarget } from '@/components/map/BuildingMap';
import { BuildingSearchBox } from '@/components/map/BuildingSearchBox';
import { LoadingSpinner } from '@/components/ui';
import { getBuilding3dConfig, getBuildingById } from '@/api/buildings.api';
import { getDistricts } from '@/api/districts.api';
import { queryKeys } from '@/utils/queryKeys';
import { computeGeoJsonBbox } from '@/utils/geoBbox';

// bkz. MapPage.tsx'teki aynı yorum — maplibre-gl'i iki ayrı yerden statik import etmemek için.
const BuildingMap = lazy(() => import('@/components/map/BuildingMap').then((m) => ({ default: m.BuildingMap })));

export interface SelectedBuildingSummary {
    id: string;
    displayAddress: string | null;
    streetName: string | null;
    buildingNumber: string | null;
    neighborhoodId: string;
    neighborhoodName: string;
    latitude: number;
    longitude: number;
}

interface BuildingSelectorProps {
    districtId: string;
    value: SelectedBuildingSummary | null;
    onChange: (value: SelectedBuildingSummary | null) => void;
}

/** Hasar Tespiti formunun "Bina Konumu" bölümü için 3B bina seçici — sade: yalnızca basemap,
 *  adres arama, binalar, seçili bina (operasyonel katmanlar/legend/admin araçları YOK, bkz.
 *  ürün kararı). Binaya tıklamak formu DOĞRUDAN doldurmaz — önce bir onay kartı gösterilir,
 *  kullanıcı "Bu Binayı Seç"e basınca `onChange` çağrılır. */
export const BuildingSelector: React.FC<BuildingSelectorProps> = ({ districtId, value, onChange }) => {
    const [pendingBuildingId, setPendingBuildingId] = useState<string | null>(null);
    const [flyTo, setFlyTo] = useState<FlyToTarget | null>(null);
    const [picking, setPicking] = useState(false);

    const configQuery = useQuery({ queryKey: queryKeys.buildings.config(), queryFn: getBuilding3dConfig });
    const districtsQuery = useQuery({ queryKey: queryKeys.districts.all, queryFn: getDistricts });
    const district = districtsQuery.data?.find((d) => d.id === districtId);
    const initialBounds = useMemo(
        () => (district?.polygon ? computeGeoJsonBbox(district.polygon) : null),
        [district]
    );

    const pendingQuery = useQuery({
        queryKey: queryKeys.buildings.detail(pendingBuildingId ?? ''),
        queryFn: () => getBuildingById(pendingBuildingId!),
        enabled: !!pendingBuildingId,
    });

    const showPicker = picking || !value;

    const handleConfirm = () => {
        if (!pendingQuery.data) return;
        const b = pendingQuery.data;
        onChange({
            id: b.id,
            displayAddress: b.displayAddress,
            streetName: b.streetName,
            buildingNumber: b.buildingNumber,
            neighborhoodId: b.neighborhoodId,
            neighborhoodName: b.neighborhoodName,
            latitude: b.latitude,
            longitude: b.longitude,
        });
        setPendingBuildingId(null);
        setPicking(false);
    };

    // ── Zaten bir bina seçilmiş — kompakt kart ──
    if (value && !showPicker) {
        return (
            <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3">
                <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-2">
                        <Building2 className="h-4 w-4 text-blue-600 mt-0.5 shrink-0" />
                        <div>
                            <p className="text-xs font-semibold text-blue-800 uppercase tracking-wide">Seçilen Bina</p>
                            <p className="text-sm text-gray-900 font-medium mt-0.5">
                                {value.displayAddress ?? `${value.streetName ?? ''} ${value.buildingNumber ?? ''}`.trim()}
                            </p>
                            <p className="text-xs text-gray-500">
                                {value.neighborhoodName} Mah.{district ? ` · ${district.name}` : ''}
                            </p>
                        </div>
                    </div>
                    <div className="flex flex-col gap-1.5 shrink-0">
                        <button
                            type="button"
                            onClick={() => {
                                setFlyTo({ lat: value.latitude, lon: value.longitude, token: Date.now() });
                                setPicking(true);
                            }}
                            className="text-xs px-2.5 py-1 rounded-md border border-blue-300 bg-white text-blue-700 hover:bg-blue-100 font-medium"
                        >
                            Haritada Göster
                        </button>
                        <button
                            type="button"
                            onClick={() => { onChange(null); setPicking(true); }}
                            className="flex items-center gap-1 text-xs px-2.5 py-1 rounded-md border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 font-medium"
                        >
                            <Pencil className="h-3 w-3" /> Değiştir
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // ── Seçim modu — harita + arama + onay kartı ──
    return (
        <div className="space-y-2">
            <div className="bg-blue-50 border border-blue-200 rounded-lg px-3 py-2 text-xs text-blue-700 leading-relaxed">
                Binanızı haritadan seçin: adres arayın veya haritada bir binaya dokunun, ardından{' '}
                <span className="font-semibold">"Bu Binayı Seç"</span>e tıklayın.
            </div>

            <div className="relative" style={{ height: '360px', borderRadius: '8px', overflow: 'hidden', border: '1px solid #e5e7eb' }}>
                {(configQuery.isLoading || districtsQuery.isLoading) && (
                    <div className="absolute inset-0 z-20 flex items-center justify-center bg-white">
                        <LoadingSpinner size="md" />
                    </div>
                )}

                {configQuery.data && district && (
                    <Suspense fallback={<div className="absolute inset-0 flex items-center justify-center bg-white"><LoadingSpinner size="md" /></div>}>
                        <BuildingMap
                            mode="selector"
                            districtId={districtId}
                            initialBounds={initialBounds}
                            buildingMinZoom={configQuery.data.buildingMinZoom}
                            extrusionMinZoom={configQuery.data.extrusionMinZoom}
                            selectedBuildingGeometry={pendingQuery.data?.geometry ?? null}
                            onSelectBuilding={setPendingBuildingId}
                            flyTo={flyTo}
                        />
                    </Suspense>
                )}

                {configQuery.data && district && (
                    <div className="absolute top-2 left-2 right-2 z-10">
                        <BuildingSearchBox
                            districtId={districtId}
                            districtName={district?.name}
                            placeholder="Sokak veya adres ara"
                            className="bg-white rounded-md shadow"
                            onSelectResult={(r) => setFlyTo({ lat: r.latitude, lon: r.longitude, token: Date.now() })}
                        />
                    </div>
                )}

                {pendingBuildingId && (
                    <div className="absolute left-1/2 -translate-x-1/2 bottom-3 z-10 w-[calc(100%-24px)] max-w-sm">
                        <div className="bg-white rounded-lg shadow-xl border border-gray-200 p-3">
                            {pendingQuery.isLoading && (
                                <div className="flex justify-center py-2"><LoadingSpinner size="sm" /></div>
                            )}
                            {pendingQuery.data && (
                                <>
                                    <div className="flex items-start justify-between gap-2">
                                        <div>
                                            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Seçili Bina</p>
                                            <p className="text-sm font-medium text-gray-900 mt-0.5">
                                                {pendingQuery.data.displayAddress}
                                            </p>
                                            <p className="text-xs text-gray-500">
                                                {district ? `${pendingQuery.data.neighborhoodName} / ${district.name}` : pendingQuery.data.neighborhoodName}
                                            </p>
                                        </div>
                                        <button
                                            type="button"
                                            onClick={() => setPendingBuildingId(null)}
                                            aria-label="Vazgeç"
                                            className="text-gray-400 hover:text-gray-600 shrink-0"
                                        >
                                            <X className="h-4 w-4" />
                                        </button>
                                    </div>
                                    <button
                                        type="button"
                                        onClick={handleConfirm}
                                        className="mt-2 w-full flex items-center justify-center gap-1.5 text-sm px-3 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition-colors"
                                    >
                                        <Check className="h-4 w-4" /> Bu Binayı Seç
                                    </button>
                                </>
                            )}
                        </div>
                    </div>
                )}
            </div>

            {value && (
                <button
                    type="button"
                    onClick={() => setPicking(false)}
                    className="flex items-center gap-1.5 text-xs text-gray-500 hover:text-gray-700"
                >
                    <MapPin className="h-3 w-3" /> Seçimi iptal et, önceki binaya dön
                </button>
            )}
        </div>
    );
};
