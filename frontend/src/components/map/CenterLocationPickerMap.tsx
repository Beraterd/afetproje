/**
 * CenterLocationPickerMap
 *
 * A reusable map component for picking an operational coordination center location.
 * Unlike LocationPickerMap (which fetches neighborhood data internally), this component
 * receives the GeoJSON polygon geometry directly as a prop.
 *
 * Used for both district and neighborhood coordination center forms.
 */
import { useEffect, useRef, useState, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, GeoJSON, useMapEvents, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { CheckCircle, MapPin, AlertCircle, Loader2 } from 'lucide-react';
import { isPointInGeoJsonPolygon } from '@/utils/pointInPolygon';

delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const ISTANBUL_CENTER: [number, number] = [41.0082, 28.9784];

function getPolygonCenter(polygon: any): [number, number] | null {
    if (!polygon) return null;
    try {
        const bounds = L.geoJSON({ type: 'Feature', geometry: polygon, properties: {} } as any).getBounds();
        if (!bounds.isValid()) return null;
        const c = bounds.getCenter();
        return [c.lat, c.lng];
    } catch {
        return null;
    }
}

function toFeature(geometry: any): GeoJSON.Feature {
    return { type: 'Feature', geometry, properties: {} };
}

const MapRecenterer: React.FC<{ center: [number, number]; zoom?: number }> = ({ center, zoom = 14 }) => {
    const map = useMap();
    useEffect(() => {
        map.setView(center, zoom);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [center[0], center[1]]);
    return null;
};

const MapClickHandler: React.FC<{
    polygon: any;
    onValidClick: (lat: number, lng: number) => void;
    onOutOfBounds: (msg: string) => void;
    boundaryLabel: string;
}> = ({ polygon, onValidClick, onOutOfBounds, boundaryLabel }) => {
    useMapEvents({
        click(e) {
            const { lat, lng } = e.latlng;
            if (polygon && !isPointInGeoJsonPolygon(lat, lng, polygon)) {
                onOutOfBounds(
                    `Seçtiğiniz konum ${boundaryLabel} sınırları dışında kalıyor. Lütfen ${boundaryLabel} sınırları içinden bir konum seçin.`
                );
                return;
            }
            onValidClick(lat, lng);
        },
    });
    return null;
};

export interface CenterLocationPickerMapProps {
    /** GeoJSON geometry (Polygon or MultiPolygon) of the boundary to restrict movement */
    polygon: any;
    /** Initial map center; defaults to polygon centroid or Istanbul center */
    initialCenter?: [number, number];
    /** Turkish label used in error messages: "ilçe" or "mahalle" */
    boundaryLabel: string;
    /** Optional address fields for geocoding */
    streetName?: string;
    buildingNo?: string;
    areaName?: string;   // neighborhood or district name for geocoding
    districtName?: string;

    lat: number | null;
    lng: number | null;
    locationConfirmed: boolean;
    onChange: (lat: number, lng: number) => void;
    onConfirm: () => void;
}

export const CenterLocationPickerMap: React.FC<CenterLocationPickerMapProps> = ({
    polygon,
    initialCenter,
    boundaryLabel,
    streetName,
    buildingNo,
    areaName,
    districtName,
    lat,
    lng,
    locationConfirmed,
    onChange,
    onConfirm,
}) => {
    const [outOfBoundsError, setOutOfBoundsError] = useState<string | null>(null);
    const [geocoding, setGeocoding] = useState(false);
    const geocodeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const polyCenter = getPolygonCenter(polygon);
    const mapCenter: [number, number] = initialCenter ?? polyCenter ?? ISTANBUL_CENTER;
    const hasPosition = lat !== null && lng !== null;

    const handleValidatedPosition = useCallback(
        (newLat: number, newLng: number) => {
            if (polygon && !isPointInGeoJsonPolygon(newLat, newLng, polygon)) {
                setOutOfBoundsError(
                    `Seçtiğiniz konum ${boundaryLabel} sınırları dışında kalıyor. Lütfen ${boundaryLabel} sınırları içinden bir konum seçin.`
                );
                return;
            }
            setOutOfBoundsError(null);
            onChange(newLat, newLng);
        },
        [polygon, boundaryLabel, onChange]
    );

    // Address geocoding: debounce 1 second, call Nominatim
    useEffect(() => {
        const street = (streetName ?? '').trim();
        const bno = (buildingNo ?? '').trim();
        if (!street && !bno) return;

        if (geocodeTimerRef.current) clearTimeout(geocodeTimerRef.current);

        geocodeTimerRef.current = setTimeout(async () => {
            const parts = [
                street && bno ? `${street} ${bno}` : street || bno,
                areaName,
                districtName,
                'İstanbul',
                'Türkiye',
            ].filter(Boolean);
            const q = encodeURIComponent(parts.join(', '));
            const url = `https://nominatim.openstreetmap.org/search?q=${q}&format=json&limit=1&addressdetails=0`;

            setGeocoding(true);
            try {
                const res = await fetch(url, {
                    headers: { 'Accept-Language': 'tr', 'User-Agent': 'AfetKoordinasyon/1.0' },
                });
                if (!res.ok) return;
                const data = await res.json();
                if (!data || data.length === 0) return;

                const geoLat = parseFloat(data[0].lat);
                const geoLng = parseFloat(data[0].lon);

                if (!polygon || isPointInGeoJsonPolygon(geoLat, geoLng, polygon)) {
                    setOutOfBoundsError(null);
                    onChange(geoLat, geoLng);
                } else if (polyCenter) {
                    setOutOfBoundsError(null);
                    onChange(polyCenter[0], polyCenter[1]);
                }
            } catch {
                // Geocoding failed silently
            } finally {
                setGeocoding(false);
            }
        }, 1000);

        return () => {
            if (geocodeTimerRef.current) clearTimeout(geocodeTimerRef.current);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [streetName, buildingNo]);

    return (
        <div className="space-y-2">
            <div className="bg-blue-50 border border-blue-200 rounded-lg px-3 py-2 text-xs text-blue-700 leading-relaxed">
                <span className="font-semibold">Konum Seçimi:</span> Koordinatörlük merkezinin fiziksel konumunu haritada işaretleyin.
                Haritaya tıklayarak veya pini sürükleyerek konumu belirleyin, ardından{' '}
                <span className="font-semibold">"Konumu Onayla"</span>'ya tıklayın.
            </div>

            {geocoding && (
                <div className="flex items-center gap-1.5 text-xs text-blue-600">
                    <Loader2 className="h-3 w-3 animate-spin" />
                    Adres aranıyor...
                </div>
            )}

            {outOfBoundsError && (
                <div className="flex items-start gap-2 bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-xs text-red-700">
                    <AlertCircle className="h-3.5 w-3.5 flex-shrink-0 mt-0.5" />
                    <span>{outOfBoundsError}</span>
                </div>
            )}

            <div
                style={{ height: '320px', borderRadius: '8px', overflow: 'hidden', border: '1px solid #e5e7eb' }}
                className="relative z-0"
            >
                <MapContainer
                    center={mapCenter}
                    zoom={13}
                    style={{ height: '100%', width: '100%' }}
                    zoomControl={true}
                >
                    <TileLayer
                        attribution="&copy; OpenStreetMap contributors"
                        url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
                    />

                    {mapCenter && <MapRecenterer center={mapCenter} zoom={hasPosition ? 15 : 13} />}

                    <MapClickHandler
                        polygon={polygon}
                        onValidClick={(clat, clng) => {
                            setOutOfBoundsError(null);
                            onChange(clat, clng);
                        }}
                        onOutOfBounds={(msg) => setOutOfBoundsError(msg)}
                        boundaryLabel={boundaryLabel}
                    />

                    {polygon && (
                        <GeoJSON
                            key={JSON.stringify(polygon).slice(0, 32)}
                            data={toFeature(polygon)}
                            style={() => ({
                                fillColor: '#3b82f6',
                                fillOpacity: 0.1,
                                color: '#3b82f6',
                                weight: 2,
                                dashArray: '5',
                            })}
                        />
                    )}

                    {hasPosition && (
                        <Marker
                            position={[lat!, lng!]}
                            draggable={true}
                            eventHandlers={{
                                dragend: (e: any) => {
                                    const pos = e.target.getLatLng();
                                    handleValidatedPosition(pos.lat, pos.lng);
                                },
                            }}
                        />
                    )}
                </MapContainer>
            </div>

            {hasPosition ? (
                <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-500 font-mono">
                        {lat!.toFixed(6)}, {lng!.toFixed(6)}
                    </span>
                    {locationConfirmed ? (
                        <span className="flex items-center gap-1.5 text-xs text-green-600 font-semibold">
                            <CheckCircle className="h-3.5 w-3.5" />
                            Konum onaylandı
                        </span>
                    ) : (
                        <button
                            type="button"
                            onClick={onConfirm}
                            className="flex items-center gap-1.5 text-xs px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white rounded-lg font-medium transition-colors"
                        >
                            <MapPin className="h-3.5 w-3.5" />
                            Konumu Onayla
                        </button>
                    )}
                </div>
            ) : (
                <p className="text-xs text-gray-400 text-center py-1">
                    Haritaya tıklayarak veya pini sürükleyerek konum seçin
                </p>
            )}
        </div>
    );
};
