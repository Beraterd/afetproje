import React from 'react';
import { MAP_TILE_URL, MAP_TILE_ATTRIBUTION } from '@/config/mapTiles';
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import { EarthquakeEventResponse, EarthquakeRiskLevel } from '@/types';
import { SOURCE_BADGE_CLASSES, SOURCE_LABELS } from '@/utils/earthquakeSourceLabels';

const RISK_MARKER_COLOR: Record<EarthquakeRiskLevel, string> = {
    LOW: '#9ca3af',
    MEDIUM: '#3b82f6',
    HIGH: '#f59e0b',
    CRITICAL: '#dc2626',
};

interface EarthquakeMapProps {
    points: EarthquakeEventResponse[];
}

/** Liste ile AYNI filtrelenmiş veri kümesini alır — bağımsız bir dataset çekmez (item 12). */
export const EarthquakeMap: React.FC<EarthquakeMapProps> = ({ points }) => {
    if (points.length === 0) {
        return null;
    }

    return (
        <div className="glass-card overflow-hidden" style={{ height: 360 }} data-testid="earthquake-map">
            <MapContainer center={[39.0, 35.0]} zoom={6} className="w-full h-full">
                <TileLayer
                    attribution={MAP_TILE_ATTRIBUTION}
                    url={MAP_TILE_URL}
                />
                {points.map((eq) => (
                    <CircleMarker
                        key={eq.id}
                        center={[eq.latitude, eq.longitude]}
                        radius={Math.max(4, eq.magnitude * 2.2)}
                        pathOptions={{
                            color: RISK_MARKER_COLOR[eq.riskLevel],
                            fillColor: RISK_MARKER_COLOR[eq.riskLevel],
                            fillOpacity: 0.55,
                        }}
                    >
                        <Popup>
                            <div className="text-sm">
                                <strong>M{eq.magnitude.toFixed(1)}</strong> — {eq.location || 'Konum belirlenemedi'}
                                <br />
                                <span className={`inline-block mt-1 px-1.5 py-0.5 rounded text-xs ${SOURCE_BADGE_CLASSES[eq.source]}`}>
                                    {SOURCE_LABELS[eq.source]}
                                </span>
                            </div>
                        </Popup>
                    </CircleMarker>
                ))}
            </MapContainer>
        </div>
    );
};
