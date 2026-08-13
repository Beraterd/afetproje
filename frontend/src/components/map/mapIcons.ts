import L from 'leaflet';
import type { MapDistrictResponse, MapNeighborhoodResponse } from '@/types';
import { DAMAGE_STATUS_COLORS } from '@/utils/damageColors';

// ─── Color tables ────────────────────────────────────────────────────────────
export const RiskColors: Record<string, string> = {
    GREEN:  '#22c55e',
    YELLOW: '#eab308',
    RED:    '#ef4444',
    PURPLE: '#7c3aed',
};

export const RiskLevelLabels: Record<string, string> = {
    LOW:      'Düşük',
    MEDIUM:   'Orta',
    HIGH:     'Yüksek',
    CRITICAL: 'Kritik',
};

export const DamagePinColors: Record<string, string> = DAMAGE_STATUS_COLORS;

const DamagePinStroke: Record<string, string> = {
    UNASSESSED: '#6b7280',
    LIGHT:      '#ca8a04',
    MODERATE:   '#c2410c',
    HEAVY:      '#991b1b',
    COLLAPSED:  '#450a0a',
};

// ─── Icon factories ───────────────────────────────────────────────────────────
export function createDamageIcon(damageLevel: string): L.DivIcon {
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

export function createDistrictCenterIcon(): L.DivIcon {
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

export function createNeighborhoodCenterIcon(): L.DivIcon {
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

export function createResourceIcon(count: number): L.DivIcon {
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

export function polygonCenter(polygon: any): L.LatLng | null {
    if (!polygon) return null;
    try {
        const bounds = L.geoJSON({ type: 'Feature', geometry: polygon, properties: {} } as any).getBounds();
        return bounds.isValid() ? bounds.getCenter() : null;
    } catch {
        return null;
    }
}

// ─── Map style helpers ────────────────────────────────────────────────────────
export function districtStyle(riskColor: string): L.PathOptions {
    const color = RiskColors[riskColor] || '#9ca3af';
    return { fillColor: color, fillOpacity: 0.45, color, weight: 2.5 };
}

export function neighborhoodStyle(riskColor: string, isActive: boolean): L.PathOptions {
    const color = RiskColors[riskColor] || '#9ca3af';
    return {
        fillColor: color,
        fillOpacity: isActive ? 0.75 : 0.55,
        color: isActive ? '#1d4ed8' : '#ffffff',
        weight: isActive ? 2.5 : 1.5,
    };
}

export function toFeature(geometry: any): GeoJSON.Feature {
    return { type: 'Feature', geometry, properties: {} };
}

export function googleMapsUrl(lat: number, lng: number): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
}

// ─── Tooltip builders (hover — sürükleme/seçim değil) ─────────────────────────
export function buildDistrictTooltip(d: MapDistrictResponse): string {
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

export function buildNeighborhoodTooltip(nb: MapNeighborhoodResponse): string {
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
