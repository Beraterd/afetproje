import { lazy, Suspense, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { FlyToTarget } from '@/components/map/BuildingMap';
import { BuildingSearchBox } from '@/components/map/BuildingSearchBox';
import { BuildingLegend } from '@/components/map/BuildingLegend';
import { BuildingDetailsPanel } from '@/components/map/BuildingDetailsPanel';
import { LoadingSpinner } from '@/components/ui';
import { getBuilding3dConfig } from '@/api/buildings.api';
import { getDistricts } from '@/api/districts.api';
import { queryKeys } from '@/utils/queryKeys';
import { computeGeoJsonBbox } from '@/utils/geoBbox';

// bkz. MapPage.tsx'teki aynı yorum — üç kullanım noktası (burada, MapPage, BuildingSelector)
// aynı dinamik import referansını paylaşır, tek bir chunk'a dedupe edilir.
const BuildingMap = lazy(() => import('@/components/map/BuildingMap').then((m) => ({ default: m.BuildingMap })));

/**
 * Geliştirici/debug rotası — `/map-3d` normal kullanıcı navigasyonunda (sidebar) GÖRÜNMEZ.
 * Üretim akışında 3B bina katmanına Operasyon Haritası'ndaki Pendik/mahalle drill-down'ından
 * ulaşılır (bkz. MapPage.tsx). Bu sayfa yalnızca `BuildingMap`/`BuildingSearchBox`/
 * `BuildingDetailsPanel`'i izole test etmek için tutuluyor.
 */
export const Map3DPage: React.FC = () => {
    const [selectedBuildingId, setSelectedBuildingId] = useState<string | null>(null);
    const [flyTo, setFlyTo] = useState<FlyToTarget | null>(null);

    const configQuery = useQuery({ queryKey: queryKeys.buildings.config(), queryFn: getBuilding3dConfig });
    const districtsQuery = useQuery({ queryKey: queryKeys.districts.all, queryFn: getDistricts });

    const pilotDistrict = useMemo(() => {
        if (!configQuery.data || !districtsQuery.data) return null;
        return districtsQuery.data.find((d) => configQuery.data!.enabledDistricts.includes(d.name)) ?? null;
    }, [configQuery.data, districtsQuery.data]);

    const initialBounds = useMemo(
        () => (pilotDistrict?.polygon ? computeGeoJsonBbox(pilotDistrict.polygon) : null),
        [pilotDistrict]
    );

    const isLoading = configQuery.isLoading || districtsQuery.isLoading;
    const isError = configQuery.isError || districtsQuery.isError;

    return (
        <div className="relative w-full h-[calc(100vh-240px)] min-h-[500px] rounded-lg overflow-hidden border border-slate-300 shadow-sm">
            {isLoading && (
                <div className="absolute inset-0 z-20 flex items-center justify-center bg-white">
                    <LoadingSpinner size="lg" label="3B bina pilotu yükleniyor..." />
                </div>
            )}
            {isError && (
                <div className="absolute inset-0 z-20 flex items-center justify-center bg-white px-6 text-center">
                    <p className="text-red-600 text-sm">3B bina pilotu yapılandırması yüklenemedi.</p>
                </div>
            )}
            {!isLoading && !isError && !pilotDistrict && (
                <div className="absolute inset-0 z-20 flex items-center justify-center bg-white px-6 text-center">
                    <p className="text-slate-600 text-sm max-w-sm">
                        Pilot ilçe ({configQuery.data?.enabledDistricts.join(', ')}) sistemde bulunamadı.
                    </p>
                </div>
            )}

            {!isLoading && !isError && pilotDistrict && configQuery.data && (
                <Suspense fallback={<div className="absolute inset-0 flex items-center justify-center bg-white"><LoadingSpinner size="lg" /></div>}>
                    <BuildingMap
                        mode="operations"
                        districtId={pilotDistrict.id}
                        initialBounds={initialBounds}
                        buildingMinZoom={configQuery.data.buildingMinZoom}
                        extrusionMinZoom={configQuery.data.extrusionMinZoom}
                        selectedBuildingGeometry={null}
                        onSelectBuilding={setSelectedBuildingId}
                        flyTo={flyTo}
                    />
                </Suspense>
            )}

            {pilotDistrict && (
                <div className="absolute top-4 left-4 z-10 w-72 max-w-[calc(100%-2rem)]">
                    <BuildingSearchBox
                        districtId={pilotDistrict.id}
                        onSelectResult={(r) => {
                            setFlyTo((prev) => ({ lat: r.latitude, lon: r.longitude, token: (prev?.token ?? 0) + 1 }));
                        }}
                    />
                </div>
            )}

            <BuildingLegend />

            {selectedBuildingId && (
                <BuildingDetailsPanel buildingId={selectedBuildingId} onClose={() => setSelectedBuildingId(null)} />
            )}
        </div>
    );
};
