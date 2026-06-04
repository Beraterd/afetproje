import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getEvents } from '@/api/events.api';
import { getTeams } from '@/api/teams.api';
import { getDistricts } from '@/api/districts.api';
import { queryKeys } from '@/utils/queryKeys';
import { DataTable, ColumnDef, Button, Badge, FormField } from '@/components/ui';
import { EventSummaryResponse, EventStatus } from '@/types';
import { Plus } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';

const TEAM_TR: Record<string, string> = {
    SEARCH_RESCUE: 'Arama Kurtarma Ekibi',
    FOOD_WATER: 'Yemek ve İçme Suyu Dağıtım Ekibi',
    EVACUATION: 'Tahliye Ekibi',
    COMMUNICATION: 'İletişim Ekibi',
    PSYCHOSOCIAL: 'Psikososyal Destek Ekibi',
    HASAR_TESPIT_EKIBI: 'Hasar Tespit Ekibi',
    OTHER: 'Diğer',
};

function statusBadge(status: EventStatus) {
    switch (status) {
        case 'OPEN':
        case 'IN_PROGRESS': return <Badge variant="warning">Devam Ediyor</Badge>;
        case 'COMPLETED': return <Badge variant="info">Tamamlandı</Badge>;
        case 'CLOSED': return <Badge variant="neutral">İptal Edildi</Badge>;
        default: return <Badge variant="neutral">{status}</Badge>;
    }
}

export const EventsPage: React.FC = () => {
    const navigate = useNavigate();
    const user = useAuthStore((s) => s.user);

    const [page, setPage] = useState(0);
    const [statusFilter, setStatusFilter] = useState<EventStatus | ''>('');
    const [districtFilter, setDistrictFilter] = useState('');
    const [teamFilter, setTeamFilter] = useState('');

    const { data, isLoading, isError } = useQuery({
        queryKey: queryKeys.events.list({ page, status: statusFilter, districtId: districtFilter, teamId: teamFilter }),
        queryFn: () => getEvents({
            page,
            size: 10,
            status: statusFilter || undefined,
            districtId: districtFilter || undefined,
            teamId: teamFilter || undefined,
        }),
    });

    const { data: teams } = useQuery({
        queryKey: ['teams'],
        queryFn: getTeams,
        staleTime: 5 * 60 * 1000,
    });

    // Only ADMINs need the district filter (coordinators auto-scope to their own)
    const isAdmin = user?.role === 'ADMIN';
    const { data: districts } = useQuery({
        queryKey: ['districts'],
        queryFn: getDistricts,
        enabled: isAdmin,
        staleTime: 5 * 60 * 1000,
    });

    const columns: ColumnDef<EventSummaryResponse>[] = [
        {
            header: 'İsim',
            render: (row) => (
                <span className="text-sm font-medium text-gray-900">
                    {row.eventTeamCode || row.team?.teamCode || '-'}
                </span>
            ),
        },
        {
            header: 'Olay',
            accessor: 'title',
            render: (row) => <div className="font-medium text-gray-900">{row.title}</div>,
        },
        {
            header: 'Durum',
            accessor: 'status',
            render: (row) => statusBadge(row.status),
        },
        {
            header: 'Ekip',
            render: (row) => (
                <span className="text-sm text-gray-700">
                    {row.team ? (TEAM_TR[row.team.name] || row.team.name) : '-'}
                </span>
            ),
        },
        {
            header: 'Konum',
            render: (row) => (
                <div className="text-xs text-gray-500">
                    {row.neighborhood?.districtName} / {row.neighborhood?.name}
                </div>
            ),
        },
        {
            header: 'Gerekli Kişi',
            render: (row) => <span className="text-sm text-gray-700">{row.requiredPeople} kişi</span>,
        },
        {
            header: 'İşlemler',
            render: (row) => (
                <Button variant="secondary" size="sm" onClick={() => navigate(`/events/${row.id}`)}>
                    Görüntüle
                </Button>
            ),
        },
    ];

    const canCreate = user && ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'].includes(user.role);

    return (
        <div className="space-y-6">
            <div className="sm:flex sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:tracking-tight">
                        Ekip Durumu
                    </h1>
                    <p className="mt-1 text-sm text-gray-500">
                        Ekip ihtiyacı olan tüm olayların ve görev atamalarının listesi.
                    </p>
                </div>
                <div className="mt-4 sm:ml-16 sm:mt-0 sm:flex-none">
                    {canCreate && (
                        <Button onClick={() => navigate('/events/create')} leftIcon={<Plus className="h-4 w-4" />}>
                            Olay Oluştur
                        </Button>
                    )}
                </div>
            </div>

            <div className="flex flex-wrap items-end gap-4 bg-white p-4 shadow rounded-lg mb-4">
                <FormField label="Durum" className="w-44">
                    <select
                        value={statusFilter}
                        onChange={(e) => { setStatusFilter(e.target.value as EventStatus | ''); setPage(0); }}
                        className="block w-full rounded-md border-0 py-1.5 pl-3 pr-10 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm sm:leading-6"
                    >
                        <option value="">Tüm Durumlar</option>
                        <option value="IN_PROGRESS">Devam Ediyor</option>
                        <option value="COMPLETED">Tamamlandı</option>
                        <option value="CLOSED">İptal Edildi</option>
                    </select>
                </FormField>

                {isAdmin && districts && (
                    <FormField label="İlçe" className="w-48">
                        <select
                            value={districtFilter}
                            onChange={(e) => { setDistrictFilter(e.target.value); setPage(0); }}
                            className="block w-full rounded-md border-0 py-1.5 pl-3 pr-10 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm sm:leading-6"
                        >
                            <option value="">Tüm İlçeler</option>
                            {districts.map((d: any) => (
                                <option key={d.id} value={d.id}>{d.name}</option>
                            ))}
                        </select>
                    </FormField>
                )}

                {teams && (
                    <FormField label="Ekip Türü" className="w-52">
                        <select
                            value={teamFilter}
                            onChange={(e) => { setTeamFilter(e.target.value); setPage(0); }}
                            className="block w-full rounded-md border-0 py-1.5 pl-3 pr-10 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm sm:leading-6"
                        >
                            <option value="">Tüm Ekipler</option>
                            {teams.map((t: any) => (
                                <option key={t.id} value={t.id}>
                                    {t.teamCode ? `${t.teamCode} — ${TEAM_TR[t.name] || t.name}` : (TEAM_TR[t.name] || t.name)}
                                </option>
                            ))}
                        </select>
                    </FormField>
                )}
            </div>

            {isError ? (
                <div className="bg-red-50 p-4 rounded-md">
                    <h3 className="text-sm font-medium text-red-800">Olaylar yüklenirken hata oluştu. Lütfen daha sonra tekrar deneyin.</h3>
                </div>
            ) : (
                <DataTable
                    columns={columns}
                    data={data?.content || []}
                    isLoading={isLoading}
                    emptyMessage="Hiç olay bulunamadı."
                    pagination={data ? { page: data.number, totalPages: data.totalPages } : undefined}
                    onPageChange={setPage}
                />
            )}
        </div>
    );
};
