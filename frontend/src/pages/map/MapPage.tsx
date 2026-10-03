import React, { lazy, Suspense, useEffect, useMemo, useRef, useState } from 'react';
import L from 'leaflet';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
    getMapDistricts,
    getMapNeighborhoods,
    getMapDamagePoints,
    getDistrictCentersForMap,
    getNeighborhoodCentersForMap,
} from '@/api/map.api';
import { getMapDamageSummary } from '@/api/damageAssessments.api';
import { queryOperationsAi, OperationsAiResponse } from '@/api/operationsAi.api';
import { queryKeys } from '@/utils/queryKeys';
import { LoadingSpinner } from '@/components/ui';
import { MapDistrictResponse, MapNeighborhoodResponse, DistrictCoordinatorMapResponse, NeighborhoodCoordinatorMapResponse } from '@/types';
import { DamagePointResponse } from '@/types/damage';
import { RoleGuard } from '@/layouts/RoleGuard';
import { useAuthStore } from '@/store/authStore';
import { Bot } from 'lucide-react';
import { MapBreadcrumb } from '@/components/map/MapBreadcrumb';
import { MapLayerControls } from '@/components/map/MapLayerControls';
import { MapEntityDetails, SelectedMapEntity } from '@/components/map/MapEntityDetails';
import { AiDecisionSupportDrawer } from '@/components/map/AiDecisionSupportDrawer';
import { OperationMap } from '@/components/map/OperationMap';
import type { FlyToTarget, StreetFlyToTarget } from '@/components/map/BuildingMap';
import { MapSearchBox } from '@/components/map/MapSearchBox';
import { BuildingLegend } from '@/components/map/BuildingLegend';
import { BuildingDetailsPanel } from '@/components/map/BuildingDetailsPanel';
import { BuildingColorModeControl, BuildingColorMode } from '@/components/map/BuildingColorModeControl';
import { getBuilding3dConfig, getBuildingById } from '@/api/buildings.api';
import { getStreetById } from '@/api/streets.api';
import { StreetSearchResultResponse } from '@/types/street';
import { LayerState, LayerKey, getDefaultLayersForRole } from '@/components/map/operationMapLayers';
import { ErrorBoundary } from '@/components/shared/ErrorBoundary';
import { isWebglSupported } from '@/utils/webgl';

// maplibre-gl (~900kB) yalnızca gerçekten Pendik/mahalle görünümüne girildiğinde indirilsin diye
// dinamik import — BuildingMap hem burada hem BuildingSelector.tsx'te (Hasar Tespiti) statik
// import edilseydi, iki farklı lazy route'tan paylaşılan bir modül olarak ana bundle'a taşınırdı.
const BuildingMap = lazy(() => import('@/components/map/BuildingMap').then((m) => ({ default: m.BuildingMap })));

export const MapPage: React.FC = () => {
    const user = useAuthStore((s) => s.user);

    const [activeDistrictId, setActiveDistrictId]       = useState<string | null>(null);
    const [activeNeighborhoodId, setActiveNeighborhoodId] = useState<string | null>(null);
    const [activeBounds, setActiveBounds]               = useState<L.LatLngBounds | null>(null);
    const [activeNeighborhoodPolygon, setActiveNeighborhoodPolygon] = useState<GeoJSON.Geometry | null>(null);
    const [showDistrictList, setShowDistrictList]       = useState(false);
    const [selectedEntity, setSelectedEntity]           = useState<SelectedMapEntity | null>(null);

    // ── 3B bina katmanı — mahalle seçilip ilçe 3B-uygunsa (bkz. building3dConfig.enabledDistricts)
    // Leaflet render'ının yerini alır, bkz. is3dActive aşağıda. ──
    const [selectedBuildingId, setSelectedBuildingId]   = useState<string | null>(null);
    const [buildingFlyTo, setBuildingFlyTo]             = useState<FlyToTarget | null>(null);
    const [colorMode, setColorMode]                     = useState<BuildingColorMode>('damage');
    // ── Sokak arama — item 21-25,41,42: seçim yalnız id tutar, geometri React Query cache'inden
    // gelir (BuildingDetailsPanel'in selectedBuildingId'yle aynı deseni); mahalle/ilçe değişince
    // aşağıdaki handler'larda sıfırlanır. ──
    const [selectedStreetId, setSelectedStreetId]       = useState<string | null>(null);
    const [streetFlyTo, setStreetFlyTo]                 = useState<StreetFlyToTarget | null>(null);
    // WebGL yoksa BuildingMap (maplibre-gl) hiç mount edilmez — Leaflet OperationMap'e düşer,
    // bkz. is3dActive aşağıda. Cihaz ömrü boyunca sabit olduğundan bir kez hesaplanır.
    const webglOk = useMemo(() => isWebglSupported(), []);

    // ── maplibre-gl (~900kB) chunk'ını tarayıcı boştayken önceden ısıt ──
    // Kullanıcı bir mahalleye tıklayıp BuildingMap'in Suspense fallback'ine düşene kadar bu
    // chunk hiç indirilmemiş olabilir — ilk indirme+parse+WebGL init o anda senkron bir
    // gecikme/kasma olarak hissedilir ("ilk 3B görünüm yüklenirken kasma"). requestIdleCallback
    // ile sayfa açılışının/ilk boyamanın ARKASINDAN, tarayıcı asıl işini bitirdikten sonra
    // indirmeyi tetikleriz — map initialization'ı geciktirmez (bkz. kural H), yalnızca
    // kullanıcı mahalleye gerçekten tıkladığında chunk çoktan cache'te olur. WebGL yoksa hiç
    // tetiklenmez (o cihazda 3B asla açılmayacak, boşuna bant genişliği harcanmasın).
    useEffect(() => {
        if (!webglOk) return;
        const prefetch = () => { void import('@/components/map/BuildingMap'); };
        const ric = (window as any).requestIdleCallback as ((cb: () => void) => number) | undefined;
        if (ric) {
            const id = ric(prefetch);
            return () => (window as any).cancelIdleCallback?.(id);
        }
        const timeoutId = window.setTimeout(prefetch, 2000);
        return () => window.clearTimeout(timeoutId);
    }, [webglOk]);

    // Rol default'u yalnızca ilk mount'ta hesaplanır — kullanıcı layer'ları değiştirdikten
    // sonra sayfa içindeki başka bir re-render bunu resetlemez (bkz. #17).
    const [layers, setLayers] = useState<LayerState>(() => getDefaultLayersForRole(user?.role));

    const toggleLayer = (key: LayerKey) =>
        setLayers((prev) => ({ ...prev, [key]: !prev[key] }));

    // ── AI assistant state ──
    const [aiDrawerOpen, setAiDrawerOpen] = useState(false);
    const [aiPrompt, setAiPrompt]       = useState('');
    const [aiResult, setAiResult]       = useState<OperationsAiResponse | null>(null);
    const [aiError, setAiError]         = useState<string | null>(null);
    const aiResultRef                   = useRef<HTMLDivElement>(null);

    const aiMutation = useMutation({
        mutationFn: queryOperationsAi,
        onSuccess: (data) => {
            setAiResult(data);
            setAiError(null);
            setTimeout(() => aiResultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 100);
        },
        onError: (err: any) => {
            setAiError(err?.message ?? 'Bir hata oluştu. Lütfen tekrar deneyin.');
        },
    });

    const handleAiSubmit = () => {
        if (!aiPrompt.trim() || aiMutation.isPending) return;
        setAiResult(null);
        setAiError(null);
        aiMutation.mutate({ prompt: aiPrompt.trim() });
    };

    const handleQuickQuery = (prompt: string) => {
        setAiPrompt(prompt);
        setAiResult(null);
        setAiError(null);
        aiMutation.mutate({ prompt });
    };

    // ── Data fetches ──
    const { data: districts, isLoading: loadingDistricts } = useQuery({
        queryKey: queryKeys.map.districts(),
        queryFn:  getMapDistricts,
    });

    const { data: neighborhoods, isLoading: loadingNeighborhoods } = useQuery({
        queryKey: queryKeys.map.neighborhoods(activeDistrictId!),
        queryFn:  () => getMapNeighborhoods(activeDistrictId!),
        enabled:  !!activeDistrictId,
    });

    const { data: damagePoints = [] } = useQuery<DamagePointResponse[]>({
        queryKey: queryKeys.map.damagePoints(activeDistrictId ?? undefined, activeNeighborhoodId ?? undefined),
        queryFn:  () => getMapDamagePoints(
            activeNeighborhoodId ? undefined : activeDistrictId ?? undefined,
            activeNeighborhoodId ?? undefined
        ),
        enabled: layers.damage,
        // §1 — Ek güvenlik filtresi: asıl filtre backend'de uygulanır; burada yalnızca
        // sahada doğrulanıp koordinatör onaylı (KOORDINATOR_ONAYLADI) ve koordinatı olan
        // pinler haritaya çizilir. İnceleme bekleyen kayıtlar hiçbir koşulda görünmez.
        select: (rows) => rows.filter(
            (p) => p.verificationStatus === 'KOORDINATOR_ONAYLADI'
                && p.latitude != null && p.longitude != null
        ),
    });

    const { data: districtCenters = [] } = useQuery<DistrictCoordinatorMapResponse[]>({
        queryKey: queryKeys.map.districtCenters(),
        queryFn:  getDistrictCentersForMap,
        enabled:  layers.districtCenters,
    });

    const { data: neighborhoodCenters = [] } = useQuery<NeighborhoodCoordinatorMapResponse[]>({
        queryKey: queryKeys.map.neighborhoodCenters(activeDistrictId ?? undefined),
        queryFn:  () => getNeighborhoodCentersForMap(activeDistrictId ?? undefined),
        enabled:  layers.neighborhoodCenters && !!activeDistrictId,
    });

    // ── 3B bina katmanı ──
    const { data: building3dConfig } = useQuery({
        queryKey: queryKeys.buildings.config(),
        queryFn:  getBuilding3dConfig,
    });
    const activeNeighborhood     = neighborhoods?.find((nb) => nb.id === activeNeighborhoodId);
    const activeNeighborhoodName = activeNeighborhood?.name;
    const activeDistrictName     = districts?.find((d) => d.id === activeDistrictId)?.name;
    const is3dEligible = !!activeNeighborhoodId && !!activeDistrictId && !!activeDistrictName
        && !!building3dConfig?.enabledDistricts.includes(activeDistrictName);
    const is3dActive = is3dEligible && webglOk;
    const is3dBlockedByWebgl = is3dEligible && !webglOk;

    // Mahalle bina sayısı — mevcut `neighborhoods` fetch'inden geliyor, ayrı bir query gerekmez.
    const activeNeighborhoodBuildingCount = activeNeighborhood?.buildingCount;

    const { data: damageSummary } = useQuery({
        queryKey: queryKeys.map.damageSummary(),
        queryFn:  getMapDamageSummary,
        enabled:  is3dActive,
    });
    const activeNeighborhoodDamageSummary = damageSummary?.find((s) => s.neighborhoodId === activeNeighborhoodId);
    // Sıfır olan seviyeler gösterilmez, hepsi sıfırsa hiçbir şey gösterilmez — sahte veri yok.
    const damageSummaryText = activeNeighborhoodDamageSummary
        ? [
            activeNeighborhoodDamageSummary.heavyCount > 0 ? `${activeNeighborhoodDamageSummary.heavyCount} ağır` : null,
            activeNeighborhoodDamageSummary.collapsedCount > 0 ? `${activeNeighborhoodDamageSummary.collapsedCount} yıkılmış` : null,
        ].filter(Boolean).join(' · ') || null
        : null;

    // BuildingDetailsPanel aynı query key'i kullanır — React Query cache'i paylaşır, çift istek atılmaz.
    const { data: selectedBuildingDetail } = useQuery({
        queryKey: queryKeys.buildings.detail(selectedBuildingId ?? ''),
        queryFn:  () => getBuildingById(selectedBuildingId!),
        enabled:  is3dActive && !!selectedBuildingId,
    });

    // Seçili sokağın tam geometrisi — yalnız highlight çizmek için (autocomplete sonucu geometri
    // taşımaz, bkz. StreetSearchResultResponse).
    const { data: selectedStreetDetail } = useQuery({
        queryKey: queryKeys.streets.detail(selectedStreetId ?? ''),
        queryFn:  () => getStreetById(selectedStreetId!),
        enabled:  is3dActive && !!selectedStreetId,
    });

    const isLoading = loadingDistricts || (!!activeDistrictId && loadingNeighborhoods);

    // ── Handlers ──
    const handleDistrictClick = (district: MapDistrictResponse) => {
        setActiveDistrictId(district.id);
        setActiveNeighborhoodId(null);
        setActiveNeighborhoodPolygon(null);
        setShowDistrictList(false);
        setSelectedEntity(null);
        setSelectedBuildingId(null);
        setBuildingFlyTo(null);
        setSelectedStreetId(null);
        setStreetFlyTo(null);
        if (district.polygon) {
            try {
                setActiveBounds(L.geoJSON(district.polygon).getBounds());
            } catch { setActiveBounds(null); }
        } else {
            setActiveBounds(null);
        }
    };

    const handleNeighborhoodClick = (nb: MapNeighborhoodResponse) => {
        setActiveNeighborhoodId(nb.id);
        setSelectedEntity(null);
        setSelectedBuildingId(null);
        setBuildingFlyTo(null);
        setSelectedStreetId(null);
        setStreetFlyTo(null);
        // Mahalle bounds'una zoom — 3B-uygun ilçelerde BuildingMap bu bounds'u initialBounds
        // olarak devralır (aynı konumda, ani sıçrama olmadan render motoru değişir).
        if (nb.polygon) {
            try {
                setActiveBounds(L.geoJSON(nb.polygon).getBounds());
                setActiveNeighborhoodPolygon(nb.polygon);
            } catch {
                setActiveNeighborhoodPolygon(null);
                /* bounds hesaplanamazsa mevcut görünüm korunur */
            }
        } else {
            setActiveNeighborhoodPolygon(null);
        }
    };
    const handleBackToCity = () => {
        setActiveDistrictId(null);
        setActiveNeighborhoodId(null);
        setActiveBounds(null);
        setActiveNeighborhoodPolygon(null);
        setSelectedEntity(null);
        setSelectedBuildingId(null);
        setBuildingFlyTo(null);
        setSelectedStreetId(null);
        setStreetFlyTo(null);
    };
    const handleBackToDistrict = () => {
        setActiveNeighborhoodId(null);
        setActiveNeighborhoodPolygon(null);
        setSelectedEntity(null);
        setSelectedBuildingId(null);
        setBuildingFlyTo(null);
        setSelectedStreetId(null);
        setStreetFlyTo(null);
    };

    const handleSelectStreet = (result: StreetSearchResultResponse) => {
        setSelectedStreetId(result.id);
        setStreetFlyTo((prev) => ({
            bounds: [result.bounds.minLon, result.bounds.minLat, result.bounds.maxLon, result.bounds.maxLat],
            center: result.center,
            token: (prev?.token ?? 0) + 1,
        }));
    };

    const layerCounts = {
        damage: damagePoints.length,
        resources: (activeDistrictId ? neighborhoods : districts)
            ?.reduce((sum, area: any) => sum + (area.openResourceRequestCount ?? 0), 0),
        districtCenters: districtCenters.length,
        neighborhoodCenters: neighborhoodCenters.length,
    };

    return (
        <div className="flex flex-col gap-4 pb-10">
            <MapBreadcrumb
                districts={districts}
                neighborhoods={neighborhoods}
                activeDistrictId={activeDistrictId}
                activeNeighborhoodId={activeNeighborhoodId}
                activeDistrictName={activeDistrictName}
                activeNeighborhoodName={activeNeighborhoodName}
                showDistrictList={showDistrictList}
                onToggleDistrictList={() => setShowDistrictList((v) => !v)}
                onSelectDistrict={handleDistrictClick}
                onSelectNeighborhood={handleNeighborhoodClick}
                onBackToCity={handleBackToCity}
                onBackToDistrict={handleBackToDistrict}
            />

            {/* Map + overlay panels */}
            <div className="relative h-[calc(100vh-240px)] min-h-[400px] bg-gray-100 rounded-lg overflow-hidden border border-gray-300 shadow-sm z-0">
                {isLoading && (
                    <div className="absolute inset-0 z-50 flex items-center justify-center bg-white/50 backdrop-blur-sm">
                        <LoadingSpinner size="lg" />
                    </div>
                )}

                {/* 2B'ye özgü katman kontrolleri/seçim paneli — 3B bina görünümünde bu Leaflet
                    layer'ları (risk/hasar/kaynak/merkez) hiç render edilmediği için gösterilmez. */}
                {!is3dActive && (
                    <>
                        <MapLayerControls layers={layers} counts={layerCounts} onChange={toggleLayer} />
                        <MapEntityDetails entity={selectedEntity} onClose={() => setSelectedEntity(null)} />
                    </>
                )}

                <div className="absolute z-[1000] top-3 left-3 flex flex-col items-start gap-2 w-72 max-w-[calc(100%-90px)]">
                    {is3dActive && activeDistrictId && (
                        <MapSearchBox
                            districtId={activeDistrictId}
                            districtName={activeDistrictName}
                            neighborhoodId={activeNeighborhoodId}
                            className="w-full bg-white rounded-full shadow-lg [&_input]:rounded-full [&_input]:border-0 [&_input]:shadow-none"
                            onSelectBuilding={(r) => {
                                setBuildingFlyTo((prev) => ({ lat: r.latitude, lon: r.longitude, token: (prev?.token ?? 0) + 1 }));
                            }}
                            onSelectStreet={handleSelectStreet}
                        />
                    )}

                    {is3dActive && activeNeighborhoodName && (
                        <div className="bg-white/95 backdrop-blur rounded-full shadow px-3 py-1 text-[11px] text-slate-600 flex items-center gap-1.5 max-w-full">
                            <span className="font-medium text-slate-800 truncate">{activeNeighborhoodName} Mahallesi</span>
                            {typeof activeNeighborhoodBuildingCount === 'number' && (
                                <span className="shrink-0">· {activeNeighborhoodBuildingCount.toLocaleString('tr-TR')} bina</span>
                            )}
                            {damageSummaryText && <span className="shrink-0 text-slate-500">· {damageSummaryText}</span>}
                        </div>
                    )}

                    {/* AI Karar Destek açma butonu — yalnızca yönetici/koordinatör rollerinde */}
                    <RoleGuard allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR']}>
                        <button
                            onClick={() => setAiDrawerOpen(true)}
                            aria-expanded={aiDrawerOpen}
                            aria-controls="ai-decision-support-drawer"
                            className="flex items-center gap-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium px-3 py-2 rounded-full shadow-lg"
                        >
                            <Bot className="h-4 w-4" aria-hidden="true" />
                            AI Karar Destek
                            {aiResult && (
                                <span className="h-1.5 w-1.5 rounded-full bg-white" aria-label="Hazır analiz sonucu var" />
                            )}
                        </button>
                    </RoleGuard>
                </div>

                {!is3dActive && layers.neighborhoodCenters && !activeDistrictId && (
                    <div
                        style={{ position: 'absolute', bottom: 24, left: '50%', transform: 'translateX(-50%)', zIndex: 1000 }}
                        className="bg-violet-50 border border-violet-200 text-violet-800 text-xs px-4 py-2 rounded-full shadow-md whitespace-nowrap pointer-events-none"
                    >
                        Mahalle koordinasyon merkezlerini görmek için önce bir ilçe seçin.
                    </div>
                )}

                {is3dActive && activeDistrictId && activeNeighborhoodId && building3dConfig ? (
                    <ErrorBoundary level="section" sectionName="3B Bina Katmanı">
                        {building3dConfig.totalBuildingCount === 0 ? (
                            <div className="absolute inset-0 flex items-center justify-center bg-white px-6 text-center">
                                <div className="max-w-sm">
                                    <p className="text-sm text-slate-600">Bu ilçe için bina verisi henüz içe aktarılmamış.</p>
                                    <RoleGuard allowedRoles={['ADMIN']}>
                                        <p className="text-xs text-slate-400 mt-2">
                                            Bina verisini Sistem Bakımı bölümünden içe aktarabilirsiniz.
                                        </p>
                                    </RoleGuard>
                                </div>
                            </div>
                        ) : activeNeighborhood?.buildingCount === 0 ? (
                            // İlçenin geneli için veri var ama BU mahallede yok — Pendik'in tamamına
                            // genelleştirmede gerçek DB envanterinde bulunan bir durum (bazı mahallelerin
                            // sınır/import verisi eksik). Mahalle sınırı da genelde bozuk olduğundan
                            // (bkz. rapor) BuildingMap'i hiç mount etmeyip net bir mesaj gösteriyoruz.
                            <div className="absolute inset-0 flex items-center justify-center bg-white px-6 text-center">
                                <p className="text-sm text-slate-600 max-w-sm">
                                    Bu mahalle için bina verisi bulunamadı.
                                </p>
                            </div>
                        ) : (
                            <>
                                {/* `key={activeNeighborhoodId}`: BuildingMap mahalleler arasında yeniden
                                    KULLANILMAZ, her mahalle seçiminde tamamen yeniden mount edilir —
                                    kamera fitBounds/zoom-taban/mahalle sınır seed'i yalnızca mount-time
                                    effect'inde çalışıyor (bkz. BuildingMap.tsx), bu yüzden prop değişimiyle
                                    "reaktif" güncelleme YETERLİ DEĞİL: key olmadan ikinci bir mahalleye
                                    geçişte kamera eski mahallede kalır, source'ta eski mahallenin binaları
                                    görünmeye devam edebilirdi. Remount bunu kökten engelliyor. */}
                                <Suspense fallback={<div className="absolute inset-0 flex items-center justify-center bg-white"><LoadingSpinner size="lg" /></div>}>
                                    <BuildingMap
                                        key={activeNeighborhoodId}
                                        mode="operations"
                                        districtId={activeDistrictId}
                                        neighborhoodId={activeNeighborhoodId}
                                        neighborhoodName={activeNeighborhoodName}
                                        initialBounds={activeBounds
                                            ? [activeBounds.getWest(), activeBounds.getSouth(), activeBounds.getEast(), activeBounds.getNorth()]
                                            : null}
                                        buildingMinZoom={building3dConfig.buildingMinZoom}
                                        extrusionMinZoom={building3dConfig.extrusionMinZoom}
                                        selectedBuildingGeometry={selectedBuildingDetail?.geometry ?? null}
                                        onSelectBuilding={setSelectedBuildingId}
                                        flyTo={buildingFlyTo}
                                        colorMode={colorMode}
                                        neighborhoodBoundary={activeNeighborhoodPolygon}
                                        selectedStreetGeometry={selectedStreetDetail?.geometry ?? null}
                                        streetFlyTo={streetFlyTo}
                                    />
                                </Suspense>
                                <BuildingLegend mode={colorMode} />
                                <BuildingColorModeControl value={colorMode} onChange={setColorMode} />
                                {selectedBuildingId && (
                                    <BuildingDetailsPanel buildingId={selectedBuildingId} onClose={() => setSelectedBuildingId(null)} />
                                )}
                            </>
                        )}
                    </ErrorBoundary>
                ) : (
                    <ErrorBoundary level="section" sectionName="Operasyon Haritası">
                        <OperationMap
                            layers={layers}
                            districts={districts}
                            neighborhoods={neighborhoods}
                            damagePoints={damagePoints}
                            districtCenters={districtCenters}
                            neighborhoodCenters={neighborhoodCenters}
                            activeDistrictId={activeDistrictId}
                            activeNeighborhoodId={activeNeighborhoodId}
                            activeBounds={activeBounds}
                            activeDistrictName={activeDistrictName}
                            onSelectDistrict={handleDistrictClick}
                            onSelectNeighborhood={handleNeighborhoodClick}
                            onSelectEntity={setSelectedEntity}
                        />
                        {is3dBlockedByWebgl && (
                            <div
                                style={{ position: 'absolute', bottom: 24, left: '50%', transform: 'translateX(-50%)', zIndex: 1000 }}
                                className="bg-amber-50 border border-amber-200 text-amber-800 text-xs px-4 py-2 rounded-full shadow-md whitespace-nowrap pointer-events-none"
                            >
                                3B görünüm bu cihazda desteklenmiyor, 2B görünüme geçildi.
                            </div>
                        )}
                    </ErrorBoundary>
                )}
            </div>

            <ErrorBoundary level="section" sectionName="AI Karar Destek">
                <AiDecisionSupportDrawer
                    open={aiDrawerOpen}
                    onClose={() => setAiDrawerOpen(false)}
                    prompt={aiPrompt}
                    onPromptChange={setAiPrompt}
                    onSubmit={handleAiSubmit}
                    onQuickQuery={handleQuickQuery}
                    isPending={aiMutation.isPending}
                    result={aiResult}
                    error={aiError}
                    resultRef={aiResultRef}
                />
            </ErrorBoundary>
        </div>
    );
};
