export type LocationSource = 'GPS' | 'Network' | 'Cell' | 'Unknown';

export interface HighAccuracyResult {
    latitude: number;
    longitude: number;
    accuracy: number;
    altitude: number | null;
    heading: number | null;
    speed: number | null;
    timestamp: number;
    source: LocationSource;
}

export function deriveSource(accuracy: number): LocationSource {
    if (accuracy <= 50) return 'GPS';
    if (accuracy <= 300) return 'Network';
    if (accuracy <= 2000) return 'Cell';
    return 'Unknown';
}

// PositionOptions — her iki fonksiyonda da aynı ayarlar
const GEO_OPTIONS: PositionOptions = {
    enableHighAccuracy: true,
    timeout: 15000,
    maximumAge: 0,
};

const WATCH_DURATION_MS = 12000;  // 12 saniye: GPS fix için yeterli süre
const EARLY_STOP_ACCURACY_M = 20; // bu hassasiyete ulaşılırsa erken dur
export const ACCURACY_REJECT_M = 500;   // bu değerin üstü backend'e gönderilmez
export const ACCURACY_WARNING_M = 100;  // bu değerin üstü acil mesajda uyarı verir

function logReading(index: number, pos: GeolocationPosition, isBest: boolean): void {
    const c = pos.coords;
    console.log(`[Geolocation] Okuma #${index + 1}${isBest ? ' ★ EN İYİ' : ''}`, {
        latitude: c.latitude.toFixed(7),
        longitude: c.longitude.toFixed(7),
        accuracy: `±${Math.round(c.accuracy)} m`,
        source: deriveSource(c.accuracy),
        altitude: c.altitude != null ? `${c.altitude.toFixed(1)} m` : null,
        heading: c.heading,
        speed: c.speed,
        timestamp: new Date(pos.timestamp).toISOString(),
    });
}

/**
 * watchPosition ile WATCH_DURATION_MS boyunca okuma yapar.
 * Tüm okumaları loglar. En düşük accuracy'li ölçümü döner.
 * accuracy > ACCURACY_REJECT_M ise ACCURACY_TOO_LOW:{value} hatasıyla reddeder.
 */
export function getHighAccuracyPosition(): Promise<HighAccuracyResult> {
    return new Promise((resolve, reject) => {
        if (!navigator?.geolocation) {
            reject(new Error('Tarayıcınız konum servislerini desteklemiyor.'));
            return;
        }

        let bestPos: GeolocationPosition | null = null;
        let readingCount = 0;
        let watchId: number | null = null;
        let timer: ReturnType<typeof setTimeout> | null = null;
        let settled = false;

        const finish = () => {
            if (settled) return;
            settled = true;
            if (timer !== null) clearTimeout(timer);
            if (watchId !== null) navigator.geolocation.clearWatch(watchId);

            if (!bestPos) {
                console.warn('[Geolocation] Hiç okuma alınamadı — reddedildi');
                reject(new Error('Konum alınamadı.'));
                return;
            }

            const c = bestPos.coords;
            if (c.accuracy > ACCURACY_REJECT_M) {
                console.warn(
                    `[Geolocation] En iyi ölçüm ±${Math.round(c.accuracy)} m — ${ACCURACY_REJECT_M} m eşiği aşıldı, backend'e GÖNDERİLMEDİ`,
                );
                reject(new Error(`ACCURACY_TOO_LOW:${Math.round(c.accuracy)}`));
                return;
            }

            console.log(
                `[Geolocation] Seçilen ölçüm: ±${Math.round(c.accuracy)} m (${deriveSource(c.accuracy)}) — backend'e GÖNDERİLİYOR`,
            );

            resolve({
                latitude: c.latitude,
                longitude: c.longitude,
                accuracy: c.accuracy,
                altitude: c.altitude,
                heading: c.heading,
                speed: c.speed,
                timestamp: bestPos.timestamp,
                source: deriveSource(c.accuracy),
            });
        };

        console.log(`[Geolocation] watchPosition başladı (${WATCH_DURATION_MS / 1000} s, enableHighAccuracy: true)`);

        watchId = navigator.geolocation.watchPosition(
            (pos) => {
                const isBest = !bestPos || pos.coords.accuracy < bestPos.coords.accuracy;
                logReading(readingCount, pos, isBest);
                readingCount++;

                if (isBest) bestPos = pos;

                if (pos.coords.accuracy <= EARLY_STOP_ACCURACY_M) {
                    console.log(`[Geolocation] ±${Math.round(pos.coords.accuracy)} m ≤ ${EARLY_STOP_ACCURACY_M} m — erken dur`);
                    finish();
                }
            },
            (err) => {
                if (settled) return;
                settled = true;
                if (timer !== null) clearTimeout(timer);
                if (watchId !== null) navigator.geolocation.clearWatch(watchId);
                console.warn('[Geolocation] Hata:', err.code, err.message);
                reject(err);
            },
            GEO_OPTIONS,
        );

        timer = setTimeout(() => {
            console.log(`[Geolocation] ${WATCH_DURATION_MS / 1000} s doldu (${readingCount} okuma)`);
            finish();
        }, WATCH_DURATION_MS);
    });
}

/**
 * Acil durum için hız öncelikli tek ölçüm (getCurrentPosition).
 * enableHighAccuracy: true — accuracy değeri caller tarafından kontrol edilmeli.
 */
export function getQuickPosition(): Promise<HighAccuracyResult> {
    return new Promise((resolve, reject) => {
        if (!navigator?.geolocation) {
            reject(new Error('Tarayıcınız konum servislerini desteklemiyor.'));
            return;
        }

        console.log('[Geolocation] getCurrentPosition başladı (enableHighAccuracy: true)');

        navigator.geolocation.getCurrentPosition(
            (pos) => {
                logReading(0, pos, true);
                const c = pos.coords;
                resolve({
                    latitude: c.latitude,
                    longitude: c.longitude,
                    accuracy: c.accuracy,
                    altitude: c.altitude,
                    heading: c.heading,
                    speed: c.speed,
                    timestamp: pos.timestamp,
                    source: deriveSource(c.accuracy),
                });
            },
            (err) => {
                console.warn('[Geolocation] Hata:', err.code, err.message);
                reject(err);
            },
            GEO_OPTIONS,
        );
    });
}

export function accuracyLabel(accuracy: number): string {
    if (accuracy <= ACCURACY_WARNING_M) return `±${Math.round(accuracy)} m`;
    if (accuracy <= ACCURACY_REJECT_M)
        return `±${Math.round(accuracy)} m — Düşük Hassasiyet`;
    return `±${Math.round(accuracy)} m — Kullanılamaz`;
}
