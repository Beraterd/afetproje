/**
 * Point-in-polygon (ray casting) for GeoJSON geometries.
 * GeoJSON uses [longitude, latitude] ordering in coordinates.
 */

function isPointInRing(lngLat: [number, number], ring: [number, number][]): boolean {
    const [x, y] = lngLat;
    let inside = false;
    for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
        const [xi, yi] = ring[i];
        const [xj, yj] = ring[j];
        const intersect =
            yi > y !== yj > y &&
            x < ((xj - xi) * (y - yi)) / (yj - yi) + xi;
        if (intersect) inside = !inside;
    }
    return inside;
}

/**
 * Returns true if the point (lat, lng) falls inside the given GeoJSON geometry.
 * If geometry is null/undefined, returns true (no restriction).
 */
export function isPointInGeoJsonPolygon(lat: number, lng: number, geometry: any): boolean {
    if (!geometry) return true;

    const point: [number, number] = [lng, lat]; // GeoJSON: [lon, lat]

    if (geometry.type === 'Polygon') {
        const outerRing = geometry.coordinates[0] as [number, number][];
        return isPointInRing(point, outerRing);
    }

    if (geometry.type === 'MultiPolygon') {
        return (geometry.coordinates as [number, number][][][]).some((poly) =>
            isPointInRing(point, poly[0])
        );
    }

    return true;
}
