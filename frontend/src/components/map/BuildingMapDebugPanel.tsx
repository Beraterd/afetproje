import { useState } from 'react';
import type { Map as MaplibreMap } from 'maplibre-gl';

/** Herhangi bir MapLibre okuması exception atarsa TÜM sonucu kaybetmemek için — her değer bağımsız
 *  okunur, biri patlarsa yalnızca o alan `{ error }` olarak görünür, diğerleri normal render olur. */
type SafeReadResult<T> = T | { error: string };

function safeRead<T>(fn: () => T): SafeReadResult<T> {
    try {
        return fn();
    } catch (error) {
        return { error: error instanceof Error ? error.message : String(error) };
    }
}

function isOk<T>(value: SafeReadResult<T>): value is T {
    return !(value !== null && typeof value === 'object' && 'error' in (value as object));
}

interface DebugResult {
    status: 'reading' | 'ok' | 'error' | 'no-map';
    mapExists: boolean;
    error?: string;
    selectedNeighborhoodId?: string | null;
    selectedNeighborhoodName?: string | null;
    apiFeatureCount?: number | null;
    lastSetDataFeatureCount?: number | null;
    zoom?: SafeReadResult<number>;
    pitch?: SafeReadResult<number>;
    bearing?: SafeReadResult<number>;
    buildingSourceExists?: SafeReadResult<boolean>;
    buildingLayerExists?: SafeReadResult<boolean>;
    visibility?: SafeReadResult<unknown>;
    fillExtrusionHeight?: SafeReadResult<unknown>;
    fillExtrusionColor?: SafeReadResult<unknown>;
    fillExtrusionOpacity?: SafeReadResult<unknown>;
    fillExtrusionBase?: SafeReadResult<unknown>;
    sourceFeatureCount?: SafeReadResult<number>;
    renderedFeatureCount?: SafeReadResult<number>;
    firstFeatures?: SafeReadResult<Array<Record<string, unknown>>>;
}

interface ProductionExtrusionPaint {
    color: unknown;
    height: unknown;
    opacity: unknown;
    base: number;
}

interface BuildingMapDebugPanelProps {
    /** Ref üzerinden değil fonksiyon üzerinden okunur — panel kendi render'ında map instance'ını
     *  cache'lemez, her tıklamada GÜNCEL `mapRef.current`'ı okur (stale closure riski yok). */
    getMap: () => MaplibreMap | null;
    buildingSourceId: string;
    buildingLayerId: string;
    /** "Debug Extrusion Kapat" tıklanınca geri yüklenecek gerçek production paint değerleri. */
    productionPaint: ProductionExtrusionPaint;
    /** Pendik'in tamamına genelleştirmede hangi mahallenin test edildiğini teşhis çıktısında
     *  görünür kılmak için — React state'inden gelir, MapLibre'den okunmaz. */
    selectedNeighborhoodId?: string | null;
    selectedNeighborhoodName?: string | null;
    /** API'den gelen ham feature sayısı ile MapLibre source'a gerçekten `setData` ile yazılan
     *  sayının karşılaştırılması — "API=500, setData=500, querySource=0" gibi zincirleri ayırt
     *  etmek için (bkz. BuildingMap.tsx'teki debug talimatı). */
    apiFeatureCount?: number | null;
    lastSetDataFeatureCount?: number | null;
}

/** DEV-only 3B render teşhis paneli — `BuildingMap.tsx` yalnızca `import.meta.env.DEV` iken bunu
 *  mount eder (bkz. orada production build doğrulaması). Amaç: "Değerleri Oku"ya basıldığında,
 *  MapLibre çağrılarından biri exception atsa BİLE, kullanıcı HER ZAMAN okunabilir bir sonuç görsün
 *  — sessiz failure kabul edilmez (canlı testte "hiçbir çıktı görünmüyor" olarak bulunan bug'ın
 *  kök nedeni: tek bir `getPaintProperty`/`getLayoutProperty` çağrısı patlarsa tüm snapshot
 *  objesinin inşası state'e hiç ulaşamadan çöküyordu). */
export const BuildingMapDebugPanel: React.FC<BuildingMapDebugPanelProps> = ({
    getMap,
    buildingSourceId,
    buildingLayerId,
    productionPaint,
    selectedNeighborhoodId = null,
    selectedNeighborhoodName = null,
    apiFeatureCount = null,
    lastSetDataFeatureCount = null,
}) => {
    const [open, setOpen] = useState(false);
    const [result, setResult] = useState<DebugResult | null>(null);
    const [forced, setForced] = useState(false);
    const [actionMessage, setActionMessage] = useState<string | null>(null);

    const readValues = () => {
        console.log('[3B DEBUG] read clicked');
        setResult({ status: 'reading', mapExists: false });

        try {
            const map = getMap();
            if (!map) {
                console.warn('[3B DEBUG] map instance missing');
                setResult({ status: 'no-map', mapExists: false });
                return;
            }

            const buildingSourceExists = safeRead(() => Boolean(map.getSource(buildingSourceId)));
            const buildingLayerExists = safeRead(() => Boolean(map.getLayer(buildingLayerId)));

            const sourceFeaturesResult = isOk(buildingSourceExists) && buildingSourceExists
                ? safeRead(() => map.querySourceFeatures(buildingSourceId))
                : ([] as GeoJSON.Feature[]);
            const renderedFeatureCount = isOk(buildingLayerExists) && buildingLayerExists
                ? safeRead(() => map.queryRenderedFeatures({ layers: [buildingLayerId] }).length)
                : 0;

            const firstFeatures = safeRead(() =>
                (isOk(sourceFeaturesResult) ? sourceFeaturesResult : []).slice(0, 3).map((f) => {
                    const props = (f.properties ?? {}) as Record<string, unknown>;
                    return {
                        geometryType: f.geometry?.type ?? null,
                        featureId: f.id ?? null,
                        propId: props.id ?? null,
                        estimatedHeightM: props.estimatedHeightM ?? null,
                        renderHeight: props.renderHeight ?? null,
                        height: props.height ?? null,
                        levels: props.levels ?? null,
                        heightSource: props.heightSource ?? null,
                        damageStatus: props.damageStatus ?? null,
                    };
                }),
            );

            setResult({
                status: 'ok',
                mapExists: true,
                selectedNeighborhoodId,
                selectedNeighborhoodName,
                apiFeatureCount,
                lastSetDataFeatureCount,
                zoom: safeRead(() => map.getZoom()),
                pitch: safeRead(() => map.getPitch()),
                bearing: safeRead(() => map.getBearing()),
                buildingSourceExists,
                buildingLayerExists,
                visibility: safeRead(() => map.getLayoutProperty(buildingLayerId, 'visibility')),
                fillExtrusionHeight: safeRead(() => map.getPaintProperty(buildingLayerId, 'fill-extrusion-height')),
                fillExtrusionColor: safeRead(() => map.getPaintProperty(buildingLayerId, 'fill-extrusion-color')),
                fillExtrusionOpacity: safeRead(() => map.getPaintProperty(buildingLayerId, 'fill-extrusion-opacity')),
                fillExtrusionBase: safeRead(() => map.getPaintProperty(buildingLayerId, 'fill-extrusion-base')),
                sourceFeatureCount: isOk(sourceFeaturesResult) ? sourceFeaturesResult.length : sourceFeaturesResult,
                renderedFeatureCount,
                firstFeatures,
            });
        } catch (error) {
            // Beklenmedik (yukarıdaki bağımsız safeRead'lerin dışında kalan) bir hata — yine de
            // kullanıcı boş panel görmesin.
            setResult({
                status: 'error',
                mapExists: !!getMap(),
                error: error instanceof Error ? error.message : String(error),
            });
        }
    };

    const toggleForcedExtrusion = () => {
        const map = getMap();
        if (!map) { setActionMessage('Map instance hazır değil.'); return; }
        if (!map.getLayer(buildingLayerId)) { setActionMessage('Building extrusion layer bulunamadı.'); return; }
        setActionMessage(null);
        const next = !forced;
        if (next) {
            map.setLayerZoomRange(buildingLayerId, 0, 24);
            map.setPaintProperty(buildingLayerId, 'fill-extrusion-color', '#ff00ff');
            map.setPaintProperty(buildingLayerId, 'fill-extrusion-height', 40 as never);
            map.setPaintProperty(buildingLayerId, 'fill-extrusion-base', 0);
            map.setPaintProperty(buildingLayerId, 'fill-extrusion-opacity', 1);
        } else {
            map.setPaintProperty(buildingLayerId, 'fill-extrusion-color', productionPaint.color as never);
            map.setPaintProperty(buildingLayerId, 'fill-extrusion-height', productionPaint.height as never);
            map.setPaintProperty(buildingLayerId, 'fill-extrusion-base', productionPaint.base);
            map.setPaintProperty(buildingLayerId, 'fill-extrusion-opacity', productionPaint.opacity as never);
        }
        setForced(next);
    };

    const forceCamera = () => {
        const map = getMap();
        if (!map) { setActionMessage('Map instance hazır değil.'); return; }
        setActionMessage(null);
        map.jumpTo({ pitch: 55, bearing: -15 });
    };

    return (
        <div className="absolute bottom-2 left-1/2 -translate-x-1/2 z-20 w-[calc(100%-16px)] max-w-lg">
            <button
                type="button"
                onClick={() => setOpen((v) => !v)}
                className="mx-auto block bg-black/80 text-lime-300 text-[10px] font-mono px-2 py-1 rounded-t"
            >
                🐞 3B Debug {open ? '▲' : '▼'}
            </button>
            {open && (
                <div className="bg-black/90 text-lime-300 font-mono text-[10px] p-2 rounded-b">
                    <div className="flex flex-wrap gap-1.5 mb-2">
                        <button
                            type="button"
                            onClick={readValues}
                            className="bg-lime-700/40 hover:bg-lime-700/60 text-lime-100 px-2 py-1 rounded"
                        >
                            Değerleri Oku
                        </button>
                        <button
                            type="button"
                            onClick={toggleForcedExtrusion}
                            className={`px-2 py-1 rounded ${forced ? 'bg-fuchsia-700/60 hover:bg-fuchsia-700/80 text-fuchsia-50' : 'bg-lime-700/40 hover:bg-lime-700/60 text-lime-100'}`}
                        >
                            {forced ? 'Debug Extrusion Kapat' : 'Debug Extrusion Aç (magenta/40m)'}
                        </button>
                        <button
                            type="button"
                            onClick={forceCamera}
                            className="bg-lime-700/40 hover:bg-lime-700/60 text-lime-100 px-2 py-1 rounded"
                        >
                            Kamerayı Zorla (55°/-15°)
                        </button>
                    </div>

                    {actionMessage && <p className="text-amber-400 mb-2">{actionMessage}</p>}

                    {!result && <p className="text-lime-500">"Değerleri Oku"ya basın.</p>}
                    {result?.status === 'reading' && <p className="text-lime-500">Değerler okunuyor...</p>}
                    {result?.status === 'no-map' && <p className="text-amber-400">Map instance hazır değil.</p>}
                    {(result?.status === 'ok' || result?.status === 'error') && (
                        <pre className="max-h-80 overflow-auto whitespace-pre-wrap break-all text-[10px]">
                            {JSON.stringify(result, null, 2)}
                        </pre>
                    )}
                </div>
            )}
        </div>
    );
};
