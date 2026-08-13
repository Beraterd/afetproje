import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, X, Loader2 } from 'lucide-react';
import { searchGlobal } from '@/api/search.api';
import { useDebounce } from '@/hooks/useDebounce';
import { getSearchResultTarget, SEARCH_CATEGORY_LABEL_TR } from '@/utils/searchResultTarget';
import type { SearchResultItem, SearchResultType } from '@/types';

const MIN_QUERY_LENGTH = 2;

function groupByType(results: SearchResultItem[]): Array<[SearchResultType, SearchResultItem[]]> {
    const groups = new Map<SearchResultType, SearchResultItem[]>();
    for (const item of results) {
        const list = groups.get(item.type) ?? [];
        list.push(item);
        groups.set(item.type, list);
    }
    return Array.from(groups.entries());
}

/**
 * Header'daki global arama — Olay/Hasar Tespiti/Ekip/Kullanıcı/Kaynak Talebi/Mahalle-İlçe
 * üzerinde arama yapar. Yetkilendirme/kapsam backend'de uygulanır; burada yalnızca sunum var.
 */
export const GlobalSearch: React.FC<{ className?: string }> = ({ className }) => {
    const navigate = useNavigate();
    const containerRef = useRef<HTMLDivElement>(null);
    const [query, setQuery] = useState('');
    const [open, setOpen] = useState(false);
    const [activeIndex, setActiveIndex] = useState(-1);
    const debouncedQuery = useDebounce(query.trim(), 350);
    const enabled = debouncedQuery.length >= MIN_QUERY_LENGTH;

    const { data, isFetching, isError } = useQuery({
        queryKey: ['globalSearch', debouncedQuery],
        queryFn: () => searchGlobal(debouncedQuery),
        enabled,
        staleTime: 30_000,
    });

    const results = useMemo(() => data?.results ?? [], [data]);
    const grouped = useMemo(() => groupByType(results), [results]);
    const flatResults = useMemo(() => grouped.flatMap(([, items]) => items), [grouped]);

    useEffect(() => {
        setActiveIndex(-1);
    }, [debouncedQuery]);

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
                setOpen(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const goToResult = (item: SearchResultItem) => {
        const target = getSearchResultTarget(item);
        setOpen(false);
        setQuery('');
        if (target) navigate(target);
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Escape') {
            setOpen(false);
            return;
        }
        if (!open || flatResults.length === 0) return;
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setActiveIndex((i) => (i + 1) % flatResults.length);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setActiveIndex((i) => (i <= 0 ? flatResults.length - 1 : i - 1));
        } else if (e.key === 'Enter') {
            e.preventDefault();
            const target = activeIndex >= 0 ? flatResults[activeIndex] : flatResults[0];
            if (target) goToResult(target);
        }
    };

    const showDropdown = open && debouncedQuery.length > 0;

    return (
        <div ref={containerRef} className={className ?? 'relative w-full max-w-md'}>
            <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" aria-hidden="true" />
                <input
                    type="text"
                    role="combobox"
                    aria-expanded={showDropdown}
                    aria-controls="global-search-results"
                    aria-label="Uygulama genelinde ara"
                    placeholder="Olay, hasar, ekip, kullanıcı, kaynak talebi, mahalle ara…"
                    value={query}
                    onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
                    onFocus={() => setOpen(true)}
                    onKeyDown={handleKeyDown}
                    className="w-full rounded-full border border-gray-200 bg-white/80 pl-9 pr-8 py-2 text-sm text-gray-700 placeholder:text-gray-400 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                />
                {query && (
                    <button
                        type="button"
                        aria-label="Aramayı temizle"
                        onClick={() => { setQuery(''); setOpen(false); }}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                        <X className="h-4 w-4" />
                    </button>
                )}
            </div>

            {showDropdown && (
                <div
                    id="global-search-results"
                    role="listbox"
                    className="absolute z-[1600] mt-2 w-full max-h-96 overflow-y-auto rounded-xl border border-gray-200 bg-white shadow-lg"
                >
                    {debouncedQuery.length < MIN_QUERY_LENGTH ? (
                        <p className="px-4 py-3 text-sm text-gray-400">En az {MIN_QUERY_LENGTH} karakter yazın…</p>
                    ) : isFetching ? (
                        <div className="flex items-center gap-2 px-4 py-3 text-sm text-gray-500">
                            <Loader2 className="h-4 w-4 animate-spin" />
                            Aranıyor…
                        </div>
                    ) : isError ? (
                        <p className="px-4 py-3 text-sm text-red-600">Arama şu anda kullanılamıyor.</p>
                    ) : results.length === 0 ? (
                        <p className="px-4 py-3 text-sm text-gray-400">Sonuç bulunamadı.</p>
                    ) : (
                        grouped.map(([type, items]) => (
                            <div key={type} className="border-b border-gray-100 last:border-b-0">
                                <p className="px-4 pt-2.5 pb-1 text-xs font-semibold uppercase tracking-wide text-gray-400">
                                    {SEARCH_CATEGORY_LABEL_TR[type]}
                                </p>
                                <ul>
                                    {items.map((item) => {
                                        const flatIndex = flatResults.indexOf(item);
                                        const active = flatIndex === activeIndex;
                                        return (
                                            <li key={`${item.type}-${item.id}`} role="option" aria-selected={active}>
                                                <button
                                                    type="button"
                                                    onMouseEnter={() => setActiveIndex(flatIndex)}
                                                    onClick={() => goToResult(item)}
                                                    className={`w-full text-left px-4 py-2 text-sm ${active ? 'bg-brand-50' : 'hover:bg-gray-50'}`}
                                                >
                                                    <div className="font-medium text-gray-900">{item.title}</div>
                                                    {(item.subtitle || item.statusLabel) && (
                                                        <div className="text-xs text-gray-500">
                                                            {item.subtitle}
                                                            {item.subtitle && item.statusLabel ? ' · ' : ''}
                                                            {item.statusLabel}
                                                        </div>
                                                    )}
                                                </button>
                                            </li>
                                        );
                                    })}
                                </ul>
                            </div>
                        ))
                    )}
                </div>
            )}
        </div>
    );
};
