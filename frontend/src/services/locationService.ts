import { useAuthStore } from '@/store/authStore';
import { updateLocationPermission } from '@/api/users.api';
import { getHighAccuracyPosition } from '@/utils/geolocation';

/**
 * Yüksek hassasiyetli konumu arka planda alır ve backend'e kaydeder.
 * Hiçbir şekilde kullanıcıyı bekletmez — fire-and-forget.
 *
 * @param onSuccess - Başarılı kaydedilince çağrılır (toast vb. için)
 */
export function startBackgroundLocationUpdate(
    onSuccess?: (msg: string) => void,
): void {
    getHighAccuracyPosition()
        .then(async (pos) => {
            try {
                await updateLocationPermission({
                    permissionStatus: 'GRANTED',
                    latitude: pos.latitude,
                    longitude: pos.longitude,
                    accuracy: pos.accuracy,
                    locationSource: pos.source,
                });

                // Store'daki locationPermissionStatus'u güncelle
                const { user, accessToken, setAuth } = useAuthStore.getState();
                if (user && accessToken) {
                    setAuth(accessToken, { ...user, locationPermissionStatus: 'GRANTED' });
                }

                onSuccess?.('Konumunuz güncellendi.');
            } catch (saveErr) {
                console.debug('[Location] Arka plan kayıt hatası:', saveErr);
            }
        })
        .catch((err) => {
            const msg = err instanceof Error ? err.message : String(err);
            if (msg.startsWith('ACCURACY_TOO_LOW:')) {
                console.debug(
                    `[Location] Arka plan: hassasiyet düşük (±${msg.split(':')[1]} m), kaydedilmedi`,
                );
            } else {
                console.debug('[Location] Arka plan konum alınamadı:', msg);
            }
        });
}
