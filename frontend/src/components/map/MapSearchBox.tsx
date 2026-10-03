import { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, MapPin, Milestone } from 'lucide-react';
import { searchBuildings } from '@/api/buildings.api';
import { searchStreets } from '@/api/streets.api';
import { BuildingSearchResultResponse } from '@/types/building';
import { StreetSearchResultResponse } from '@/types/street';
import { queryKeys } from '@/utils/queryKeys';
import { useDebounce } from '@/hooks/useDebounce';

const STREET_MIN_QUERY_LENGTH = 2;
const BUILDING_MIN_QUERY_LENGTH = 3;

type CombinedResult =
    | { kind: 'street'; data: StreetSearchResultResponse }
    | { kind: 'building'; data: BuildingSearchResultResponse };

interface MapSearchBoxProps {
    districtId: string;
    districtName?: string;
    /** null = henüz mahalle seçilmedi — sokak araması bu context'te devre dışı kalır,
     *  bina araması (mevcut davranış, ilçe-scoped) yine çalışmaya devam eder (item 10). */
    neighborhoodId: string | null;
    placeholder?: string;
    /** Bir bina sonucuna tıklandığında/Enter'a basıldığında — SADECE haritayı o konuma götürür,
     *  binayı otomatik SEÇMEZ (mevcut ürün kararı, bkz. eski BuildingSearchBox). */
    onSelectBuilding: (result: BuildingSearchResultResponse) => void;
    /** Bir sokak sonucuna tıklandığında/Enter'a basıldığında — harita sokağa zoom yapar ve
     *  segmenti highlight eder (item 21-24). */
    onSelectStreet: (result: StreetSearchResultResponse) => void;
    className?: string;
}

/** Operasyon Haritası'nın birleşik arama kutusu — mahalle-scoped gerçek sokak/cadde arama
 *  (item 1-52) + mevcut ilçe-scoped bina/adres arama (BuildingSearchBox'tan devralınan,
 *  DEĞİŞTİRİLMEMİŞ davranış) aynı dropdown'da kategorize sunulur (item 30). Hasar Tespiti bina
 *  seçicisi hâlâ ayrı `BuildingSearchBox`'ı kullanır — bu component yalnızca Operasyon
 *  Haritası'na özgüdür, building search regression riski yok. */
export const MapSearchBox: React.FC<MapSearchBoxProps> = ({
    districtId,
    districtName,
    neighborhoodId,
    placeholder = 'Sokak, cadde veya adres ara',
    onSelectBuilding,
    onSelectStreet,
    className,
}) => {
    const [text, setText] = useState('');
    const [highlightedIndex, setHighlightedIndex] = useState(-1);
    const [closed, setClosed] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);
    const debounced = useDebounce(text, 300);
    const trimmed = debounced.trim();

    const streetQuery = useQuery({
        queryKey: queryKeys.streets.search(districtId, neighborhoodId ?? '', trimmed),
        queryFn: ({ signal }) => searchStreets(trimmed, districtId, neighborhoodId!, undefined, signal),
        enabled: !!neighborhoodId && trimmed.length >= STREET_MIN_QUERY_LENGTH,
    });

    const buildingQuery = useQuery({
        queryKey: queryKeys.buildings.search(districtId, trimmed),
        queryFn: () => searchBuildings(trimmed, districtId),
        enabled: trimmed.length >= BUILDING_MIN_QUERY_LENGTH,
    });

    // Yazmaya devam edince kapalı dropdown'ı tekrar aç, eski vurguyu temizle.
    useEffect(() => {
        setClosed(false);
        setHighlightedIndex(-1);
    }, [text]);

    const streets = neighborhoodId ? (streetQuery.data ?? []) : [];
    const buildings = buildingQuery.data ?? [];
    const combined: CombinedResult[] = [
        ...streets.map((s): CombinedResult => ({ kind: 'street', data: s })),
        ...buildings.map((b): CombinedResult => ({ kind: 'building', data: b })),
    ];

    const showDropdown = !closed && trimmed.length >= STREET_MIN_QUERY_LENGTH;
    const isLoading =
        (!!neighborhoodId && trimmed.length >= STREET_MIN_QUERY_LENGTH && streetQuery.isLoading) ||
        (trimmed.length >= BUILDING_MIN_QUERY_LENGTH && buildingQuery.isLoading);
    const settledEmpty =
        (!neighborhoodId || streetQuery.isSuccess) &&
        (trimmed.length < BUILDING_MIN_QUERY_LENGTH || buildingQuery.isSuccess) &&
        !isLoading && combined.length === 0;

    const selectResult = (item: CombinedResult) => {
        if (item.kind === 'street') onSelectStreet(item.data);
        else onSelectBuilding(item.data);
        setText('');
        setHighlightedIndex(-1);
        setClosed(true);
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'ArrowDown') {
            if (!combined.length) return;
            e.preventDefault();
            setClosed(false);
            setHighlightedIndex((i) => Math.min(i + 1, combined.length - 1));
        } else if (e.key === 'ArrowUp') {
            if (!combined.length) return;
            e.preventDefault();
            setClosed(false);
            setHighlightedIndex((i) => Math.max(i - 1, 0));
        } else if (e.key === 'Enter') {
            if (!trimmed) return; // item 19: boş sorguda hiçbir şey yapma
            e.preventDefault();
            const target = highlightedIndex >= 0 ? combined[highlightedIndex] : combined[0];
            if (target) selectResult(target);
        } else if (e.key === 'Escape') {
            setClosed(true);
            setHighlightedIndex(-1);
            inputRef.current?.blur();
        }
    };

    const listboxId = 'map-search-listbox';

    return (
        <div className={`relative ${className ?? ''}`}>
            <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
                ref={inputRef}
                type="text"
                role="combobox"
                aria-expanded={showDropdown}
                aria-controls={listboxId}
                aria-autocomplete="list"
                value={text}
                onChange={(e) => setText(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder={placeholder}
                className="w-full pl-8 pr-2 py-1.5 text-sm border border-slate-200 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {showDropdown && (
                <div
                    id={listboxId}
                    role="listbox"
                    className="absolute left-0 right-0 top-full mt-1 bg-white border border-slate-200 rounded-md shadow-lg max-h-72 overflow-y-auto z-20"
                >
                    {!neighborhoodId && (
                        <p className="text-xs text-slate-400 px-3 py-2 border-b border-slate-100">
                            Sokak aramak için önce bir mahalle seçin.
                        </p>
                    )}

                    {isLoading && <p className="text-xs text-slate-400 px-3 py-2">Aranıyor…</p>}

                    {!isLoading && streets.length > 0 && (
                        <>
                            <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400 px-3 pt-2 pb-1">Sokaklar</p>
                            {streets.map((s) => {
                                const idx = combined.findIndex((c) => c.kind === 'street' && c.data.id === s.id);
                                return (
                                    <button
                                        key={s.id}
                                        type="button"
                                        role="option"
                                        aria-selected={idx === highlightedIndex}
                                        onMouseEnter={() => setHighlightedIndex(idx)}
                                        onClick={() => selectResult({ kind: 'street', data: s })}
                                        className={`w-full text-left px-3 py-2 text-xs border-b border-slate-100 last:border-0 flex items-start gap-1.5 ${
                                            idx === highlightedIndex ? 'bg-blue-50' : 'hover:bg-slate-50'
                                        }`}
                                    >
                                        <Milestone size={12} className="text-slate-400 mt-0.5 shrink-0" />
                                        <span>
                                            <span className="font-medium text-slate-800">{s.name}</span>
                                            <span className="block text-slate-400">
                                                {districtName ? `${s.neighborhoodName} · ${districtName}` : s.neighborhoodName}
                                            </span>
                                        </span>
                                    </button>
                                );
                            })}
                        </>
                    )}

                    {!isLoading && buildings.length > 0 && (
                        <>
                            <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400 px-3 pt-2 pb-1">Binalar / Adresler</p>
                            {buildings.map((b) => {
                                const idx = combined.findIndex((c) => c.kind === 'building' && c.data.id === b.id);
                                return (
                                    <button
                                        key={b.id}
                                        type="button"
                                        role="option"
                                        aria-selected={idx === highlightedIndex}
                                        onMouseEnter={() => setHighlightedIndex(idx)}
                                        onClick={() => selectResult({ kind: 'building', data: b })}
                                        className={`w-full text-left px-3 py-2 text-xs border-b border-slate-100 last:border-0 flex items-start gap-1.5 ${
                                            idx === highlightedIndex ? 'bg-blue-50' : 'hover:bg-slate-50'
                                        }`}
                                    >
                                        <MapPin size={12} className="text-slate-400 mt-0.5 shrink-0" />
                                        <span>
                                            <span className="font-medium text-slate-800">
                                                {b.displayAddress ?? `${b.streetName ?? ''} ${b.buildingNumber ?? ''}`.trim()}
                                            </span>
                                            <span className="block text-slate-400">
                                                {districtName ? `${b.neighborhoodName} · ${districtName}` : b.neighborhoodName}
                                            </span>
                                        </span>
                                    </button>
                                );
                            })}
                        </>
                    )}

                    {settledEmpty && (
                        <p className="text-xs text-slate-400 px-3 py-2">
                            {neighborhoodId ? 'Bu mahallede eşleşen sokak veya adres bulunamadı.' : 'Bu aramayla eşleşen konum bulunamadı.'}
                        </p>
                    )}
                </div>
            )}
        </div>
    );
};
