const EARTH_RADIUS_M = 6371000;

/** Ortalama yürüyüş hızı — 4.5-5 km/s aralığının ortası. Gerçek routing verisi değildir,
 *  yalnızca kuş uçuşu mesafeden kaba bir tahmin üretir. */
const AVERAGE_WALKING_SPEED_KMH = 4.8;

function toRadians(degrees: number): number {
    return (degrees * Math.PI) / 180;
}

/** İki koordinat arasındaki kuş uçuşu (great-circle) mesafeyi metre cinsinden hesaplar. */
export function calculateDistanceMeters(
    lat1: number, lng1: number, lat2: number, lng2: number,
): number {
    const dLat = toRadians(lat2 - lat1);
    const dLng = toRadians(lng2 - lng1);
    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(dLng / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS_M * c;
}

/** Kullanıcı dostu mesafe metni: 1000 m altı metre, üstü tek ondalıklı km. */
export function formatDistance(meters: number): string {
    if (meters < 1000) return `${Math.round(meters)} m`;
    return `${(meters / 1000).toFixed(1)} km`;
}

/** Kuş uçuşu mesafeden kaba bir yürüme süresi tahmini (dakika) — gerçek routing değildir. */
export function estimateWalkingMinutes(meters: number): number {
    const metersPerMinute = (AVERAGE_WALKING_SPEED_KMH * 1000) / 60;
    return Math.max(1, Math.round(meters / metersPerMinute));
}

/** "Tahmini N dk yürüme" — çağıran taraf bunun gerçek routing verisi olmadığını UI'da da
 *  açıkça belirtmeli (bkz. EmergencyAssemblyAreasPage). */
export function formatWalkingEstimate(meters: number): string {
    return `Tahmini ${estimateWalkingMinutes(meters)} dk yürüme`;
}
