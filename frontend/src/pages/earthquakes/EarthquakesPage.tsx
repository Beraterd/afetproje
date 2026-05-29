import React, { useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getEarthquakes, syncEarthquakes } from '@/api/earthquakes.api';
import { queryKeys } from '@/utils/queryKeys';
import { Badge, Button, DataTable, ColumnDef, LoadingSpinner } from '@/components/ui';
import { EarthquakeEventResponse, EarthquakeRiskLevel } from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';
import { Activity, RefreshCw } from 'lucide-react';

const SYNC_INTERVAL_MS = 30_000;
// API'den UTC string gelir ("...Z" veya "+00:00"). Browser TZ'den bağımsız olarak
// Turkey Standard Time (UTC+3, sabit — 2016'dan beri DST yok) olarak göster.
// Format: DD-MM-YYYY HH:mm:ss — AFAD resmi sitesi Tarih(TS) kolonu ile birebir.
const formatIstanbul = (isoString: string): string => {
    const utcMs = new Date(isoString).getTime();
    const istMs = utcMs + 3 * 60 * 60 * 1000;   // UTC → TSİ (+3h)
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

export const EarthquakesPage: React.FC = () => {
    const user = useAuthStore((s) => s.user);
    const toast = useToast();
    const queryClient = useQueryClient();
    const [page, setPage] = useState(0);

    const isFetchingRef = useRef(false);

    const { data, isLoading, isError } = useQuery({
        queryKey: queryKeys.earthquakes.list({ page, size: 20 }),
        queryFn: async () => {
            if (isFetchingRef.current) return undefined as never;
            isFetchingRef.current = true;
            try {
                return await getEarthquakes({ page, size: 20 });
            } finally {
                isFetchingRef.current = false;
            }
        },
        refetchOnMount: true,
        refetchInterval: SYNC_INTERVAL_MS,
        refetchIntervalInBackground: false,
    });

    const syncMutation = useMutation({
        mutationFn: () => syncEarthquakes(24),
        onSuccess: (result) => {
            const detail = [
                `+${result.savedCount ?? result.newEventsCount} yeni`,
                `${result.skippedDuplicateCount} tekrar`,
                result.durationMs ? `${result.durationMs}ms` : null,
            ].filter(Boolean).join(' · ');
            toast.success(`Sync tamamlandı: ${detail}`);
            queryClient.invalidateQueries({ queryKey: queryKeys.earthquakes.all });
        },
        onError: () => toast.error('Senkronizasyon başarısız'),
    });

    const columns: ColumnDef<EarthquakeEventResponse>[] = [
        {
            header: 'EventID',
            accessor: 'externalId',
            render: (row) => (
                <span className="font-mono text-xs text-gray-500">{row.externalId}</span>
            ),
        },
        {
            header: 'Büyüklük',
            accessor: 'magnitude',
            render: (row) => (
                <span className="text-lg font-bold text-gray-900">{row.magnitude.toFixed(1)}</span>
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
                    {(row.province || row.district) && (
                        <div className="text-xs text-gray-500">
                            {[row.province, row.district].filter(Boolean).join(' / ')}
                        </div>
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
                <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700">
                    {row.source}
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
                        AFAD Depremleri
                    </h1>
                    <p className="mt-1 text-sm text-gray-500">
                        AFAD Deprem Dairesi'nden alınan gerçek zamanlı deprem verileri.
                        Her 30 saniyede otomatik güncellenir.
                    </p>
                </div>
                {user?.role === 'ADMIN' && (
                    <div className="mt-4 sm:mt-0">
                        <Button
                            onClick={() => syncMutation.mutate()}
                            loading={syncMutation.isPending}
                            leftIcon={<RefreshCw className="h-4 w-4" />}
                            variant="secondary"
                        >
                            Manuel Senkronizasyon
                        </Button>
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

            <DataTable
                columns={columns}
                data={data?.content || []}
                isLoading={isLoading}
                emptyMessage="Kayıtlı deprem verisi bulunamadı. Admin, Manuel Senkronizasyon ile AFAD'dan veri çekebilir."
                pagination={data ? { page: data.number, totalPages: data.totalPages } : undefined}
                onPageChange={setPage}
            />
        </div>
    );
};
