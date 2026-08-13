import React from 'react';
import type { MapDistrictResponse, MapNeighborhoodResponse } from '@/types';

const RISK_DOT_CLASS: Record<string, string> = {
    GREEN: 'bg-green-500',
    YELLOW: 'bg-yellow-500',
    RED: 'bg-red-500',
    PURPLE: 'bg-violet-600',
};

interface MapBreadcrumbProps {
    districts?: MapDistrictResponse[];
    neighborhoods?: MapNeighborhoodResponse[];
    activeDistrictId: string | null;
    activeNeighborhoodId: string | null;
    activeDistrictName?: string;
    activeNeighborhoodName?: string;
    showDistrictList: boolean;
    onToggleDistrictList: () => void;
    onSelectDistrict: (district: MapDistrictResponse) => void;
    onSelectNeighborhood: (nb: MapNeighborhoodResponse) => void;
    onBackToCity: () => void;
    onBackToDistrict: () => void;
}

/** Harita üstündeki gezinme şeridi: risk lejantı, ilçe listesi (poligonu olmayan ilçeler için)
 *  ve şehir/ilçe/mahalle breadcrumb'ı. Bunların hepsi "şu an haritada neredeyim, nasıl geri
 *  dönerim" sorusuna hizmet ettiği için tek component'te toplandı. */
export const MapBreadcrumb: React.FC<MapBreadcrumbProps> = ({
    districts, neighborhoods, activeDistrictId, activeNeighborhoodId,
    activeDistrictName, activeNeighborhoodName, showDistrictList,
    onToggleDistrictList, onSelectDistrict, onSelectNeighborhood,
    onBackToCity, onBackToDistrict,
}) => {
    const activeDistrictHasPolygon = !!districts?.find((d) => d.id === activeDistrictId)?.polygon;

    return (
        <div className="space-y-3">
            <div className="mb-1 sm:flex sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:tracking-tight">
                        Operasyon Paneli
                    </h1>
                    <p className="mt-1 text-sm text-gray-500">
                        İstanbul geneli risk haritası ve saha operasyon görünümü.
                    </p>
                </div>
                <div className="flex flex-wrap gap-3 items-center mt-2 sm:mt-0">
                    <div className="flex items-center space-x-3 text-sm">
                        <span className="flex items-center gap-1"><span className={`w-3 h-3 rounded-full inline-block ${RISK_DOT_CLASS.GREEN}`} />Düşük</span>
                        <span className="flex items-center gap-1"><span className={`w-3 h-3 rounded-full inline-block ${RISK_DOT_CLASS.YELLOW}`} />Orta</span>
                        <span className="flex items-center gap-1"><span className={`w-3 h-3 rounded-full inline-block ${RISK_DOT_CLASS.RED}`} />Yüksek</span>
                        <span className="flex items-center gap-1"><span className={`w-3 h-3 rounded-full inline-block ${RISK_DOT_CLASS.PURPLE}`} />Kritik</span>
                    </div>
                    {districts && districts.length > 0 && (
                        <button
                            onClick={onToggleDistrictList}
                            aria-expanded={showDistrictList}
                            className="text-sm px-3 py-1.5 rounded-md border border-gray-300 bg-white hover:bg-gray-50 font-medium"
                        >
                            {showDistrictList ? 'Listeyi Kapat' : `İlçe Listesi (${districts.length})`}
                        </button>
                    )}
                    <div className="flex items-center gap-2 text-sm">
                        {activeDistrictId && (
                            <button onClick={onBackToCity} className="font-medium text-blue-600 hover:text-blue-500">
                                ← Şehir Görünümü
                            </button>
                        )}
                        {activeNeighborhoodId && (
                            <>
                                <span className="text-gray-400">/</span>
                                <button onClick={onBackToDistrict} className="font-medium text-blue-600 hover:text-blue-500">
                                    İlçe Görünümü
                                </button>
                                <span className="text-gray-400">/</span>
                                <span className="text-gray-700 font-medium">{activeNeighborhoodName}</span>
                            </>
                        )}
                    </div>
                </div>
            </div>

            {showDistrictList && districts && (
                <div className="p-3 bg-white border border-gray-200 rounded-lg shadow-sm max-h-48 overflow-y-auto">
                    <p className="text-xs text-gray-500 mb-2">
                        Haritada polygon olmayan ilçeler (sınır verisi bekleniyor) listeden seçilebilir.
                    </p>
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-1">
                        {[...districts].sort((a, b) => a.name.localeCompare(b.name, 'tr')).map((d) => (
                            <button
                                key={d.id}
                                onClick={() => onSelectDistrict(d)}
                                className={`text-left text-xs px-2 py-1.5 rounded border transition-colors ${
                                    activeDistrictId === d.id
                                        ? 'bg-blue-600 text-white border-blue-600'
                                        : 'bg-white hover:bg-gray-50 border-gray-200'
                                }`}
                            >
                                <span className={`inline-block w-2 h-2 rounded-full mr-1 ${RISK_DOT_CLASS[d.riskColor] ?? RISK_DOT_CLASS.PURPLE}`} />
                                {d.name}
                                {!d.polygon && <span className="text-gray-400 ml-1">○</span>}
                            </button>
                        ))}
                    </div>
                </div>
            )}

            {activeDistrictId && neighborhoods && neighborhoods.length > 0 && !activeDistrictHasPolygon && (
                <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                    <p className="text-xs font-medium text-blue-800 mb-2">{activeDistrictName} — Mahalleler</p>
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-1">
                        {neighborhoods.map((nb) => (
                            <button
                                key={nb.id}
                                onClick={() => onSelectNeighborhood(nb)}
                                className={`text-left text-xs px-2 py-1 rounded border ${
                                    activeNeighborhoodId === nb.id
                                        ? 'bg-blue-600 text-white border-blue-600'
                                        : 'bg-white hover:bg-gray-50 border-gray-200'
                                }`}
                            >
                                <span className={`inline-block w-2 h-2 rounded-full mr-1 ${RISK_DOT_CLASS[nb.riskColor] ?? RISK_DOT_CLASS.PURPLE}`} />
                                {nb.name}
                            </button>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};
