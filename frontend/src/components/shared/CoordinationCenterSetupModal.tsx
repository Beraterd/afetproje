import React from 'react';
import { useNavigate } from 'react-router-dom';
import { MapPin, X, AlertTriangle } from 'lucide-react';

interface Props {
    isOpen: boolean;
    role: 'DISTRICT_COORDINATOR' | 'NEIGHBORHOOD_COORDINATOR';
    districtName?: string;
    neighborhoodName?: string;
    onDismiss: () => void;
}

export const CoordinationCenterSetupModal: React.FC<Props> = ({
    isOpen,
    role,
    districtName,
    neighborhoodName,
    onDismiss,
}) => {
    const navigate = useNavigate();

    if (!isOpen) return null;

    const isDistrict = role === 'DISTRICT_COORDINATOR';
    const areaName = isDistrict ? districtName : neighborhoodName;
    const areaLabel = isDistrict ? 'ilçe' : 'mahalle';

    const handleSetNow = () => {
        onDismiss();
        navigate('/coordination-center');
    };

    return (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-gray-900/60 backdrop-blur-sm"
                onClick={onDismiss}
            />

            {/* Modal */}
            <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl overflow-hidden">
                {/* Header */}
                <div className="bg-amber-50 border-b border-amber-100 px-6 py-5 flex items-start gap-4">
                    <div className="flex-shrink-0 w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center">
                        <AlertTriangle className="h-5 w-5 text-amber-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                        <h2 className="text-base font-semibold text-gray-900">
                            Koordinatörlük Merkezi Tanımlı Değil
                        </h2>
                        {areaName && (
                            <p className="text-sm text-amber-700 mt-0.5 font-medium">{areaName}</p>
                        )}
                    </div>
                    <button
                        onClick={onDismiss}
                        className="flex-shrink-0 text-gray-400 hover:text-gray-600 transition-colors"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* Body */}
                <div className="px-6 py-5">
                    <p className="text-sm text-gray-700 leading-relaxed">
                        <strong>{areaName ?? (isDistrict ? 'İlçenizin' : 'Mahallenizin')}</strong>{' '}
                        koordinatörlük merkezinin fiziksel konumu henüz tanımlanmamış.
                    </p>
                    <p className="text-sm text-gray-600 leading-relaxed mt-2">
                        Saha operasyonları sırasında diğer koordinatörler ve gönüllüler bu konumu
                        kullanır. Lütfen {areaLabel} koordinatörlük merkezinin adresini ve harita
                        konumunu belirleyin.
                    </p>

                    <div className="mt-4 bg-blue-50 rounded-lg px-4 py-3 text-xs text-blue-700">
                        <span className="font-semibold">Operasyon merkezi nedir?</span> Bu alan,
                        koordinatörlerin toplandığı ve operasyonların yürütüldüğü fiziksel noktadır.
                        Kişisel adresinizden farklı olabilir.
                    </div>
                </div>

                {/* Footer */}
                <div className="px-6 pb-6 flex flex-col-reverse sm:flex-row gap-3">
                    <button
                        onClick={onDismiss}
                        className="flex-1 px-4 py-2.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                    >
                        Daha Sonra
                    </button>
                    <button
                        onClick={handleSetNow}
                        className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors"
                    >
                        <MapPin className="h-4 w-4" />
                        Şimdi Konum Gir
                    </button>
                </div>
            </div>
        </div>
    );
};
