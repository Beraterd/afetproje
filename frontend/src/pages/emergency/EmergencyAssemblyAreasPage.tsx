import { useEffect, useState } from 'react';
import { MapPin, Navigation, AlertTriangle, Loader2 } from 'lucide-react';
import { getMyAssemblyAreas } from '@/api/emergencyContacts.api';
import { MyAssemblyArea } from '@/types';

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

    const buildNavUrl = (area: MyAssemblyArea): string | null => {
        if (area.googleMapsUrl) return area.googleMapsUrl;
        if (area.latitude != null && area.longitude != null) {
            return `https://www.google.com/maps/dir/?api=1&destination=${area.latitude},${area.longitude}`;
        }
        return null;
    };

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
                        {areas.map(area => {
                            const navUrl = buildNavUrl(area);
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
                                            <h2 className="text-base font-semibold text-gray-900 break-words">
                                                {area.name}
                                            </h2>
                                            {area.address && (
                                                <p className="text-sm text-gray-500 mt-0.5 break-words">
                                                    {area.address}
                                                </p>
                                            )}
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
