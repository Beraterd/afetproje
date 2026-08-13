import { useEffect, useState } from 'react';
import { MapPin, Navigation, AlertTriangle, Loader2, LocateFixed } from 'lucide-react';
import { getMyAssemblyAreas } from '@/api/emergencyContacts.api';
import { MyAssemblyArea } from '@/types';
import { getQuickPosition } from '@/utils/geolocation';
import { calculateDistanceMeters, formatDistance, formatWalkingEstimate } from '@/utils/distance';

type LocationStatus = 'idle' | 'loading' | 'granted' | 'denied' | 'unavailable';

interface AreaWithDistance extends MyAssemblyArea {
    distanceMeters: number | null;
}

function buildNavUrl(area: MyAssemblyArea): string | null {
    if (area.googleMapsUrl) return area.googleMapsUrl;
    if (area.latitude != null && area.longitude != null) {
        return `https://www.google.com/maps/dir/?api=1&destination=${area.latitude},${area.longitude}`;
    }
    return null;
}

/**
 * Acil durum / toplanma alanları sayfası.
 * Giriş yapmış kullanıcının mahallesindeki aktif/onaylı toplanma alanlarını listeler.
 * WhatsApp/SMS bildirimindeki kısa link bu sayfaya yönlendirir.
 * Mobil öncelikli tasarım.
 */
export function EmergencyAssemblyAreasPage() {
    const [areas, setAreas] = useState<MyAssemblyArea[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    const [locationStatus, setLocationStatus] = useState<LocationStatus>('idle');
    const [userCoords, setUserCoords] = useState<{ lat: number; lng: number } | null>(null);

    useEffect(() => {
        let mounted = true;
        getMyAssemblyAreas()
            .then(data => {
                if (mounted) setAreas(data);
            })
            .catch(() => {
                if (mounted) setError(true);
            })
            .finally(() => {
                if (mounted) setLoading(false);
            });
        return () => {
            mounted = false;
        };
    }, []);

    const requestLocation = () => {
        if (!navigator?.geolocation) {
            setLocationStatus('unavailable');
            return;
        }
        setLocationStatus('loading');
        // Tek seferlik konum yeterli — sürekli watchPosition/pil tüketen polling gerekmiyor.
        getQuickPosition()
            .then((pos) => {
                setUserCoords({ lat: pos.latitude, lng: pos.longitude });
                setLocationStatus('granted');
            })
            .catch(() => {
                setLocationStatus('denied');
            });
    };

    // Sayfa açılışında konum bir kez otomatik denenir: tarayıcı zaten daha önce izin verdiyse
    // sessizce sonuçlanır, karar verilmemişse tarayıcının TEK SEFERLİK native izin diyaloğu
    // çıkar. Reddedilirse bir daha OTOMATİK istenmez — kullanıcı isterse aşağıdaki "Konumumu
    // Kullan" butonuyla kendi isteğiyle tekrar deneyebilir.
    useEffect(() => {
        requestLocation();
    }, []);

    const areasWithDistance: AreaWithDistance[] = areas.map((area) => ({
        ...area,
        distanceMeters:
            userCoords && area.latitude != null && area.longitude != null
                ? calculateDistanceMeters(userCoords.lat, userCoords.lng, area.latitude, area.longitude)
                : null,
    }));

    // Mesafesi bilinen alanlar yakınlık sırasına göre; mesafesi olmayanlar sona düşer.
    const sortedAreas = userCoords
        ? [...areasWithDistance].sort((a, b) => {
              if (a.distanceMeters == null) return 1;
              if (b.distanceMeters == null) return -1;
              return a.distanceMeters - b.distanceMeters;
          })
        : areasWithDistance;

    const nearestId = sortedAreas.find((a) => a.distanceMeters != null)?.id;

    return (
        <div className="min-h-screen bg-gray-50">
            <div className="max-w-xl mx-auto px-4 py-5 space-y-4">
                {/* Header */}
                <div className="bg-red-600 text-white rounded-xl p-4 shadow-sm">
                    <div className="flex items-center gap-2">
                        <AlertTriangle className="h-6 w-6 flex-shrink-0" />
                        <h1 className="text-lg font-bold">Acil Durum — Toplanma Alanları</h1>
                    </div>
                    <p className="text-sm text-red-100 mt-1">
                        Mahallenizdeki en yakın güvenli toplanma alanlarına yönelin. Yetkili
                        duyurularını (AFAD) takip etmeyi unutmayın.
                    </p>
                </div>

                {/* Konum izni durumu — asla agresif tekrar istek yok, yalnızca pasif bilgi + manuel buton */}
                {!loading && areas.length > 0 && locationStatus !== 'granted' && locationStatus !== 'loading' && (
                    <div className="bg-blue-50 border border-blue-200 rounded-xl p-3 flex items-start gap-2.5">
                        <LocateFixed className="h-4 w-4 text-blue-600 flex-shrink-0 mt-0.5" />
                        <div className="flex-1 min-w-0">
                            <p className="text-xs text-blue-800">
                                {locationStatus === 'unavailable'
                                    ? 'Tarayıcınız konum servislerini desteklemiyor.'
                                    : 'Konum izni verirseniz size en yakın alanı gösterebiliriz.'}
                            </p>
                            {locationStatus !== 'unavailable' && (
                                <button
                                    onClick={requestLocation}
                                    className="text-xs font-medium text-blue-700 underline mt-1"
                                >
                                    Konumumu Kullan
                                </button>
                            )}
                        </div>
                    </div>
                )}

                {locationStatus === 'loading' && (
                    <div className="flex items-center gap-2 text-xs text-gray-500 px-1">
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        Konumunuz alınıyor...
                    </div>
                )}

                {loading && (
                    <div className="flex items-center justify-center py-16 text-gray-500">
                        <Loader2 className="h-6 w-6 animate-spin mr-2" />
                        Toplanma alanları yükleniyor...
                    </div>
                )}

                {!loading && error && (
                    <div className="bg-white border border-gray-200 rounded-xl p-6 text-center">
                        <p className="text-sm text-gray-600">
                            Toplanma alanları yüklenemedi. Lütfen tekrar deneyin.
                        </p>
                    </div>
                )}

                {!loading && !error && areas.length === 0 && (
                    <div className="bg-white border border-gray-200 rounded-xl p-6 text-center">
                        <MapPin className="h-10 w-10 text-gray-300 mx-auto mb-2" />
                        <p className="text-sm text-gray-600">
                            Mahallenize ait kayıtlı toplanma alanı bulunamadı.
                        </p>
                        <p className="text-xs text-gray-400 mt-1">
                            Profil bilgilerinizde mahalle seçili olduğundan emin olun.
                        </p>
                    </div>
                )}

                {!loading && !error && areas.length > 0 && (
                    <div className="space-y-3">
                        {sortedAreas.map((area) => {
                            const navUrl = buildNavUrl(area);
                            const isNearest = area.id === nearestId;
                            return (
                                <div
                                    key={area.id}
                                    className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm"
                                >
                                    <div className="flex items-start gap-3">
                                        <div className="h-10 w-10 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
                                            <MapPin className="h-5 w-5 text-red-600" />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center gap-2 flex-wrap">
                                                <h2 className="text-base font-semibold text-gray-900 break-words">
                                                    {area.name}
                                                </h2>
                                                {isNearest && (
                                                    <span className="inline-flex items-center text-[11px] font-semibold bg-green-100 text-green-800 rounded-full px-2 py-0.5">
                                                        En Yakın
                                                    </span>
                                                )}
                                            </div>
                                            {area.distanceMeters != null ? (
                                                <p className="text-sm text-gray-500 mt-0.5">
                                                    {formatDistance(area.distanceMeters)} · {formatWalkingEstimate(area.distanceMeters)}
                                                </p>
                                            ) : area.address ? (
                                                <p className="text-sm text-gray-500 mt-0.5 break-words">
                                                    {area.address}
                                                </p>
                                            ) : null}
                                        </div>
                                    </div>

                                    {navUrl ? (
                                        <a
                                            href={navUrl}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="mt-3 w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-medium px-4 py-3 rounded-lg transition-colors"
                                        >
                                            <Navigation className="h-5 w-5" />
                                            Yol Tarifi Al
                                        </a>
                                    ) : (
                                        <p className="mt-3 text-xs text-gray-400 text-center">
                                            Bu alan için konum bilgisi mevcut değil.
                                        </p>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}
