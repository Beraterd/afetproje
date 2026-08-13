import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { EarthquakeMap } from '@/components/earthquakes/EarthquakeMap';
import { SOURCE_BADGE_CLASSES, SOURCE_LABELS } from '@/utils/earthquakeSourceLabels';
import {
    getEarthquakes,
    getEarthquakeProvinces,
    getEarthquakeDistricts,
    getEarthquakeDebugStatus,
    syncEarthquakes,
    syncKandilliEarthquakes,
} from '@/api/earthquakes.api';
import { queryKeys } from '@/utils/queryKeys';
import { Badge, Button, DataTable, ColumnDef } from '@/components/ui';
import { EarthquakeEventResponse, EarthquakeRiskLevel, EarthquakeSource } from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';
import { getApiErrorMessage } from '@/utils/errorParser';
import { Activity, RefreshCw, X } from 'lucide-react';

const SYNC_INTERVAL_MS = 30_000;
const PAGE_SIZE = 20;

// API'den UTC string gelir ("...Z" veya "+00:00"). Browser TZ'den bağımsız olarak
// Turkey Standard Time (UTC+3, sabit — 2016'dan beri DST yok) olarak göster.
const formatIstanbul = (isoString: string): string => {
    const utcMs = new Date(isoString).getTime();
    const istMs = utcMs + 3 * 60 * 60 * 1000;
    const d = new Date(istMs);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${p(d.getUTCDate())}-${p(d.getUTCMonth() + 1)}-${d.getUTCFullYear()} ${p(d.getUTCHours())}:${p(d.getUTCMinutes())}:${p(d.getUTCSeconds())}`;
};

const RISK_VARIANTS: Record<EarthquakeRiskLevel, 'neutral' | 'info' | 'warning' | 'danger'> = {
    LOW: 'neutral',
    MEDIUM: 'info',
    HIGH: 'warning',
    CRITICAL: 'danger',
};

const RISK_LABELS: Record<EarthquakeRiskLevel, string> = {
    LOW: 'Düşük',
    MEDIUM: 'Orta',
    HIGH: 'Yüksek',
    CRITICAL: 'Kritik',
};

const MAGNITUDE_OPTIONS = [2, 3, 4, 5, 6];

export const EarthquakesPage: React.FC = () => {
    const user = useAuthStore((s) => s.user);
    const isAdmin = user?.role === 'ADMIN';
    const toast = useToast();
    const queryClient = useQueryClient();
    const [searchParams, setSearchParams] = useSearchParams();
    const [page, setPage] = useState(0);

    const source = (searchParams.get('source') as EarthquakeSource | null) ?? undefined;
    const province = searchParams.get('province') ?? undefined;
    const district = searchParams.get('district') ?? undefined;
    const minMagnitudeParam = searchParams.get('minMagnitude');
    const minMagnitude = minMagnitudeParam ? Number(minMagnitudeParam) : undefined;

    // Filtre değiştiğinde sayfa 0'a döner (yeni bir sonuç kümesi = yeni sayfalama).
    useEffect(() => {
        setPage(0);
    }, [source, province, district, minMagnitude]);

    const updateParam = (key: string, value: string | undefined) => {
        const next = new URLSearchParams(searchParams);
        if (value) {
            next.set(key, value);
        } else {
            next.delete(key);
        }
        // İl değişince önceki ilçe seçimi state'te KALMAMALI — farklı ile ait olabilir.
        if (key === 'province') {
            next.delete('district');
        }
        setSearchParams(next, { replace: true });
    };

    const clearAllFilters = () => setSearchParams(new URLSearchParams(), { replace: true });
    const hasActiveFilters = !!(source || province || district || minMagnitude);

    const isFetchingRef = useRef(false);

    const filterKey = { page, size: PAGE_SIZE, source, province, district, minMagnitude };

    const { data, isLoading, isError } = useQuery({
        queryKey: queryKeys.earthquakes.list(filterKey),
        queryFn: async () => {
            if (isFetchingRef.current) return undefined as never;
            isFetchingRef.current = true;
            try {
                return await getEarthquakes({ page, size: PAGE_SIZE, source, province, district, minMagnitude });
            } finally {
                isFetchingRef.current = false;
            }
        },
        refetchOnMount: true,
        refetchInterval: SYNC_INTERVAL_MS,
        refetchIntervalInBackground: false,
    });

    const { data: provinces = [] } = useQuery({
        queryKey: ['earthquakes', 'provinces'],
        queryFn: getEarthquakeProvinces,
        staleTime: Infinity, // 81 il listesi pratikte hiç değişmez
    });

    const { data: districts = [] } = useQuery({
        queryKey: ['earthquakes', 'districts', province],
        queryFn: () => getEarthquakeDistricts(province!),
        enabled: !!province,
    });

    // Yalnızca ADMIN — sync durumu/son güncelleme zamanı debug/status ile aynı yetki kapsamında.
    const { data: debugStatus } = useQuery({
        queryKey: ['earthquakes', 'debug-status'],
        queryFn: getEarthquakeDebugStatus,
        enabled: isAdmin,
        refetchInterval: isAdmin ? SYNC_INTERVAL_MS : false,
    });

    const syncMutation = useMutation({
        mutationFn: () => syncEarthquakes(24),
        onSuccess: (result) => {
            const detail = [
                `+${result.savedCount ?? result.newEventsCount} yeni`,
                `${result.skippedDuplicateCount} tekrar`,
                result.durationMs ? `${result.durationMs}ms` : null,
            ].filter(Boolean).join(' · ');
            toast.success(`AFAD senkronizasyonu tamamlandı: ${detail}`);
            queryClient.invalidateQueries({ queryKey: queryKeys.earthquakes.all });
        },
        onError: (err) => toast.error(getApiErrorMessage(err, 'AFAD senkronizasyonu başarısız')),
    });

    const syncKandilliMutation = useMutation({
        mutationFn: () => syncKandilliEarthquakes(24),
        onSuccess: (result) => {
            const detail = [
                `+${result.savedCount ?? result.newEventsCount} yeni`,
                `${result.skippedDuplicateCount} tekrar`,
                result.durationMs ? `${result.durationMs}ms` : null,
            ].filter(Boolean).join(' · ');
            toast.success(`Kandilli senkronizasyonu tamamlandı: ${detail}`);
            queryClient.invalidateQueries({ queryKey: queryKeys.earthquakes.all });
        },
        onError: (err) => toast.error(getApiErrorMessage(err, 'Kandilli senkronizasyonu başarısız')),
    });

    const mapPoints = useMemo(
        () => (data?.content ?? []).filter((e) => e.latitude != null && e.longitude != null),
        [data]
    );

    const columns: ColumnDef<EarthquakeEventResponse>[] = [
        {
            header: 'Büyüklük',
            accessor: 'magnitude',
            render: (row) => (
                <span className="text-lg font-bold text-gray-900">
                    {row.magnitude.toFixed(1)}
                    {row.magnitudeType && <span className="ml-1 text-xs font-normal text-gray-400">{row.magnitudeType}</span>}
                </span>
            ),
        },
        {
            header: 'Risk',
            accessor: 'riskLevel',
            render: (row) => (
                <Badge variant={RISK_VARIANTS[row.riskLevel]}>
                    {RISK_LABELS[row.riskLevel]}
                </Badge>
            ),
        },
        {
            header: 'Konum',
            accessor: 'location',
            render: (row) => (
                <div>
                    <div className="font-medium text-gray-900">{row.location || '-'}</div>
                    {(row.province || row.district) ? (
                        <div className="text-xs text-gray-500">
                            {[row.province, row.district].filter(Boolean).join(' / ')}
                        </div>
                    ) : (
                        <div className="text-xs text-gray-400 italic">Konum belirlenemedi</div>
                    )}
                </div>
            ),
        },
        {
            header: 'Derinlik (km)',
            accessor: 'depth',
            render: (row) => (row.depth != null ? row.depth.toFixed(1) : '-'),
        },
        {
            header: 'Tarih / Saat',
            accessor: 'eventTime',
            render: (row) => (
                <span className="text-gray-500 text-sm">
                    {formatIstanbul(row.eventTime)}
                </span>
            ),
        },
        {
            header: 'Kaynak',
            accessor: 'source',
            render: (row) => (
                <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${SOURCE_BADGE_CLASSES[row.source]}`}>
                    {SOURCE_LABELS[row.source]}
                </span>
            ),
        },
    ];

    return (
        <div className="space-y-6">
            <div className="sm:flex sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-bold leading-7 text-gray-900 flex items-center gap-2">
                        <Activity className="h-6 w-6 text-red-600" />
                        Depremler
                    </h1>
                    <p className="mt-1 text-sm text-gray-500">
                        AFAD ve Kandilli Rasathanesi kaynaklarından alınan gerçek zamanlı deprem verileri.
                        Her 30 saniyede otomatik güncellenir.
                    </p>
                </div>
                {isAdmin && (
                    <div className="mt-4 sm:mt-0 flex flex-wrap gap-2">
                        <Button
                            onClick={() => syncMutation.mutate()}
                            loading={syncMutation.isPending}
                            leftIcon={<RefreshCw className="h-4 w-4" />}
                            variant="secondary"
                            size="sm"
                        >
                            AFAD Senkronize Et
                        </Button>
                        <Button
                            onClick={() => syncKandilliMutation.mutate()}
                            loading={syncKandilliMutation.isPending}
                            leftIcon={<RefreshCw className="h-4 w-4" />}
                            variant="secondary"
                            size="sm"
                        >
                            Kandilli Senkronize Et
                        </Button>
                    </div>
                )}
            </div>

            {/* Item 24 — kaynak bilgisi/uyarı: kullanıcı kurumlar arası farkı bug sanmamalı. */}
            <div className="rounded-lg bg-gray-50 border border-gray-200 px-4 py-3 text-xs text-gray-600">
                Deprem verileri AFAD ve Kandilli Rasathanesi kaynaklarından alınmaktadır. Kurumlar arasında
                büyüklük, derinlik ve konum değerlerinde farklılıklar görülebilir; bu normaldir.
            </div>

            {isAdmin && debugStatus?.kandilliLastError && (
                <div className="rounded-lg bg-amber-50 border border-amber-200 px-4 py-3 text-sm text-amber-800">
                    Kandilli verileri şu anda güncellenemiyor ({debugStatus.kandilliLastError}). Son alınan
                    veriler gösterilmeye devam ediyor, AFAD verileri etkilenmedi.
                </div>
            )}

            {/* Filtreler */}
            <div className="glass-card p-4">
                <div className="flex flex-wrap gap-3 items-end">
                    <div>
                        <label htmlFor="eq-filter-source" className="block text-xs font-medium text-gray-500 mb-1">Kaynak</label>
                        <select
                            id="eq-filter-source"
                            className="border border-gray-300 rounded-lg px-3 py-2 text-sm min-w-[140px]"
                            value={source ?? ''}
                            onChange={(e) => updateParam('source', e.target.value || undefined)}
                        >
                            <option value="">Tüm Kaynaklar</option>
                            <option value="AFAD">AFAD</option>
                            <option value="KANDILLI">Kandilli</option>
                        </select>
                    </div>

                    <div>
                        <label htmlFor="eq-filter-province" className="block text-xs font-medium text-gray-500 mb-1">İl</label>
                        <select
                            id="eq-filter-province"
                            className="border border-gray-300 rounded-lg px-3 py-2 text-sm min-w-[160px]"
                            value={province ?? ''}
                            onChange={(e) => updateParam('province', e.target.value || undefined)}
                        >
                            <option value="">Tüm İller</option>
                            {provinces.map((p) => (
                                <option key={p} value={p}>{p}</option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label htmlFor="eq-filter-district" className="block text-xs font-medium text-gray-500 mb-1">İlçe</label>
                        <select
                            id="eq-filter-district"
                            className="border border-gray-300 rounded-lg px-3 py-2 text-sm min-w-[160px] disabled:bg-gray-100 disabled:text-gray-400"
                            value={district ?? ''}
                            disabled={!province}
                            onChange={(e) => updateParam('district', e.target.value || undefined)}
                        >
                            <option value="">Tüm İlçeler</option>
                            {districts.map((d) => (
                                <option key={d} value={d}>{d}</option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label htmlFor="eq-filter-magnitude" className="block text-xs font-medium text-gray-500 mb-1">Min. Büyüklük</label>
                        <select
                            id="eq-filter-magnitude"
                            className="border border-gray-300 rounded-lg px-3 py-2 text-sm min-w-[120px]"
                            value={minMagnitude ?? ''}
                            onChange={(e) => updateParam('minMagnitude', e.target.value || undefined)}
                        >
                            <option value="">Tümü</option>
                            {MAGNITUDE_OPTIONS.map((m) => (
                                <option key={m} value={m}>{m}+</option>
                            ))}
                        </select>
                    </div>
                </div>

                {hasActiveFilters && (
                    <div className="flex flex-wrap items-center gap-2 mt-3 pt-3 border-t border-gray-100">
                        {source && <FilterChip label={SOURCE_LABELS[source]} onRemove={() => updateParam('source', undefined)} />}
                        {province && <FilterChip label={province} onRemove={() => updateParam('province', undefined)} />}
                        {district && <FilterChip label={district} onRemove={() => updateParam('district', undefined)} />}
                        {minMagnitude && <FilterChip label={`${minMagnitude}+`} onRemove={() => updateParam('minMagnitude', undefined)} />}
                        <button
                            onClick={clearAllFilters}
                            className="text-xs font-medium text-brand-700 hover:text-brand-900 ml-1"
                        >
                            Filtreleri Temizle
                        </button>
                    </div>
                )}
            </div>

            {isError && (
                <div className="glass-card p-4 border-l-4 border-red-400">
                    <p className="text-sm text-red-700">
                        Deprem verileri yüklenirken hata oluştu. Lütfen sayfayı yenileyin.
                    </p>
                </div>
            )}

            {/* Harita — liste ile AYNI filtrelenmiş veri kümesini kullanır (item 12). */}
            <EarthquakeMap points={mapPoints} />

            <DataTable
                columns={columns}
                data={data?.content || []}
                isLoading={isLoading}
                emptyMessage={
                    hasActiveFilters
                        ? 'Bu filtrelerle eşleşen deprem kaydı bulunamadı.'
                        : "Kayıtlı deprem verisi bulunamadı. Admin, manuel senkronizasyon ile veri çekebilir."
                }
                pagination={data ? { page: data.number, totalPages: data.totalPages } : undefined}
                onPageChange={setPage}
            />
        </div>
    );
};

const FilterChip: React.FC<{ label: string; onRemove: () => void }> = ({ label, onRemove }) => (
    <span className="inline-flex items-center gap-1 rounded-full bg-brand-50 text-brand-700 text-xs font-medium px-2.5 py-1">
        {label}
        <button onClick={onRemove} aria-label={`${label} filtresini kaldır`} className="hover:text-brand-900">
            <X className="h-3 w-3" />
        </button>
    </span>
);
