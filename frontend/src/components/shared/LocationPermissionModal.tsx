import React, { useState } from 'react';
import { MapPin, Shield, Navigation } from 'lucide-react';
import { updateLocationPermission } from '@/api/users.api';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/components/shared/ToastProvider';
import { startBackgroundLocationUpdate } from '@/services/locationService';

interface Props {
    isOpen: boolean;
    onClose: () => void;
}

export const LocationPermissionModal: React.FC<Props> = ({ isOpen, onClose }) => {
    const { user, accessToken, setAuth } = useAuthStore();
    const toast = useToast();
    const [requesting, setRequesting] = useState(false);

    if (!isOpen) return null;

    const updateStore = (status: string) => {
        if (user && accessToken) {
            setAuth(accessToken, { ...user, locationPermissionStatus: status });
        }
    };

    const handleGrant = () => {
        if (!navigator?.geolocation) {
            updateLocationPermission({ permissionStatus: 'DENIED' }).catch(() => {});
            updateStore('DENIED');
            onClose();
            return;
        }

        setRequesting(true);

        let resolved = false;

        const grantAndClose = () => {
            if (resolved) return;
            resolved = true;
            // İzin durumunu backend'e kaydet (konum yok — arka planda alınacak)
            updateLocationPermission({ permissionStatus: 'GRANTED' }).catch(() => {});
            updateStore('GRANTED');
            // Modal hemen kapanır
            onClose();
            // Yüksek hassasiyetli konum arka planda alınır
            startBackgroundLocationUpdate(toast.success);
        };

        const denyAndClose = () => {
            if (resolved) return;
            resolved = true;
            updateLocationPermission({ permissionStatus: 'DENIED' }).catch(() => {});
            updateStore('DENIED');
            onClose();
        };

        // Permissions API varsa izin diyaloğu kapanır kapanmaz modal kapanır
        navigator.permissions
            .query({ name: 'geolocation' as PermissionName })
            .then((perm) => {
                // İzin zaten verilmiş
                if (perm.state === 'granted') {
                    grantAndClose();
                    return;
                }
                // İzin zaten reddedilmiş
                if (perm.state === 'denied') {
                    denyAndClose();
                    return;
                }

                // 'prompt' — kullanıcı henüz karar vermedi
                // Diyalog kapanır kapanmaz tetiklenecek listener
                const handleChange = () => {
                    perm.removeEventListener('change', handleChange);
                    if (perm.state === 'granted') grantAndClose();
                    else denyAndClose();
                };
                perm.addEventListener('change', handleChange);

                // Tarayıcı izin diyaloğunu aç
                navigator.geolocation.getCurrentPosition(
                    () => {
                        perm.removeEventListener('change', handleChange);
                        grantAndClose();
                    },
                    () => {
                        perm.removeEventListener('change', handleChange);
                        denyAndClose();
                    },
                    { timeout: 30000, maximumAge: 300000, enableHighAccuracy: false },
                );
            })
            .catch(() => {
                // Permissions API desteklenmiyor — basit getCurrentPosition fallback
                navigator.geolocation.getCurrentPosition(
                    () => grantAndClose(),
                    () => denyAndClose(),
                    { timeout: 30000, maximumAge: 300000, enableHighAccuracy: false },
                );
            });
    };

    const handleSkip = () => {
        updateLocationPermission({ permissionStatus: 'SKIPPED' }).catch(() => {});
        updateStore('SKIPPED');
        onClose();
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden">
                <div className="bg-blue-700 px-6 py-5">
                    <div className="flex items-center gap-3">
                        <div className="bg-white bg-opacity-20 rounded-full p-2">
                            <MapPin className="h-6 w-6 text-white" />
                        </div>
                        <div>
                            <h2 className="text-white font-bold text-lg">Konum Paylaşımı</h2>
                            <p className="text-blue-100 text-sm">Afet anında konum bilginizi paylaşın</p>
                        </div>
                    </div>
                </div>

                <div className="p-6 space-y-5">
                    <p className="text-gray-700 text-sm leading-relaxed">
                        Deprem veya afet durumunda acil mesaj gönderdiğinizde, konumunuz yakınlarınıza
                        Google Haritalar bağlantısı olarak iletilebilir.
                    </p>

                    <div className="space-y-3">
                        <div className="flex items-start gap-3 text-sm text-gray-600">
                            <Navigation className="h-4 w-4 text-blue-600 mt-0.5 flex-shrink-0" />
                            <span>Konum bilgisi yalnızca acil mesajlarda kullanılır</span>
                        </div>
                        <div className="flex items-start gap-3 text-sm text-gray-600">
                            <Shield className="h-4 w-4 text-green-600 mt-0.5 flex-shrink-0" />
                            <span>İzin vermek zorunlu değildir; profil sayfasından her zaman değiştirebilirsiniz</span>
                        </div>
                    </div>

                    <div className="space-y-3 pt-2">
                        <button
                            onClick={handleGrant}
                            disabled={requesting}
                            className="w-full py-3 px-4 bg-blue-600 text-white rounded-xl font-semibold hover:bg-blue-700 disabled:opacity-60 transition-colors"
                        >
                            {requesting ? 'İzin isteniyor…' : 'Konum İzni Ver'}
                        </button>
                        <button
                            onClick={handleSkip}
                            disabled={requesting}
                            className="w-full py-3 px-4 bg-gray-100 text-gray-700 rounded-xl font-semibold hover:bg-gray-200 disabled:opacity-60 transition-colors"
                        >
                            Şimdi Değil
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};
