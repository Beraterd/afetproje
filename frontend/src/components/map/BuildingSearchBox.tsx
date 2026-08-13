import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, MapPin } from 'lucide-react';
import { searchBuildings } from '@/api/buildings.api';
import { BuildingSearchResultResponse } from '@/types/building';
import { queryKeys } from '@/utils/queryKeys';
import { useDebounce } from '@/hooks/useDebounce';

interface BuildingSearchBoxProps {
    districtId: string;
    /** Sonuç satırlarında "{mahalle} · {districtName}" göstermek için — verilmezse yalnızca mahalle adı gösterilir. */
    districtName?: string;
    placeholder?: string;
    /** Bir sonuca tıklandığında — SADECE haritayı o konuma götürür, binayı otomatik SEÇMEZ
     *  (kullanıcı binayı haritadan kendi tıklayarak seçmeli, bkz. ürün kararı). */
    onSelectResult: (result: BuildingSearchResultResponse) => void;
    className?: string;
}

/** Adres/sokak arama kutusu — Operasyon Haritası ve Hasar Tespiti bina seçicisi arasında paylaşılır.
 *  Türkçe katlama + sokak/cadde kısaltma eşdeğerliği backend'de yapılır (bkz. TurkishTextNormalizer),
 *  bu component yalnızca debounce + sonuç listesi sunar. */
export const BuildingSearchBox: React.FC<BuildingSearchBoxProps> = ({
    districtId,
    districtName,
    placeholder = 'Mahalle, sokak veya adres ara',
    onSelectResult,
    className,
}) => {
    const [text, setText] = useState('');
    const debounced = useDebounce(text, 400);
    const trimmed = debounced.trim();

    const searchQuery = useQuery({
        queryKey: queryKeys.buildings.search(districtId, trimmed),
        queryFn: () => searchBuildings(trimmed, districtId),
        enabled: trimmed.length >= 3,
    });

    const showDropdown = text.trim().length >= 3;

    return (
        <div className={`relative ${className ?? ''}`}>
            <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
                type="text"
                value={text}
                onChange={(e) => setText(e.target.value)}
                placeholder={placeholder}
                className="w-full pl-8 pr-2 py-1.5 text-sm border border-slate-200 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {showDropdown && (
                <div className="absolute left-0 right-0 top-full mt-1 bg-white border border-slate-200 rounded-md shadow-lg max-h-56 overflow-y-auto z-20">
                    {searchQuery.isLoading && (
                        <p className="text-xs text-slate-400 px-3 py-2">Aranıyor…</p>
                    )}
                    {searchQuery.isSuccess && searchQuery.data.length === 0 && (
                        <p className="text-xs text-slate-400 px-3 py-2">Bu aramayla eşleşen adres bulunamadı.</p>
                    )}
                    {searchQuery.data?.map((r) => (
                        <button
                            key={r.id}
                            type="button"
                            onClick={() => {
                                onSelectResult(r);
                                setText('');
                            }}
                            className="w-full text-left px-3 py-2 text-xs hover:bg-slate-50 border-b border-slate-100 last:border-0 flex items-start gap-1.5"
                        >
                            <MapPin size={12} className="text-slate-400 mt-0.5 shrink-0" />
                            <span>
                                <span className="font-medium text-slate-800">
                                    {r.displayAddress ?? `${r.streetName ?? ''} ${r.buildingNumber ?? ''}`.trim()}
                                </span>
                                <span className="block text-slate-400">
                                    {districtName ? `${r.neighborhoodName} · ${districtName}` : r.neighborhoodName}
                                </span>
                            </span>
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
};
