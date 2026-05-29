import { useEffect, useRef, useState, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, GeoJSON, useMapEvents, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useQuery } from '@tanstack/react-query';
import { getMapNeighborhoods } from '@/api/map.api';
import { MapNeighborhoodResponse } from '@/types';
import { CheckCircle, MapPin, AlertCircle, Loader2 } from 'lucide-react';
import { isPointInGeoJsonPolygon } from '@/utils/pointInPolygon';

// Leaflet icon fix (idempotent)
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const ISTANBUL_CENTER: [number, number] = [41.0082, 28.9784];
const OUT_OF_BOUNDS_MSG =
    'Seçtiğiniz mahalle dışında bir alan için konum seçiyorsunuz. Lütfen seçtiğiniz mahalle sınırları içerisinde konumunuzu doğrulayın.';

function getPolygonCenter(polygon: any): [number, number] | null {
    if (!polygon) return null;
    try {
        const bounds = L.geoJSON(polygon).getBounds();
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

// Moves the map view programmatically
const MapRecenterer: React.FC<{ center: [number, number]; zoom?: number }> = ({ center, zoom = 16 }) => {
    const map = useMap();
    useEffect(() => {
        map.setView(center, zoom);
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [center[0], center[1]]);
    return null;
};

// Handles map clicks to place marker — validates against polygon
const MapClickHandler: React.FC<{
    polygon: any;
    onValidClick: (lat: number, lng: number) => void;
    onOutOfBounds: () => void;
}> = ({ polygon, onValidClick, onOutOfBounds }) => {
    useMapEvents({
        click(e) {
            const { lat, lng } = e.latlng;
            if (polygon && !isPointInGeoJsonPolygon(lat, lng, polygon)) {
                onOutOfBounds();
                return;
            }
            onValidClick(lat, lng);
        },
    });
    return null;
};

export interface LocationPickerMapProps {
    districtId: string;
    neighborhoodId: string;
    neighborhoodName?: string;
    districtName?: string;
    streetName?: string;
    buildingNo?: string;
    lat: number | null;
    lng: number | null;
    locationConfirmed: boolean;
    onChange: (lat: number, lng: number) => void;
    onConfirm: () => void;
}

export const LocationPickerMap: React.FC<LocationPickerMapProps> = ({
    districtId,
    neighborhoodId,
    neighborhoodName,
    districtName,
    streetName,
    buildingNo,
    lat,
    lng,
    locationConfirmed,
    onChange,
    onConfirm,
}) => {
    const prevNeighborhoodIdRef = useRef<string>('');
    const [outOfBoundsError, setOutOfBoundsError] = useState<string | null>(null);
    const [geocoding, setGeocoding] = useState(false);
    const geocodeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const { data: neighborhoods = [] } = useQuery<MapNeighborhoodResponse[]>({
        queryKey: ['map', 'neighborhoods', districtId],
        queryFn: () => getMapNeighborhoods(districtId),
        enabled: !!districtId,
        staleTime: 5 * 60 * 1000,
    });

    const selectedNb = neighborhoods.find((n) => n.id === neighborhoodId) ?? null;
    const nbCenter = selectedNb ? getPolygonCenter(selectedNb.polygon) : null;
    const polygon = selectedNb?.polygon ?? null;

    // Mahalle değiştiğinde markeri otomatik olarak mahalle merkezine taşı
    useEffect(() => {
        if (!neighborhoodId || neighborhoodId === prevNeighborhoodIdRef.current) return;
        prevNeighborhoodIdRef.current = neighborhoodId;
        setOutOfBoundsError(null);
        if (nbCenter) {
            onChange(nbCenter[0], nbCenter[1]);
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [neighborhoodId]);

    // Validated position setter
    const handleValidatedPosition = useCallback(
        (newLat: number, newLng: number) => {
            if (polygon && !isPointInGeoJsonPolygon(newLat, newLng, polygon)) {
                setOutOfBoundsError(OUT_OF_BOUNDS_MSG);
                return;
            }
            setOutOfBoundsError(null);
            onChange(newLat, newLng);
        },
        [polygon, onChange]
    );

    // Address geocoding: debounce 1 second, call Nominatim
    useEffect(() => {
        if (!neighborhoodId) return;
        const street = (streetName ?? '').trim();
        const bno = (buildingNo ?? '').trim();
        if (!street && !bno) return;

        if (geocodeTimerRef.current) clearTimeout(geocodeTimerRef.current);

        geocodeTimerRef.current = setTimeout(async () => {
            // Build query: streetName + buildingNo, neighborhoodName, districtName, Istanbul
            const parts = [
                street && bno ? `${street} ${bno}` : street || bno,
                neighborhoodName,
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

                if (polygon && isPointInGeoJsonPolygon(geoLat, geoLng, polygon)) {
                    setOutOfBoundsError(null);
                    onChange(geoLat, geoLng);
                } else if (nbCenter) {
                    // Geocode result outside polygon → use neighborhood center as fallback
                    setOutOfBoundsError(null);
                    onChange(nbCenter[0], nbCenter[1]);
                }
            } catch {
                // Geocoding failed silently — user can still pick manually
            } finally {
                setGeocoding(false);
            }
        }, 1000);

        return () => {
            if (geocodeTimerRef.current) clearTimeout(geocodeTimerRef.current);
        };
    // Only trigger when address fields change
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [streetName, buildingNo, neighborhoodId]);

    const mapCenter: [number, number] = nbCenter ?? ISTANBUL_CENTER;
    const hasPosition = lat !== null && lng !== null;

    return (
        <div className="space-y-2">
            {/* Bilgilendirme */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg px-3 py-2 text-xs text-blue-700 leading-relaxed">
                <span className="font-semibold">Konum Doğrulama:</span> Binanın konumunu haritada kontrol edin.
                Pini sürükleyerek veya haritaya tıklayarak doğru binayı işaretleyin, ardından{' '}
                <span className="font-semibold">"Konumu Onayla"</span>'ya tıklayın.
            </div>

            {/* Geocoding indicator */}
            {geocoding && (
                <div className="flex items-center gap-1.5 text-xs text-blue-600">
                    <Loader2 className="h-3 w-3 animate-spin" />
                    Adres aranıyor...
                </div>
            )}

            {/* Out of bounds error */}
            {outOfBoundsError && (
                <div className="flex items-start gap-2 bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-xs text-red-700">
                    <AlertCircle className="h-3.5 w-3.5 flex-shrink-0 mt-0.5" />
                    <span>{outOfBoundsError}</span>
                </div>
            )}

            {/* Harita */}
            <div
                style={{ height: '320px', borderRadius: '8px', overflow: 'hidden', border: '1px solid #e5e7eb' }}
                className="relative z-0"
            >
                <MapContainer
                    center={mapCenter}
                    zoom={15}
                    style={{ height: '100%', width: '100%' }}
                    zoomControl={true}
                >
                    <TileLayer
                        attribution="&copy; OpenStreetMap contributors"
                        url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
                    />

                    {/* Mahalle değişince haritayı yeni merkeze taşı */}
                    {nbCenter && <MapRecenterer center={nbCenter} />}

                    {/* Haritaya tıklanınca marker taşı (polygon kontrolü ile) */}
                    <MapClickHandler
                        polygon={polygon}
                        onValidClick={(clat, clng) => {
                            setOutOfBoundsError(null);
                            onChange(clat, clng);
                        }}
                        onOutOfBounds={() => setOutOfBoundsError(OUT_OF_BOUNDS_MSG)}
                    />

                    {/* Seçili mahalle poligonu */}
                    {selectedNb?.polygon && (
                        <GeoJSON
                            key={selectedNb.id}
                            data={toFeature(selectedNb.polygon)}
                            style={() => ({
                                fillColor: '#3b82f6',
                                fillOpacity: 0.12,
                                color: '#3b82f6',
                                weight: 2,
                                dashArray: '4',
                            })}
                        />
                    )}

                    {/* Sürüklenebilir marker */}
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

            {/* Koordinat satırı + Onay butonu */}
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
