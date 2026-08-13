/** Bir GeoJSON Geometry/Feature'ın kapsadığı [minLon, minLat, maxLon, maxLat] kutusunu hesaplar. */
export function computeGeoJsonBbox(input: unknown): [number, number, number, number] | null {
    let minLon = Infinity;
    let minLat = Infinity;
    let maxLon = -Infinity;
    let maxLat = -Infinity;

    function visit(coords: unknown): void {
        if (!Array.isArray(coords)) return;
        if (typeof coords[0] === 'number') {
            const [lon, lat] = coords as number[];
            if (lon < minLon) minLon = lon;
            if (lon > maxLon) maxLon = lon;
            if (lat < minLat) minLat = lat;
            if (lat > maxLat) maxLat = lat;
        } else {
            coords.forEach(visit);
        }
    }

    const geometry: any = input && (input as any).type === 'Feature' ? (input as any).geometry : input;
    if (!geometry?.coordinates) return null;
    visit(geometry.coordinates);
    if (!Number.isFinite(minLon) || !Number.isFinite(minLat)) return null;
    return [minLon, minLat, maxLon, maxLat];
}
