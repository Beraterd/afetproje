import { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
    Map as MaplibreMap,
    NavigationControl,
    AttributionControl,
    type LngLatBoundsLike,
    type MapLayerMouseEvent,
    type GeoJSONSource,
} from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { RotateCcw } from 'lucide-react';
import { getBuildings } from '@/api/buildings.api';
import { BuildingFeatureCollection } from '@/types/building';
import {
    ISTANBUL_FALLBACK_CENTER,
    MAP3D_CAMERA,
    NEIGHBORHOOD_BOUNDARY_COLOR,
    NEIGHBORHOOD_BOUNDARY_FILL_OPACITY,
    NEIGHBORHOOD_BOUNDARY_LINE_WIDTH,
} from '@/config/map3d';
import { usePrefersReducedMotion } from '@/hooks/usePrefersReducedMotion';
import { queryKeys } from '@/utils/queryKeys';
import type { BuildingColorMode } from './BuildingColorModeControl';
import { buildFillColorExpression, buildFillOpacityExpression, buildHeightExpression } from './buildingColorExpressions';
import { BuildingMapDebugPanel } from './BuildingMapDebugPanel';

// Mevcut 2B haritayla (OperationMap.tsx) aynı basemap kaynağı — tutarlı görünüm ve
// zaten doğrulanmış attribution için.
const TILE_URL_TEMPLATE =
    import.meta.env.VITE_MAP_TILE_URL || 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png';
const TILE_ATTRIBUTION =
    import.meta.env.VITE_MAP_ATTRIBUTION || '&copy; OpenStreetMap contributors &copy; CARTO';

const BUILDINGS_SOURCE_ID = 'buildings';
const BUILDINGS_FILL_LAYER_ID = 'buildings-fill';
const SELECTED_SOURCE_ID = 'buildings-selected';
const SELECTED_LAYER_ID = 'buildings-selected-outline';
const BOUNDARY_SOURCE_ID = 'neighborhood-boundary';
const BOUNDARY_FILL_LAYER_ID = 'neighborhood-boundary-fill';
const BOUNDARY_LINE_LAYER_ID = 'neighborhood-boundary-line';
// Sokak arama seçimi vurgusu — bina anahat rengi (#2563eb, mavi) ve mahalle sınırı rengi
// (#0f766e, teal) ile karışmasın diye ayrı, belirgin bir ton (item 24).
const STREET_HIGHLIGHT_SOURCE_ID = 'street-highlight';
const STREET_HIGHLIGHT_LAYER_ID = 'street-highlight-line';
const STREET_HIGHLIGHT_COLOR = '#d97706';

const EMPTY_COLLECTION: BuildingFeatureCollection = { type: 'FeatureCollection', features: [] };

export interface FlyToTarget {
    lat: number;
    lon: number;
    /** Aynı binaya ikinci kez uçuş isteğini de tetiklemek için artan sayaç. */
    token: number;
}

export interface StreetFlyToTarget {
    /** [minLon, minLat, maxLon, maxLat] — seçili mahalleye zaten clip edilmiş sokak segmenti. */
    bounds: [number, number, number, number];
    center: { lon: number; lat: number };
    /** Aynı sokağa ikinci kez uçuş isteğini de tetiklemek için artan sayaç. */
    token: number;
}

export type BuildingMapMode = 'operations' | 'selector';

interface HoverInfo {
    x: number;
    y: number;
    label: string;
    sub: string;
}

interface BuildingMapProps {
    mode: BuildingMapMode;
    districtId: string;
    /** Verilirse bbox sorgusu bu mahalleyle sınırlanır (performans + "yalnız bu mahalledeki
     *  binalar" davranışı için) — Operasyon Haritası'nda mahalle seçildiğinde kullanılır. */
    neighborhoodId?: string;
    /** Yalnızca DEV teşhis paneli için — üretim mantığında kullanılmaz. */
    neighborhoodName?: string;
    /** İlk kamera konumu için bbox — [minLon, minLat, maxLon, maxLat]. */
    initialBounds: [number, number, number, number] | null;
    buildingMinZoom: number;
    extrusionMinZoom: number;
    /** Seçili binanın anahat vurgusu için geometrisi (yalnızca görsel amaçlı — id state'i üst bileşende tutulur). */
    selectedBuildingGeometry: GeoJSON.Geometry | null;
    onSelectBuilding: (buildingId: string | null) => void;
    flyTo: FlyToTarget | null;
    /** `mode="operations"` için bina renklendirme modu — "selector" modunda yok sayılır. */
    colorMode?: BuildingColorMode;
    /** Seçili mahallenin sınır geometrisi — belirgin outline + hafif dolgu olarak çizilir. */
    neighborhoodBoundary?: GeoJSON.Geometry | null;
    /** Arama sonucundan seçilen sokağın anahat vurgusu için geometrisi (LineString/
     *  MultiLineString) — yalnızca görsel amaçlı, id state'i üst bileşende tutulur. */
    selectedStreetGeometry?: GeoJSON.Geometry | null;
    /** Sokak arama sonucundan uçuş — mevcut bina flyTo'sundan farklı olarak pitch/bearing'i
     *  ZORLAMAZ (mevcut kamera açısını korur, item 27/28), bounds tabanlıdır (item 21/23). */
    streetFlyTo?: StreetFlyToTarget | null;
}

/** 3B bina katmanı — kendi viewport (bbox) sorgusunu kendi yönetir (bkz.
 *  CoordinationCenterPage'in DistrictSection'ı gibi, disiplinli bir "self-contained" istisna):
 *  bbox/zoom, maplibre kamerasının imperatif state'inden gelir, React state'e her hareket
 *  karesinde yansıtılıp üst bileşene prop olarak geri basmak gereksiz re-render üretir.
 *
 *  İki kullanım modu paylaşır: Operasyon Haritası (mode="operations" — hasar durumuna göre
 *  renklendirme) ve Hasar Tespiti bina seçici (mode="selector" — nötr renk, sade). İkisi de
 *  aynı bbox/zoom/tıklama mekaniğini kullanır; farkları yalnızca boyama ve üst bileşenin
 *  tıklamaya nasıl tepki verdiğidir (detay paneli vs. onay kartı). */
export const BuildingMap: React.FC<BuildingMapProps> = ({
    mode,
    districtId,
    neighborhoodId,
    neighborhoodName,
    initialBounds,
    buildingMinZoom,
    extrusionMinZoom,
    selectedBuildingGeometry,
    onSelectBuilding,
    flyTo,
    colorMode = 'damage',
    neighborhoodBoundary = null,
    selectedStreetGeometry = null,
    streetFlyTo = null,
}) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const mapRef = useRef<MaplibreMap | null>(null);
    const loadedRef = useRef(false);

    const [viewport, setViewport] = useState<{ bbox: string; zoom: number } | null>(null);
    const [isTopDown, setIsTopDown] = useState(false);
    const [hoverInfo, setHoverInfo] = useState<HoverInfo | null>(null);
    // Yalnızca DEV teşhis paneli için — üretim mantığında kullanılmaz.
    const [lastSetDataFeatureCount, setLastSetDataFeatureCount] = useState<number | null>(null);

    const prefersReducedMotion = usePrefersReducedMotion();
    // Efekt/handler closure'ları mount'ta sabitlendiği için güncel değeri ref üzerinden okur.
    const reducedMotionRef = useRef(prefersReducedMotion);
    reducedMotionRef.current = prefersReducedMotion;

    const buildingsQuery = useQuery({
        queryKey: queryKeys.buildings.bbox(districtId, neighborhoodId, viewport?.bbox, viewport ? Math.floor(viewport.zoom) : null),
        queryFn: ({ signal }) => getBuildings({ districtId, neighborhoodId, bbox: viewport!.bbox, zoom: Math.floor(viewport!.zoom) }, signal),
        enabled: !!viewport && viewport.zoom >= buildingMinZoom,
        // Aynı mahalle içinde pan/zoom yaparken önceki sonucu göstermeye devam eder (flicker
        // olmasın diye) — mahalleler arası sızıntı riski yok, çünkü MapPage artık BuildingMap'i
        // `key={neighborhoodId}` ile her mahalle değişiminde TAMAMEN yeniden mount ediyor (bu
        // query da dahil sıfırdan kuruluyor), placeholderData önceki mahalleden asla gelmiyor.
        placeholderData: (prev) => prev,
    });

    // ── Harita kurulumu — yalnızca mount'ta bir kez ──
    useEffect(() => {
        if (!containerRef.current) return;

        const map = new MaplibreMap({
            container: containerRef.current,
            style: {
                version: 8,
                sources: {
                    'carto-basemap': {
                        type: 'raster',
                        tiles: ['a', 'b', 'c', 'd'].map((s) => TILE_URL_TEMPLATE.replace('{s}', s)),
                        tileSize: 256,
                        attribution: TILE_ATTRIBUTION,
                    },
                },
                layers: [{ id: 'carto-basemap-layer', type: 'raster', source: 'carto-basemap' }],
            },
            pitch: MAP3D_CAMERA.pitch,
            bearing: MAP3D_CAMERA.bearing,
            attributionControl: false,
        });
        mapRef.current = map;

        if (initialBounds) {
            // maxZoom: çok küçük mahallelerde fitBounds aşırı yakınlaşıp bağlamı kaybettirebilir —
            // MapLibre'nin native FitBoundsOptions.maxZoom'u bunu tek çağrıda sınırlar.
            map.fitBounds(initialBounds as LngLatBoundsLike, { padding: 40, duration: 0, maxZoom: MAP3D_CAMERA.maxInitialZoom });
            // fitBounds bir mahalleyi ekrana sığdırırken hesapladığı zoom extrusionMinZoom eşiğinin
            // altında kalabilir — bu durumda 'fill-extrusion-height' step expression'ı 0 döner ve
            // binalar tamamen düz/görünmez kalır (canlı Kurtköy verisinde doğrulanan gerçek bug).
            // Küçük/orta mahallelerde (≤1 zoom seviyesi fark) kamerayı sessizce eşiğe tamamlıyoruz —
            // kullanıcı context kaybetmeden binaları hemen görür. GERÇEKTEN geniş mahallelerde
            // (Şile/Beykoz gibi, >1 seviye fark) zorla yakınlaştırmak kullanıcıyı mahallenin
            // anlamsız bir köşesine sıçratır — bunun yerine doğal görünümde bırakılır, "yakınlaşın"
            // ipucu (bkz. showZoomHint / emitViewport) gösterilir.
            if (extrusionMinZoom - map.getZoom() > 0 && extrusionMinZoom - map.getZoom() <= 1) {
                map.setZoom(extrusionMinZoom);
            }
        } else {
            map.setCenter(ISTANBUL_FALLBACK_CENTER);
            map.setZoom(Math.max(16, extrusionMinZoom));
        }

        // maplibre-gl kaynak/worker hatalarını exception olarak fırlatmaz, 'error' event'i
        // yayınlar — sessizce yutulmasın diye console'a bağlıyoruz.
        map.on('error', (e) => {
            console.error('[BuildingMap] maplibre error:', e.error ?? e);
        });

        map.addControl(new NavigationControl({ visualizePitch: true }), 'top-right');
        map.addControl(new AttributionControl({ compact: true }), 'bottom-right');

        const emitViewport = () => {
            const b = map.getBounds();
            const bbox = `${b.getWest()},${b.getSouth()},${b.getEast()},${b.getNorth()}`;
            setViewport({ bbox, zoom: map.getZoom() });
        };

        map.on('load', () => {
            loadedRef.current = true;

            // Mahalle sınırı — dolgu (zemin tonu) extrusion layer'ından ÖNCE eklenir ki binaların
            // altında kalsın. `neighborhoodBoundary` mount anında zaten dolu olabilir (kullanıcı
            // mahalleye tıkladığında BuildingMap ilk kez bu prop'la birlikte mount olur) — bu yüzden
            // kaynağı mount closure'ındaki güncel değerle SEED ediyoruz; yalnızca EMPTY_COLLECTION
            // ile başlatıp `neighborhoodBoundary` değişikliğini bekleyen bir efekte güvenmek, prop
            // hiç "değişmediği" için (mount'ta zaten o değerle geliyor) o efektin load sonrası asla
            // tetiklenmemesine — yani sınırın hiç çizilmemesine — yol açan gerçek bir bugdı.
            map.addSource(BOUNDARY_SOURCE_ID, {
                type: 'geojson',
                data: (neighborhoodBoundary
                    ? { type: 'Feature', geometry: neighborhoodBoundary, properties: {} }
                    : EMPTY_COLLECTION) as GeoJSON.Feature | GeoJSON.FeatureCollection,
            });
            map.addLayer({
                id: BOUNDARY_FILL_LAYER_ID,
                type: 'fill',
                source: BOUNDARY_SOURCE_ID,
                paint: { 'fill-color': NEIGHBORHOOD_BOUNDARY_COLOR, 'fill-opacity': NEIGHBORHOOD_BOUNDARY_FILL_OPACITY },
            });

            map.addSource(BUILDINGS_SOURCE_ID, { type: 'geojson', data: EMPTY_COLLECTION });
            // MapLibre geçersiz bir paint expression'ı (ör. desteklenmeyen bir data-driven değer)
            // için addLayer'ı SESSİZCE reddedebilir (throw etmeden 'error' event'i yayınlar) — bu
            // yüzden ekleme burada açıkça doğrulanır, canlı Kurtköy testinde bulunan
            // "buildingLayerExists: false" bug'ının bir daha fark edilmeden geçmemesi için.
            try {
                map.addLayer({
                    id: BUILDINGS_FILL_LAYER_ID,
                    type: 'fill-extrusion',
                    source: BUILDINGS_SOURCE_ID,
                    paint: {
                        'fill-extrusion-height': buildHeightExpression(extrusionMinZoom) as never,
                        'fill-extrusion-base': 0,
                        'fill-extrusion-opacity': buildFillOpacityExpression(mode, colorMode) as never,
                        'fill-extrusion-color': buildFillColorExpression(mode, colorMode) as never,
                    },
                });
                if (import.meta.env.DEV) {
                    console.log('[3B] building layer added:', Boolean(map.getLayer(BUILDINGS_FILL_LAYER_ID)));
                }
            } catch (error) {
                console.error('[3B] addLayer(buildings-fill) failed:', error);
            }

            // Sınır anahattı extrusion layer'ından SONRA eklenir — MapLibre'de line/fill layer'lar
            // fill-extrusion'ın derinlik tamponuna katılmaz, yani extrusion'dan ÖNCE eklenen bir line
            // layer uzun binaların altında/arkasında görsel olarak kaybolabilir. Sonradan eklemek
            // anahattın binalardan bağımsız her zaman görünür kalmasını garanti eder.
            map.addLayer({
                id: BOUNDARY_LINE_LAYER_ID,
                type: 'line',
                source: BOUNDARY_SOURCE_ID,
                paint: { 'line-color': NEIGHBORHOOD_BOUNDARY_COLOR, 'line-width': NEIGHBORHOOD_BOUNDARY_LINE_WIDTH },
            });

            map.addSource(SELECTED_SOURCE_ID, { type: 'geojson', data: EMPTY_COLLECTION });
            map.addLayer({
                id: SELECTED_LAYER_ID,
                type: 'line',
                source: SELECTED_SOURCE_ID,
                paint: { 'line-color': '#2563eb', 'line-width': 3.5 },
            });

            // Sokak vurgusu — seçili bina anahattıyla aynı desende ayrı source/layer (bkz. o
            // layer'ın yorumu), extrusion'dan SONRA eklenir ki uzun binaların arkasında
            // kaybolmasın (aynı derinlik-tamponu gerekçesi, bkz. BOUNDARY_LINE_LAYER_ID yorumu).
            map.addSource(STREET_HIGHLIGHT_SOURCE_ID, { type: 'geojson', data: EMPTY_COLLECTION });
            map.addLayer({
                id: STREET_HIGHLIGHT_LAYER_ID,
                type: 'line',
                source: STREET_HIGHLIGHT_SOURCE_ID,
                paint: { 'line-color': STREET_HIGHLIGHT_COLOR, 'line-width': 5, 'line-opacity': 0.85 },
                layout: { 'line-cap': 'round', 'line-join': 'round' },
            });

            map.on('click', BUILDINGS_FILL_LAYER_ID, (e: MapLayerMouseEvent) => {
                const feature = e.features?.[0];
                const id = feature?.properties?.id as string | undefined;
                onSelectBuilding(id ?? null);
            });
            map.on('mouseenter', BUILDINGS_FILL_LAYER_ID, () => {
                map.getCanvas().style.cursor = 'pointer';
            });
            map.on('mousemove', BUILDINGS_FILL_LAYER_ID, (e: MapLayerMouseEvent) => {
                if (mode !== 'operations') return;
                const feature = e.features?.[0];
                if (!feature) return;
                const props = (feature.properties ?? {}) as Record<string, unknown>;
                const addressParts = [props.streetName, props.buildingNumber].filter(Boolean).join(' ');
                const label = addressParts || (props.name as string) || 'Bina';
                const sub = (props.damageStatusLabel as string) || 'Rapor yok';
                setHoverInfo({ x: e.point.x, y: e.point.y, label, sub });
            });
            map.on('mouseleave', BUILDINGS_FILL_LAYER_ID, () => {
                map.getCanvas().style.cursor = '';
                setHoverInfo(null);
            });

            emitViewport();
        });

        map.on('moveend', emitViewport);

        return () => {
            map.remove();
            mapRef.current = null;
            loadedRef.current = false;
        };
        // Kamera/katman kurulumu yalnızca mount'ta yapılır; mode/districtId/zoom eşikleri
        // pratikte component ömrü boyunca sabittir.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // ── Bina verisi güncellendiğinde source'u tazele ──
    useEffect(() => {
        const map = mapRef.current;
        if (!map || !loadedRef.current) return;
        const source = map.getSource(BUILDINGS_SOURCE_ID) as GeoJSONSource | undefined;
        const data = (buildingsQuery.data ?? EMPTY_COLLECTION) as GeoJSON.FeatureCollection;
        source?.setData(data);
        if (import.meta.env.DEV) setLastSetDataFeatureCount(data.features.length);
        // Yalnızca DEV'de, veri güncellendiğinde (her hareket karesinde DEĞİL) teşhis logu.
        if (import.meta.env.DEV && buildingsQuery.data) {
            console.debug(
                '[BuildingMap] building features loaded:', buildingsQuery.data.features.length,
                'zoom:', map.getZoom().toFixed(2),
                'pitch:', map.getPitch().toFixed(1),
            );
        }
    }, [buildingsQuery.data]);

    // ── Seçili bina anahat vurgusu ──
    useEffect(() => {
        const map = mapRef.current;
        if (!map || !loadedRef.current) return;
        const source = map.getSource(SELECTED_SOURCE_ID) as GeoJSONSource | undefined;
        if (!source) return;
        if (selectedBuildingGeometry) {
            source.setData({
                type: 'Feature',
                geometry: selectedBuildingGeometry,
                properties: {},
            } as GeoJSON.Feature);
        } else {
            source.setData(EMPTY_COLLECTION as unknown as GeoJSON.FeatureCollection);
        }
    }, [selectedBuildingGeometry]);

    // ── Seçili sokak anahat vurgusu ──
    useEffect(() => {
        const map = mapRef.current;
        if (!map || !loadedRef.current) return;
        const source = map.getSource(STREET_HIGHLIGHT_SOURCE_ID) as GeoJSONSource | undefined;
        if (!source) return;
        if (selectedStreetGeometry) {
            source.setData({
                type: 'Feature',
                geometry: selectedStreetGeometry,
                properties: {},
            } as GeoJSON.Feature);
        } else {
            source.setData(EMPTY_COLLECTION as unknown as GeoJSON.FeatureCollection);
        }
    }, [selectedStreetGeometry]);

    // ── Mahalle sınırı ──
    useEffect(() => {
        const map = mapRef.current;
        if (!map || !loadedRef.current) return;
        const source = map.getSource(BOUNDARY_SOURCE_ID) as GeoJSONSource | undefined;
        if (!source) return;
        if (neighborhoodBoundary) {
            source.setData({
                type: 'Feature',
                geometry: neighborhoodBoundary,
                properties: {},
            } as GeoJSON.Feature);
        } else {
            source.setData(EMPTY_COLLECTION as unknown as GeoJSON.FeatureCollection);
        }
    }, [neighborhoodBoundary]);

    // ── Renklendirme modu değişimi ──
    useEffect(() => {
        const map = mapRef.current;
        if (!map || !loadedRef.current || !map.getLayer(BUILDINGS_FILL_LAYER_ID)) return;
        map.setPaintProperty(BUILDINGS_FILL_LAYER_ID, 'fill-extrusion-color', buildFillColorExpression(mode, colorMode) as never);
        map.setPaintProperty(BUILDINGS_FILL_LAYER_ID, 'fill-extrusion-opacity', buildFillOpacityExpression(mode, colorMode) as never);
    }, [mode, colorMode]);

    // ── Arama sonucundan / seçimden uçuş ──
    useEffect(() => {
        const map = mapRef.current;
        if (!map || !flyTo) return;
        map.flyTo({
            center: [flyTo.lon, flyTo.lat],
            zoom: Math.max(map.getZoom(), extrusionMinZoom + 0.5),
            pitch: MAP3D_CAMERA.pitch,
            duration: reducedMotionRef.current ? 0 : MAP3D_CAMERA.flyDurationMs,
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [flyTo?.token]);

    // ── Sokak arama sonucundan uçuş — item 21-23,27,28: bounds tabanlı (uzun caddede sadece
    // mahalleye clip'li segment kadar zoom-out yapar), mevcut pitch/bearing KORUNUR (2B/3B
    // hangi moddaysa o modda kalır — bina flyTo'sundaki gibi 3B pitch'e zorlamaz). ──
    useEffect(() => {
        const map = mapRef.current;
        if (!map || !streetFlyTo) return;
        const duration = reducedMotionRef.current ? 0 : MAP3D_CAMERA.flyDurationMs;
        const [minLon, minLat, maxLon, maxLat] = streetFlyTo.bounds;
        const diagonal = Math.hypot(maxLon - minLon, maxLat - minLat);
        // Çok kısa sokak segmenti (item 22): fitBounds neredeyse-nokta bir dikdörtgende anlamsız/
        // aşırı yakınlaşabilir — merkez + sabit, okunabilir zoom kullan.
        const MIN_MEANINGFUL_DIAGONAL_DEG = 0.0004; // ~40m
        if (diagonal < MIN_MEANINGFUL_DIAGONAL_DEG) {
            map.flyTo({
                center: [streetFlyTo.center.lon, streetFlyTo.center.lat],
                zoom: Math.max(map.getZoom(), extrusionMinZoom + 2),
                pitch: map.getPitch(),
                bearing: map.getBearing(),
                duration,
            });
        } else {
            map.fitBounds(
                [[minLon, minLat], [maxLon, maxLat]] as LngLatBoundsLike,
                { padding: 60, maxZoom: 19, pitch: map.getPitch(), bearing: map.getBearing(), duration },
            );
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [streetFlyTo?.token]);

    const zoomTooLow = !!viewport && viewport.zoom < buildingMinZoom;

    const setFlatMode = (flat: boolean) => {
        const map = mapRef.current;
        if (!map || flat === isTopDown) return;
        map.easeTo({
            pitch: flat ? MAP3D_CAMERA.flatPitch : MAP3D_CAMERA.pitch,
            bearing: flat ? MAP3D_CAMERA.flatBearing : MAP3D_CAMERA.bearing,
            duration: reducedMotionRef.current ? 0 : MAP3D_CAMERA.easeDurationMs,
        });
        setIsTopDown(flat);
    };

    const resetView = () => {
        const map = mapRef.current;
        if (!map) return;
        const duration = reducedMotionRef.current ? 0 : MAP3D_CAMERA.easeDurationMs;
        if (initialBounds) {
            map.fitBounds(initialBounds as LngLatBoundsLike, {
                padding: 40,
                pitch: MAP3D_CAMERA.pitch,
                bearing: MAP3D_CAMERA.bearing,
                duration,
                maxZoom: MAP3D_CAMERA.maxInitialZoom,
            });
            // Animasyonlu (veya anlık) geçiş bittiğinde zoom yine de extrusionMinZoom altında
            // kalmışsa düzelt — mount'taki aynı ≤1-seviye kuralı burada da geçerli (bkz. yukarıdaki
            // kurulum effect'i): küçük farkı tamamla, gerçekten geniş mahallede zorlama.
            map.once('moveend', () => {
                const gap = extrusionMinZoom - map.getZoom();
                if (gap > 0 && gap <= 1) map.setZoom(extrusionMinZoom);
            });
        } else {
            map.easeTo({
                center: ISTANBUL_FALLBACK_CENTER,
                zoom: 16,
                pitch: MAP3D_CAMERA.pitch,
                bearing: MAP3D_CAMERA.bearing,
                duration,
            });
        }
        setIsTopDown(false);
    };

    return (
        <div className="relative w-full h-full">
            <div ref={containerRef} className="w-full h-full" />

            <div className="absolute top-[92px] right-2.5 z-10 flex flex-col gap-1.5">
                {/* 2B/3B: mevcut modun ne olduğu ve tıklanınca ne olacağı ayrı ayrı belirsizdi
                    (tek buton hem durumu hem eylemi aynı anda göstermeye çalışıyordu) — iki seçenekli
                    segmented control, aktif seçenek belirgin şekilde vurgulanır. */}
                <div
                    role="group"
                    aria-label="Kamera görünümü"
                    className="flex bg-white border border-slate-300 rounded-[4px] shadow-sm overflow-hidden"
                >
                    <button
                        type="button"
                        onClick={() => setFlatMode(true)}
                        aria-pressed={isTopDown}
                        title="2B (üstten) görünüme geç"
                        aria-label="2B görünüme geç"
                        className={`w-[29px] h-[29px] flex items-center justify-center text-[10px] font-semibold transition-colors ${
                            isTopDown ? 'bg-blue-600 text-white' : 'text-slate-700 hover:bg-slate-50'
                        }`}
                    >
                        2B
                    </button>
                    <button
                        type="button"
                        onClick={() => setFlatMode(false)}
                        aria-pressed={!isTopDown}
                        title="3B görünüme geç"
                        aria-label="3B görünüme geç"
                        className={`w-[29px] h-[29px] flex items-center justify-center text-[10px] font-semibold border-l border-slate-300 transition-colors ${
                            !isTopDown ? 'bg-blue-600 text-white' : 'text-slate-700 hover:bg-slate-50'
                        }`}
                    >
                        3B
                    </button>
                </div>
                <button
                    type="button"
                    onClick={resetView}
                    className="w-[29px] h-[29px] bg-white hover:bg-slate-50 border border-slate-300 rounded-[4px] shadow-sm flex items-center justify-center text-slate-700"
                    title="Görünümü Sıfırla"
                    aria-label="Görünümü Sıfırla"
                >
                    <RotateCcw size={14} />
                </button>
            </div>

            {mode === 'operations' && hoverInfo && (
                <div
                    className="absolute z-10 pointer-events-none bg-slate-900/90 text-white text-[11px] px-2.5 py-1.5 rounded-md shadow-lg max-w-[200px]"
                    style={{ left: hoverInfo.x + 12, top: hoverInfo.y + 12 }}
                >
                    <div className="font-medium leading-tight">{hoverInfo.label}</div>
                    <div className="text-slate-300 leading-tight">{hoverInfo.sub}</div>
                </div>
            )}

            {zoomTooLow && (
                <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-10 bg-slate-900/85 text-white text-xs px-3 py-1.5 rounded-full pointer-events-none">
                    Binaları görmek için yakınlaştırın
                </div>
            )}
            {!zoomTooLow && buildingsQuery.isFetching && (
                <div className="absolute bottom-4 right-4 z-10 bg-white/90 text-slate-600 text-xs px-2.5 py-1 rounded-full shadow border border-slate-200 pointer-events-none">
                    Binalar yükleniyor…
                </div>
            )}
            {!zoomTooLow && buildingsQuery.isError && (
                <div className="absolute bottom-4 right-4 z-10 bg-red-50 text-red-700 text-xs px-2.5 py-1 rounded-full shadow border border-red-200 pointer-events-none">
                    Binalar yüklenemedi
                </div>
            )}
            {!zoomTooLow && buildingsQuery.isSuccess && buildingsQuery.data.features.length === 0 && !buildingsQuery.isFetching && (
                <div className="absolute bottom-4 right-4 z-10 bg-white/90 text-slate-600 text-xs px-2.5 py-1 rounded-full shadow border border-slate-200 pointer-events-none">
                    Bu bölgede bina verisi bulunamadı
                </div>
            )}

            {import.meta.env.DEV && (
                <BuildingMapDebugPanel
                    getMap={() => mapRef.current}
                    buildingSourceId={BUILDINGS_SOURCE_ID}
                    buildingLayerId={BUILDINGS_FILL_LAYER_ID}
                    productionPaint={{
                        color: buildFillColorExpression(mode, colorMode),
                        height: buildHeightExpression(extrusionMinZoom),
                        opacity: buildFillOpacityExpression(mode, colorMode),
                        base: 0,
                    }}
                    selectedNeighborhoodId={neighborhoodId ?? null}
                    selectedNeighborhoodName={neighborhoodName ?? null}
                    apiFeatureCount={buildingsQuery.data?.features.length ?? null}
                    lastSetDataFeatureCount={lastSetDataFeatureCount}
                />
            )}
        </div>
    );
};
