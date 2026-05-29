import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { getSimulationById, getSimulationLogs } from '@/api/simulations.api';
import { queryKeys } from '@/utils/queryKeys';
import { LoadingSpinner, Badge, DataTable, ColumnDef } from '@/components/ui';
import { SimulationLogResponse } from '@/types';
import { ShieldAlert, Users, CheckCircle, XCircle } from 'lucide-react';
import { format } from 'date-fns';

const LOG_STATUS_TR: Record<string, string> = {
    QUEUED: 'Kuyrukta',
    SENT: 'Gönderildi',
    FAILED: 'Başarısız',
    BOUNCED: 'Geri Döndü',
};

export const SimulationDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const [page, setPage] = useState(0);
    const [statusFilter, setStatusFilter] = useState('');

    const { data: simulation, isLoading: loadingSim } = useQuery({
        queryKey: queryKeys.simulations.detail(id!),
        queryFn: () => getSimulationById(id!),
        enabled: !!id,
    });

    const { data: logs, isLoading: loadingLogs } = useQuery({
        queryKey: queryKeys.simulations.logs(id!, statusFilter),
        queryFn: () => getSimulationLogs(id!, { page, size: 10, status: statusFilter || undefined }),
        enabled: !!id,
    });

    if (loadingSim) return <LoadingSpinner label="Simülasyon detayları yükleniyor..." />;
    if (!simulation) return <div className="text-red-600">Simülasyon yüklenemedi.</div>;

    const columns: ColumnDef<SimulationLogResponse>[] = [
        {
            header: 'E-posta Adresi',
            accessor: 'emailAddress',
            render: (row) => <div className="font-medium text-gray-900">{row.emailAddress}</div>,
        },
        {
            header: 'Durum',
            accessor: 'status',
            render: (row) => {
                const isSuccess = row.status === 'SENT';
                return (
                    <Badge variant={isSuccess ? 'success' : 'danger'}>
                        {LOG_STATUS_TR[row.status] || row.status}
                    </Badge>
                );
            },
        },
        {
            header: 'Yeniden Deneme',
            accessor: 'retryCount',
        },
        {
            header: 'Gönderim Zamanı',
            render: (row) => (
                <span className="text-gray-500">
                    {row.sentAt ? format(new Date(row.sentAt), 'dd.MM.yyyy HH:mm') : '-'}
                </span>
            ),
        },
        {
            header: 'Hata Detayı',
            render: (row) => (
                <span className="text-xs text-red-600 max-w-xs block truncate" title={row.lastError}>
                    {row.lastError || '-'}
                </span>
            ),
        },
    ];

    return (
        <div className="max-w-5xl mx-auto space-y-6">
            <div className="bg-white px-4 py-5 shadow sm:rounded-lg sm:px-6 border border-gray-200">
                <h3 className="text-lg font-bold leading-6 text-gray-900 flex items-center">
                    <ShieldAlert className="h-5 w-5 mr-2 text-red-600" />
                    Simülasyon Raporu: {simulation.district.name}
                </h3>
                <p className="mt-1 text-sm text-gray-500">
                    {simulation.createdBy.firstName} {simulation.createdBy.lastName} tarafından{' '}
                    {format(new Date(simulation.triggeredAt), 'dd.MM.yyyy')} tarihinde tetiklendi
                </p>

                <dl className="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-4 border-t border-gray-200 pt-5">
                    <div className="overflow-hidden rounded-lg bg-gray-50 px-4 py-5 shadow sm:p-6">
                        <dt className="truncate text-sm font-medium text-gray-500">Büyüklük</dt>
                        <dd className="mt-1 text-3xl font-semibold tracking-tight text-red-600">
                            {simulation.magnitude}
                        </dd>
                    </div>
                    <div className="overflow-hidden rounded-lg bg-gray-50 px-4 py-5 shadow sm:p-6">
                        <dt className="truncate text-sm font-medium text-gray-500 flex items-center">
                            <Users className="w-4 h-4 mr-2" /> Kuyruktaki
                        </dt>
                        <dd className="mt-1 text-3xl font-semibold tracking-tight text-gray-900">
                            {simulation.totalQueued}
                        </dd>
                    </div>
                    <div className="overflow-hidden rounded-lg bg-gray-50 px-4 py-5 shadow sm:p-6 text-green-700">
                        <dt className="truncate text-sm font-medium flex items-center">
                            <CheckCircle className="w-4 h-4 mr-2" /> Gönderildi
                        </dt>
                        <dd className="mt-1 text-3xl font-semibold tracking-tight">
                            {simulation.totalSent}
                        </dd>
                    </div>
                    <div className="overflow-hidden rounded-lg bg-gray-50 px-4 py-5 shadow sm:p-6 text-red-700">
                        <dt className="truncate text-sm font-medium flex items-center">
                            <XCircle className="w-4 h-4 mr-2" /> Başarısız
                        </dt>
                        <dd className="mt-1 text-3xl font-semibold tracking-tight">
                            {simulation.totalFailed}
                        </dd>
                    </div>
                </dl>
            </div>

            <div className="bg-white shadow sm:rounded-lg overflow-hidden border border-gray-200 pt-5">
                <div className="px-4 sm:px-6 mb-4 flex justify-between items-center">
                    <h3 className="text-lg font-medium leading-6 text-gray-900">E-posta Gönderim Kayıtları</h3>
                    <div className="w-48">
                        <select
                            value={statusFilter}
                            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
                            className="block w-full rounded-md border-0 py-1.5 pl-3 pr-10 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm sm:leading-6"
                        >
                            <option value="">Tüm Durumlar</option>
                            <option value="SENT">Gönderildi</option>
                            <option value="FAILED">Başarısız</option>
                            <option value="QUEUED">Kuyrukta</option>
                        </select>
                    </div>
                </div>

                <DataTable
                    columns={columns}
                    data={logs?.content || []}
                    isLoading={loadingLogs}
                    emptyMessage="Mevcut filtrelere göre kayıt bulunamadı."
                    pagination={logs ? { page: logs.number, totalPages: logs.totalPages } : undefined}
                    onPageChange={setPage}
                />
            </div>
        </div>
    );
};
