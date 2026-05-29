import React, { useEffect, useState } from 'react';
import { GeoJSON, MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { useQuery } from '@tanstack/react-query';
import {
    getMapDistricts,
    getMapNeighborhoods,
    getMapDamagePoints,
    getDistrictCentersForMap,
    getNeighborhoodCentersForMap,
} from '@/api/map.api';
import { queryKeys } from '@/utils/queryKeys';
import { LoadingSpinner } from '@/components/ui';
import { MapDistrictResponse, MapNeighborhoodResponse, DistrictCoordinatorMapResponse, NeighborhoodCoordinatorMapResponse } from '@/types';
import { DamagePointResponse } from '@/types/damage';
import { Layers } from 'lucide-react';

// Fix leaflet default icon issue in React
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// ─── Color tables ────────────────────────────────────────────────────────────
const RiskColors: Record<string, string> = {
    GREEN:  '#22c55e',
    YELLOW: '#eab308',
    RED:    '#ef4444',
    PURPLE: '#7c3aed',
};

const RiskLevelLabels: Record<string, string> = {
    LOW:      'Düşük',
    MEDIUM:   'Orta',
    HIGH:     'Yüksek',
    CRITICAL: 'Kritik',
};

const DamagePinColors: Record<string, string> = {
    UNASSESSED: '#9ca3af',
    LIGHT:      '#facc15',
    MODERATE:   '#f97316',
    HEAVY:      '#dc2626',
    COLLAPSED:  '#7f1d1d',
};

const DamagePinStroke: Record<string, string> = {
    UNASSESSED: '#6b7280',
    LIGHT:      '#ca8a04',
    MODERATE:   '#c2410c',
    HEAVY:      '#991b1b',
    COLLAPSED:  '#450a0a',
};

// ─── Icon factories ───────────────────────────────────────────────────────────
function createDamageIcon(damageLevel: string): L.DivIcon {
    const fill   = DamagePinColors[damageLevel] ?? DamagePinColors.UNASSESSED;
    const stroke = DamagePinStroke[damageLevel] ?? DamagePinStroke.UNASSESSED;
    const svg = `
        <svg xmlns="http://www.w3.org/2000/svg" width="28" height="40" viewBox="0 0 28 40">
            <path d="M14 2 C7.373 2 2 7.373 2 14 C2 23.5 14 38 14 38 S26 23.5 26 14 C26 7.373 20.627 2 14 2 Z"
                  fill="${fill}" stroke="${stroke}" stroke-width="1.5"/>
            <ellipse cx="11" cy="11" rx="4" ry="4" fill="white" fill-opacity="0.28"/>
        </svg>`;
    return L.divIcon({
        className: '',
        html: `<div style="filter:drop-shadow(0 3px 5px rgba(0,0,0,0.45))">${svg}</div>`,
        iconSize:    [28, 40],
        iconAnchor:  [14, 40],
        popupAnchor: [0, -42],
    });
}

function createDistrictCenterIcon(): L.DivIcon {
    const svg = `
        <svg xmlns="http://www.w3.org/2000/svg" width="38" height="38" viewBox="0 0 38 38">
            <circle cx="19" cy="19" r="17" fill="#1d4ed8" stroke="white" stroke-width="2.5"/>
            <text x="19" y="24" text-anchor="middle" font-size="17" font-family="sans-serif" fill="white">🏛</text>
        </svg>`;
    return L.divIcon({
        className: '',
        html: `<div style="filter:drop-shadow(0 3px 6px rgba(0,0,0,0.5))">${svg}</div>`,
        iconSize:    [38, 38],
        iconAnchor:  [19, 38],
        popupAnchor: [0, -40],
    });
}

function createNeighborhoodCenterIcon(): L.DivIcon {
    const svg = `
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32">
            <circle cx="16" cy="16" r="14" fill="#7c3aed" stroke="white" stroke-width="2.5"/>
            <text x="16" y="21" text-anchor="middle" font-size="14" font-family="sans-serif" fill="white">🏘</text>
        </svg>`;
    return L.divIcon({
        className: '',
        html: `<div style="filter:drop-shadow(0 3px 6px rgba(0,0,0,0.5))">${svg}</div>`,
        iconSize:    [32, 32],
        iconAnchor:  [16, 32],
        popupAnchor: [0, -34],
    });
}

function createResourceIcon(count: number): L.DivIcon {
    const svg = `
        <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36">
            <circle cx="18" cy="18" r="16" fill="#2563eb" stroke="white" stroke-width="2.5"/>
            <text x="18" y="23" text-anchor="middle" font-size="15" font-family="sans-serif" fill="white">📦</text>
        </svg>`;
    const badge = count > 1
        ? `<div style="position:absolute;top:-4px;right:-4px;background:#dc2626;color:white;border-radius:50%;width:16px;height:16px;display:flex;align-items:center;justify-content:center;font-size:9px;font-weight:700;font-family:sans-serif;line-height:1">${count > 9 ? '9+' : count}</div>`
        : '';
    return L.divIcon({
        className: '',
        html: `<div style="position:relative;filter:drop-shadow(0 3px 6px rgba(0,0,0,0.5))">${svg}${badge}</div>`,
        iconSize:    [36, 36],
        iconAnchor:  [18, 36],
        popupAnchor: [0, -38],
    });
}

function polygonCenter(polygon: any): L.LatLng | null {
    if (!polygon) return null;
    try {
        const bounds = L.geoJSON({ type: 'Feature', geometry: polygon, properties: {} } as any).getBounds();
        return bounds.isValid() ? bounds.getCenter() : null;
    } catch {
        return null;
    }
}

// ─── Map style helpers ────────────────────────────────────────────────────────
function districtStyle(riskColor: string): L.PathOptions {
    const color = RiskColors[riskColor] || '#9ca3af';
    return { fillColor: color, fillOpacity: 0.45, color, weight: 2.5 };
}

function neighborhoodStyle(riskColor: string, isActive: boolean): L.PathOptions {
    const color = RiskColors[riskColor] || '#9ca3af';
    return {
        fillColor: color,
        fillOpacity: isActive ? 0.75 : 0.55,
        color: isActive ? '#1d4ed8' : '#ffffff',
        weight: isActive ? 2.5 : 1.5,
    };
}

function toFeature(geometry: any): GeoJSON.Feature {
    return { type: 'Feature', geometry, properties: {} };
}

// ─── Tooltip builders ─────────────────────────────────────────────────────────
function buildDistrictTooltip(d: MapDistrictResponse): string {
    const riskLabel = RiskLevelLabels[d.riskLevel] ?? d.riskLevel;
    const riskColor = RiskColors[d.riskColor] || '#9ca3af';
    return `
        <div style="font-weight:600;margin-bottom:4px">${d.name}</div>
        <div style="display:flex;align-items:center;gap:6px;margin-bottom:2px">
            <span style="width:10px;height:10px;border-radius:50%;background:${riskColor};display:inline-block;flex-shrink:0"></span>
            <span>Risk: <strong>${riskLabel}</strong> (${Number(d.riskScore ?? 0).toFixed(1)})</span>
        </div>
        <div style="font-size:11px;color:#374151">Açık olay: ${d.openEventCount} · Kaynak talebi: ${d.openResourceRequestCount}</div>
        <div style="font-size:11px;color:#374151">Hasar tespit edilen bina: ${d.damageCount ?? 0}</div>
        <div style="font-size:11px;color:#6b7280;margin-top:4px">Mahalleleri görmek için tıklayın</div>
    `;
}

function buildNeighborhoodTooltip(nb: MapNeighborhoodResponse): string {
    const riskLabel = RiskLevelLabels[nb.riskLevel] ?? nb.riskLevel;
    const riskColor = RiskColors[nb.riskColor] || '#9ca3af';
    return `
        <div style="font-weight:600;margin-bottom:4px">${nb.name}</div>
        <div style="display:flex;align-items:center;gap:6px;margin-bottom:2px">
            <span style="width:10px;height:10px;border-radius:50%;background:${riskColor};display:inline-block;flex-shrink:0"></span>
            <span>Risk: <strong>${riskLabel}</strong> (${Number(nb.riskScore ?? 0).toFixed(1)})</span>
        </div>
        <div style="font-size:11px;color:#374151">Açık olay: ${nb.openEventCount} · Kaynak talebi: ${nb.openResourceRequestCount}</div>
        <div style="font-size:11px;color:#374151">Hasar tespit edilen bina: ${nb.damageCount ?? 0}</div>
        <div style="font-size:11px;color:#6b7280;margin-top:4px">Hasar pinlerini görmek için tıklayın</div>
    `;
}

// ─── Map bounds controller ────────────────────────────────────────────────────
interface MapBoundsControllerProps { bounds: L.LatLngBounds | null; }
const MapBoundsController: React.FC<MapBoundsControllerProps> = ({ bounds }) => {
    const map = useMap();
    useEffect(() => {
        if (bounds && bounds.isValid()) {
            map.fitBounds(bounds, { padding: [30, 30], maxZoom: 14 });
        } else {
            map.setView([41.0082, 28.9784], 10);
        }
    }, [bounds, map]);
    return null;
};

// ─── Layer control panel ──────────────────────────────────────────────────────
interface LayerState {
    risk: boolean;
    damage: boolean;
    resources: boolean;
    districtCenters: boolean;
    neighborhoodCenters: boolean;
}

const LayerControlPanel: React.FC<{
    layers: LayerState;
    onChange: (key: keyof LayerState) => void;
}> = ({ layers, onChange }) => {
    const [open, setOpen] = useState(true);

    const items: { key: keyof LayerState; label: string; color: string }[] = [
        { key: 'risk',               label: 'Risk Katmanı',                    color: 'bg-green-500' },
        { key: 'damage',             label: 'Hasar Tespiti',                   color: 'bg-orange-500' },
        { key: 'resources',          label: 'Kaynak Talepleri',                color: 'bg-blue-500' },
        { key: 'districtCenters',    label: 'İlçe Koordinasyon Merkezleri',    color: 'bg-indigo-600' },
        { key: 'neighborhoodCenters',label: 'Mahalle Koordinasyon Merkezleri', color: 'bg-violet-600' },
    ];

    return (
        <div
            style={{ position: 'absolute', top: 12, right: 12, zIndex: 1000 }}
            className="bg-white rounded-lg shadow-lg border border-gray-200 overflow-hidden"
        >
            <button
                onClick={() => setOpen(v => !v)}
                className="flex items-center gap-2 px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 w-full"
            >
                <Layers className="h-4 w-4 text-blue-600" />
                Katmanlar
                <span className="ml-auto text-gray-400 text-xs">{open ? '▲' : '▼'}</span>
            </button>
            {open && (
                <div className="border-t border-gray-100 p-2 space-y-1 min-w-[240px]">
                    {items.map(({ key, label, color }) => (
                        <label
                            key={key}
                            className="flex items-center gap-2 px-2 py-1.5 rounded hover:bg-gray-50 cursor-pointer"
                        >
                            <input
                                type="checkbox"
                                checked={layers[key]}
                                onChange={() => onChange(key)}
                                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                            />
                            <span className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${color}`} />
                            <span className="text-xs text-gray-700">{label}</span>
                        </label>
                    ))}
                </div>
            )}
        </div>
    );
};

// ─── Center popup content ─────────────────────────────────────────────────────
function googleMapsUrl(lat: number, lng: number): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
}

function CenterPopupContent({
    title, subtitle, address,
    openEventCount, openResourceRequestCount, totalDamageCount,
    lat, lng, centerUpdatedAt, accentColor,
}: {
    title: string;
    subtitle?: string;
    address?: string;
    openEventCount: number;
    openResourceRequestCount: number;
    totalDamageCount: number;
    lat: number;
    lng: number;
    centerUpdatedAt?: string;
    accentColor: string;
}) {
    return (
        <div style={{ minWidth: 210, fontFamily: 'inherit' }}>
            <p style={{ fontWeight: 700, fontSize: 13, marginBottom: 2, color: accentColor }}>{title}</p>
            {subtitle && (
                <p style={{ fontSize: 11, color: '#6b7280', marginBottom: address ? 2 : 6 }}>{subtitle}</p>
            )}
            {address && (
                <p style={{ fontSize: 11, color: '#374151', marginBottom: 6, fontStyle: 'italic' }}>{address}</p>
            )}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 4, marginBottom: 8 }}>
                <div style={{ textAlign: 'center', background: '#eff6ff', borderRadius: 6, padding: '4px 2px' }}>
                    <div style={{ fontSize: 15, fontWeight: 700, color: '#1d4ed8' }}>{openEventCount}</div>
                    <div style={{ fontSize: 9, color: '#6b7280' }}>Ekip İhtiyacı</div>
                </div>
                <div style={{ textAlign: 'center', background: '#fef3c7', borderRadius: 6, padding: '4px 2px' }}>
                    <div style={{ fontSize: 15, fontWeight: 700, color: '#92400e' }}>{openResourceRequestCount}</div>
                    <div style={{ fontSize: 9, color: '#6b7280' }}>Kaynak Talebi</div>
                </div>
                <div style={{ textAlign: 'center', background: '#fee2e2', borderRadius: 6, padding: '4px 2px' }}>
                    <div style={{ fontSize: 15, fontWeight: 700, color: '#b91c1c' }}>{totalDamageCount}</div>
                    <div style={{ fontSize: 9, color: '#6b7280' }}>Hasar Tespiti</div>
                </div>
            </div>
            <a
                href={googleMapsUrl(lat, lng)}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                    display: 'flex', alignItems: 'center', gap: 5, fontSize: 11,
                    color: '#1d4ed8', textDecoration: 'none', background: '#eff6ff',
                    borderRadius: 6, padding: '4px 8px', marginBottom: 4,
                }}
            >
                <svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polygon points="3 11 22 2 13 21 11 13 3 11"/>
                </svg>
                Google Harita'da Yol Tarifi
            </a>
            {centerUpdatedAt && (
                <p style={{ fontSize: 9, color: '#9ca3af' }}>
                    Son güncelleme: {new Date(centerUpdatedAt).toLocaleDateString('tr-TR')}
                </p>
            )}
        </div>
    );
}

// ─── MapPage ──────────────────────────────────────────────────────────────────
export const MapPage: React.FC = () => {
    const [activeDistrictId, setActiveDistrictId]       = useState<string | null>(null);
    const [activeNeighborhoodId, setActiveNeighborhoodId] = useState<string | null>(null);
    const [activeBounds, setActiveBounds]               = useState<L.LatLngBounds | null>(null);
    const [showDistrictList, setShowDistrictList]       = useState(false);
    const [layers, setLayers] = useState<LayerState>({
        risk:               true,
        damage:             false,
        resources:          false,
        districtCenters:    false,
        neighborhoodCenters: false,
    });

    const toggleLayer = (key: keyof LayerState) =>
        setLayers(prev => ({ ...prev, [key]: !prev[key] }));

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

    const isLoading = loadingDistricts || (!!activeDistrictId && loadingNeighborhoods);

    // ── Handlers ──
    const handleDistrictClick = (district: MapDistrictResponse) => {
        setActiveDistrictId(district.id);
        setActiveNeighborhoodId(null);
        setShowDistrictList(false);
        if (district.polygon) {
            try {
                const bounds = L.geoJSON(district.polygon).getBounds();
                setActiveBounds(bounds);
            } catch { setActiveBounds(null); }
        } else {
            setActiveBounds(null);
        }
    };

    const handleNeighborhoodClick = (nb: MapNeighborhoodResponse) => setActiveNeighborhoodId(nb.id);
    const handleBackToCity        = () => { setActiveDistrictId(null); setActiveNeighborhoodId(null); setActiveBounds(null); };
    const handleBackToDistrict    = () => setActiveNeighborhoodId(null);

    const activeNeighborhoodName = neighborhoods?.find((nb: MapNeighborhoodResponse) => nb.id === activeNeighborhoodId)?.name;
    const activeDistrictName     = districts?.find((d: MapDistrictResponse) => d.id === activeDistrictId)?.name;

    const districtsWithPolygon    = districts?.filter((d: MapDistrictResponse) => !!d.polygon) ?? [];

    return (
        <div className="flex flex-col h-[calc(100vh-100px)]">
            {/* Header */}
            <div className="mb-4 sm:flex sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:tracking-tight">
                        Operasyon Paneli
                    </h1>
                    <p className="mt-1 text-sm text-gray-500">
                        İstanbul geneli risk haritası ve saha operasyon görünümü.
                    </p>
                </div>
                <div className="flex flex-wrap gap-3 items-center mt-2 sm:mt-0">
                    <div className="flex items-center space-x-3 text-sm">
                        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full bg-green-500 inline-block"></span>Düşük</span>
                        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full bg-yellow-500 inline-block"></span>Orta</span>
                        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full bg-red-500 inline-block"></span>Yüksek</span>
                        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full bg-violet-700 inline-block"></span>Kritik</span>
                    </div>
                    {districts && districts.length > 0 && (
                        <button
                            onClick={() => setShowDistrictList(v => !v)}
                            className="text-sm px-3 py-1.5 rounded-md border border-gray-300 bg-white hover:bg-gray-50 font-medium"
                        >
                            {showDistrictList ? 'Listeyi Kapat' : `İlçe Listesi (${districts.length})`}
                        </button>
                    )}
                    <div className="flex items-center gap-2 text-sm">
                        {activeDistrictId && (
                            <button onClick={handleBackToCity} className="font-medium text-blue-600 hover:text-blue-500">
                                ← Şehir Görünümü
                            </button>
                        )}
                        {activeNeighborhoodId && (
                            <>
                                <span className="text-gray-400">/</span>
                                <button onClick={handleBackToDistrict} className="font-medium text-blue-600 hover:text-blue-500">
                                    İlçe Görünümü
                                </button>
                                <span className="text-gray-400">/</span>
                                <span className="text-gray-700 font-medium">{activeNeighborhoodName}</span>
                            </>
                        )}
                    </div>
                </div>
            </div>

            {/* District list panel */}
            {showDistrictList && districts && (
                <div className="mb-3 p-3 bg-white border border-gray-200 rounded-lg shadow-sm max-h-48 overflow-y-auto">
                    <p className="text-xs text-gray-500 mb-2">
                        Haritada polygon olmayan ilçeler (sınır verisi bekleniyor) listeden seçilebilir.
                    </p>
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-1">
                        {[...districts].sort((a: MapDistrictResponse, b: MapDistrictResponse) => a.name.localeCompare(b.name, 'tr')).map((d: MapDistrictResponse) => (
                            <button
                                key={d.id}
                                onClick={() => handleDistrictClick(d)}
                                className={`text-left text-xs px-2 py-1.5 rounded border transition-colors ${
                                    activeDistrictId === d.id
                                        ? 'bg-blue-600 text-white border-blue-600'
                                        : 'bg-white hover:bg-gray-50 border-gray-200'
                                }`}
                            >
                                <span className={`inline-block w-2 h-2 rounded-full mr-1 ${
                                    d.riskColor === 'GREEN' ? 'bg-green-500' :
                                    d.riskColor === 'YELLOW' ? 'bg-yellow-500' :
                                    d.riskColor === 'RED' ? 'bg-red-500' : 'bg-violet-600'
                                }`} />
                                {d.name}
                                {!d.polygon && <span className="text-gray-400 ml-1">○</span>}
                            </button>
                        ))}
                    </div>
                </div>
            )}

            {/* Neighborhood list for districts without polygon */}
            {activeDistrictId && neighborhoods && neighborhoods.length > 0 &&
                !districts?.find((d: MapDistrictResponse) => d.id === activeDistrictId)?.polygon && (
                <div className="mb-3 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                    <p className="text-xs font-medium text-blue-800 mb-2">{activeDistrictName} — Mahalleler</p>
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-1">
                        {neighborhoods.map((nb: MapNeighborhoodResponse) => (
                            <button
                                key={nb.id}
                                onClick={() => handleNeighborhoodClick(nb)}
                                className={`text-left text-xs px-2 py-1 rounded border ${
                                    activeNeighborhoodId === nb.id
                                        ? 'bg-blue-600 text-white border-blue-600'
                                        : 'bg-white hover:bg-gray-50 border-gray-200'
                                }`}
                            >
                                <span className={`inline-block w-2 h-2 rounded-full mr-1 ${
                                    nb.riskColor === 'GREEN' ? 'bg-green-500' :
                                    nb.riskColor === 'YELLOW' ? 'bg-yellow-500' :
                                    nb.riskColor === 'RED' ? 'bg-red-500' : 'bg-violet-600'
                                }`} />
                                {nb.name}
                            </button>
                        ))}
                    </div>
                </div>
            )}

            {/* Map */}
            <div className="relative flex-1 bg-gray-100 rounded-lg overflow-hidden border border-gray-300 shadow-sm z-0">
                {isLoading && (
                    <div className="absolute inset-0 z-50 flex items-center justify-center bg-white/50 backdrop-blur-sm">
                        <LoadingSpinner size="lg" />
                    </div>
                )}

                {/* Layer control panel — absolute inside map container */}
                <LayerControlPanel layers={layers} onChange={toggleLayer} />

                {/* Hint — shown when neighborhood centers toggle on but no district selected */}
                {layers.neighborhoodCenters && !activeDistrictId && (
                    <div
                        style={{ position: 'absolute', bottom: 24, left: '50%', transform: 'translateX(-50%)', zIndex: 1000 }}
                        className="bg-violet-50 border border-violet-200 text-violet-800 text-xs px-4 py-2 rounded-full shadow-md whitespace-nowrap pointer-events-none"
                    >
                        Mahalle koordinasyon merkezlerini görmek için önce bir ilçe seçin.
                    </div>
                )}

                <MapContainer center={[41.0082, 28.9784]} zoom={10} className="w-full h-full">
                    <TileLayer
                        attribution={import.meta.env.VITE_MAP_ATTRIBUTION || '&copy; OpenStreetMap contributors'}
                        url={import.meta.env.VITE_MAP_TILE_URL || 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png'}
                    />
                    <MapBoundsController bounds={activeBounds} />

                    {/* ── Risk layer: District polygons ── */}
                    {layers.risk && !activeDistrictId && districtsWithPolygon.map((district: MapDistrictResponse) => (
                        <GeoJSON
                            key={district.id + '-' + district.riskColor}
                            data={toFeature(district.polygon)}
                            style={() => districtStyle(district.riskColor)}
                            onEachFeature={(_, layer) => {
                                layer.bindTooltip(buildDistrictTooltip(district), { sticky: true });
                                layer.on('click', () => handleDistrictClick(district));
                            }}
                        />
                    ))}

                    {/* ── Risk layer: Neighborhood polygons ── */}
                    {layers.risk && activeDistrictId && neighborhoods?.map((nb: MapNeighborhoodResponse) => {
                        if (!nb.polygon) return null;
                        const isActive = nb.id === activeNeighborhoodId;
                        return (
                            <GeoJSON
                                key={nb.id + '-' + nb.riskColor + '-' + isActive}
                                data={toFeature(nb.polygon)}
                                style={() => neighborhoodStyle(nb.riskColor, isActive)}
                                onEachFeature={(_, layer) => {
                                    layer.bindTooltip(buildNeighborhoodTooltip(nb), { sticky: true });
                                    layer.on('click', () => handleNeighborhoodClick(nb));
                                }}
                            />
                        );
                    })}

                    {/* ── Damage layer: pins ── */}
                    {layers.damage && damagePoints.map((point: DamagePointResponse) => (
                        <Marker
                            key={point.id}
                            position={[point.latitude, point.longitude]}
                            icon={createDamageIcon(point.damageLevel)}
                        >
                            <Popup>
                                <div style={{ minWidth: 190, fontFamily: 'inherit' }}>
                                    <p style={{ fontWeight: 700, fontSize: 13, marginBottom: 4, borderBottom: '1px solid #e5e7eb', paddingBottom: 4 }}>
                                        Hasar Tespiti
                                    </p>
                                    {point.address && (
                                        <p style={{ fontSize: 12, color: '#374151', marginBottom: 3 }}>
                                            <strong>Adres:</strong> {point.address}
                                        </p>
                                    )}
                                    {(point.districtName || point.neighborhoodName) && (
                                        <p style={{ fontSize: 11, color: '#6b7280', marginBottom: 3 }}>
                                            {point.districtName}{point.neighborhoodName ? ' / ' + point.neighborhoodName : ''}
                                        </p>
                                    )}
                                    <p style={{ fontSize: 12, marginBottom: 3 }}>
                                        <strong>Hasar:</strong> {point.damageLevelLabel}
                                    </p>
                                    <p style={{ fontSize: 12, marginBottom: 3 }}>
                                        <strong>Durum:</strong> {point.verificationStatusLabel}
                                    </p>
                                    {(point.gasLeakRisk || point.collapseRisk || point.emergencyEvacuationNeeded || point.casualtiesSuspected) && (
                                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3, marginTop: 4, marginBottom: 4 }}>
                                            {point.gasLeakRisk && (
                                                <span style={{ fontSize: 10, background: '#fee2e2', color: '#b91c1c', padding: '1px 6px', borderRadius: 9999 }}>Gaz sızıntısı</span>
                                            )}
                                            {point.collapseRisk && (
                                                <span style={{ fontSize: 10, background: '#fee2e2', color: '#b91c1c', padding: '1px 6px', borderRadius: 9999 }}>Çökme riski</span>
                                            )}
                                            {point.emergencyEvacuationNeeded && (
                                                <span style={{ fontSize: 10, background: '#fef3c7', color: '#b45309', padding: '1px 6px', borderRadius: 9999 }}>Tahliye gerekli</span>
                                            )}
                                            {point.casualtiesSuspected && (
                                                <span style={{ fontSize: 10, background: '#fef3c7', color: '#b45309', padding: '1px 6px', borderRadius: 9999 }}>Kayıp şüphesi</span>
                                            )}
                                        </div>
                                    )}
                                    <a
                                        href={googleMapsUrl(point.latitude, point.longitude)}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 11, color: '#1d4ed8', marginTop: 4 }}
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <polygon points="3 11 22 2 13 21 11 13 3 11"/>
                                        </svg>
                                        Google Harita'da Aç
                                    </a>
                                    {point.note && (
                                        <p style={{ fontSize: 11, color: '#6b7280', borderTop: '1px solid #e5e7eb', paddingTop: 4, marginTop: 4 }}>
                                            <strong>Not:</strong> {point.note}
                                        </p>
                                    )}
                                </div>
                            </Popup>
                        </Marker>
                    ))}

                    {/* ── Resource requests layer: district centroids (city view) ── */}
                    {layers.resources && !activeDistrictId && districtsWithPolygon
                        .filter(d => d.openResourceRequestCount > 0)
                        .map(district => {
                            const center = polygonCenter(district.polygon);
                            if (!center) return null;
                            return (
                                <Marker
                                    key={`res-d-${district.id}`}
                                    position={center}
                                    icon={createResourceIcon(district.openResourceRequestCount)}
                                >
                                    <Popup>
                                        <div style={{ minWidth: 180, fontFamily: 'inherit' }}>
                                            <p style={{ fontWeight: 700, fontSize: 13, marginBottom: 4, borderBottom: '1px solid #e5e7eb', paddingBottom: 4 }}>
                                                Kaynak Talebi
                                            </p>
                                            <p style={{ fontSize: 12, color: '#374151', marginBottom: 3 }}>
                                                <strong>İlçe:</strong> {district.name}
                                            </p>
                                            <p style={{ fontSize: 12, color: '#374151', marginBottom: 3 }}>
                                                <strong>Açık Talep:</strong> {district.openResourceRequestCount}
                                            </p>
                                            <p style={{ fontSize: 11, color: '#6b7280' }}>
                                                Detay için ilçeyi seçin.
                                            </p>
                                        </div>
                                    </Popup>
                                </Marker>
                            );
                        })
                    }

                    {/* ── Resource requests layer: neighborhood centroids (district view) ── */}
                    {layers.resources && activeDistrictId && neighborhoods
                        ?.filter(nb => nb.openResourceRequestCount > 0 && nb.polygon)
                        .map(nb => {
                            const center = polygonCenter(nb.polygon);
                            if (!center) return null;
                            return (
                                <Marker
                                    key={`res-nb-${nb.id}`}
                                    position={center}
                                    icon={createResourceIcon(nb.openResourceRequestCount)}
                                >
                                    <Popup>
                                        <div style={{ minWidth: 180, fontFamily: 'inherit' }}>
                                            <p style={{ fontWeight: 700, fontSize: 13, marginBottom: 4, borderBottom: '1px solid #e5e7eb', paddingBottom: 4 }}>
                                                Kaynak Talebi
                                            </p>
                                            <p style={{ fontSize: 12, color: '#374151', marginBottom: 3 }}>
                                                <strong>Mahalle:</strong> {nb.name}
                                            </p>
                                            <p style={{ fontSize: 12, color: '#374151', marginBottom: 3 }}>
                                                <strong>İlçe:</strong> {activeDistrictName}
                                            </p>
                                            <p style={{ fontSize: 12, color: '#374151', marginBottom: 3 }}>
                                                <strong>Açık Talep:</strong> {nb.openResourceRequestCount}
                                            </p>
                                        </div>
                                    </Popup>
                                </Marker>
                            );
                        })
                    }

                    {/* ── District coordination centers layer (all city view) ── */}
                    {layers.districtCenters && districtCenters.map((center: DistrictCoordinatorMapResponse) => (
                        <Marker
                            key={`dc-${center.districtId}`}
                            position={[center.latitude, center.longitude]}
                            icon={createDistrictCenterIcon()}
                        >
                            <Popup>
                                <CenterPopupContent
                                    title={`${center.districtName} Koordinasyon Merkezi`}
                                    subtitle="İlçe Koordinasyon Merkezi"
                                    address={center.address}
                                    openEventCount={center.openEventCount}
                                    openResourceRequestCount={center.openResourceRequestCount}
                                    totalDamageCount={center.totalDamageCount}
                                    lat={center.latitude}
                                    lng={center.longitude}
                                    centerUpdatedAt={center.centerUpdatedAt}
                                    accentColor="#1d4ed8"
                                />
                            </Popup>
                        </Marker>
                    ))}

                    {/* ── Neighborhood coordination centers layer (district detail view only) ── */}
                    {layers.neighborhoodCenters && activeDistrictId && neighborhoodCenters.map((center: NeighborhoodCoordinatorMapResponse) => (
                        <Marker
                            key={`nc-${center.neighborhoodId}`}
                            position={[center.latitude, center.longitude]}
                            icon={createNeighborhoodCenterIcon()}
                        >
                            <Popup>
                                <CenterPopupContent
                                    title={`${center.neighborhoodName} Koordinasyon Merkezi`}
                                    subtitle={center.districtName ? `${center.districtName} İlçesi` : undefined}
                                    address={center.address}
                                    openEventCount={center.openEventCount}
                                    openResourceRequestCount={center.openResourceRequestCount}
                                    totalDamageCount={center.totalDamageCount}
                                    lat={center.latitude}
                                    lng={center.longitude}
                                    centerUpdatedAt={center.centerUpdatedAt}
                                    accentColor="#7c3aed"
                                />
                            </Popup>
                        </Marker>
                    ))}

                </MapContainer>
            </div>
        </div>
    );
};
