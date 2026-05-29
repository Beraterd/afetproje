import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getDistricts } from '@/api/districts.api';
import { assignDistrictCoordinator, listEligibleUsers } from '@/api/coordinator.api';
import { queryKeys } from '@/utils/queryKeys';
import { Button, DataTable, ColumnDef, FormField, Badge, Modal, LoadingSpinner } from '@/components/ui';
import { DistrictResponse } from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { MapPin, Settings, User } from 'lucide-react';
import { format } from 'date-fns';

const RISK_COLOR_TR: Record<string, string> = {
    GREEN: 'Düşük',
    YELLOW: 'Orta',
    RED: 'Yüksek',
};

export const DistrictsPage: React.FC = () => {
    const queryClient = useQueryClient();
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedDistrict, setSelectedDistrict] = useState<DistrictResponse | null>(null);

    const { data: districts, isLoading } = useQuery({
        queryKey: queryKeys.districts.all,
        queryFn: getDistricts,
    });

    const columns: ColumnDef<DistrictResponse>[] = [
        {
            header: 'İlçe Adı',
            accessor: 'name',
            render: (row) => (
                <div className="font-medium text-gray-900">{row.name}</div>
            ),
        },
        {
            header: 'Risk Puanı',
            render: (row) => (
                <div className="flex items-center gap-2">
                    <span className="text-sm text-gray-700">{row.riskScore}</span>
                    <span
                        className={`w-2.5 h-2.5 rounded-full ${
                            row.riskColor === 'RED'
                                ? 'bg-red-500'
                                : row.riskColor === 'YELLOW'
                                ? 'bg-yellow-400'
                                : 'bg-green-500'
                        }`}
                    />
                    <span
                        className={`text-xs font-medium ${
                            row.riskColor === 'RED'
                                ? 'text-red-600'
                                : row.riskColor === 'YELLOW'
                                ? 'text-yellow-600'
                                : 'text-green-600'
                        }`}
                    >
                        {RISK_COLOR_TR[row.riskColor] ?? row.riskColor}
                    </span>
                </div>
            ),
        },
        {
            header: 'Son Güncelleme',
            render: (row) => (
                <span className="text-sm text-gray-500">
                    {format(new Date(row.riskScoreUpdatedAt), 'dd.MM.yyyy')}
                </span>
            ),
        },
        {
            header: 'Koordinatör',
            render: (row) =>
                row.coordinator ? (
                    <div className="flex items-center gap-1.5">
                        <User className="h-3.5 w-3.5 text-gray-400 flex-shrink-0" />
                        <span className="text-sm text-gray-900">
                            {row.coordinator.firstName} {row.coordinator.lastName}
                        </span>
                    </div>
                ) : (
                    <Badge variant="warning">Atanmamış</Badge>
                ),
        },
        {
            header: 'İşlemler',
            render: (row) => (
                <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => {
                        setSelectedDistrict(row);
                        setModalOpen(true);
                    }}
                    leftIcon={<Settings className="h-4 w-4" />}
                >
                    Koordinatör Ata
                </Button>
            ),
        },
    ];

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold leading-7 text-gray-900 flex items-center gap-2">
                    <MapPin className="h-6 w-6 text-blue-600" />
                    İlçe Yönetimi
                </h1>
                <p className="mt-1 text-sm text-gray-500">
                    İlçe risk profillerini görüntüleyin ve ilçe koordinatörü atayın. Mahalle koordinatörleri için "Koordinatör Atamaları" sayfasını kullanın.
                </p>
            </div>

            <DataTable
                columns={columns}
                data={districts || []}
                isLoading={isLoading}
                emptyMessage="İlçe bulunamadı."
            />

            <AssignCoordinatorModal
                isOpen={modalOpen}
                onClose={() => {
                    setModalOpen(false);
                    setSelectedDistrict(null);
                }}
                district={selectedDistrict}
                onSuccess={() => {
                    setModalOpen(false);
                    setSelectedDistrict(null);
                    queryClient.invalidateQueries({ queryKey: queryKeys.districts.all });
                }}
            />
        </div>
    );
};

const AssignCoordinatorModal: React.FC<{
    isOpen: boolean;
    onClose: () => void;
    district: DistrictResponse | null;
    onSuccess: () => void;
}> = ({ isOpen, onClose, district, onSuccess }) => {
    const toast = useToast();
    const [search, setSearch] = useState('');
    const [selectedUserId, setSelectedUserId] = useState('');

    const { data: users = [], isLoading: usersLoading } = useQuery({
        queryKey: ['coordinators', 'eligible-users', 'DISTRICT_COORDINATOR', undefined],
        queryFn: () => listEligibleUsers('DISTRICT_COORDINATOR', undefined),
        enabled: isOpen,
    });

    const ROLE_TR: Record<string, string> = {
        DISTRICT_COORDINATOR: 'İlçe Koordinatörü',
        NEIGHBORHOOD_COORDINATOR: 'Mahalle Koordinatörü',
        VOLUNTEER: 'Gönüllü',
    };

    const filtered = users.filter((u: any) => {
        const q = search.toLowerCase();
        return (
            u.firstName.toLowerCase().includes(q) ||
            u.lastName.toLowerCase().includes(q) ||
            u.email.toLowerCase().includes(q)
        );
    });

    const mutation = useMutation({
        mutationFn: () => assignDistrictCoordinator(district!.id, selectedUserId),
        onSuccess: () => {
            toast.success('İlçe koordinatörü başarıyla güncellendi');
            onSuccess();
        },
        onError: (err: any) =>
            toast.error(err.response?.data?.message || err.message || 'Koordinatör atanamadı'),
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedUserId) return toast.error('Lütfen bir kullanıcı seçin');
        if (!district) return;
        mutation.mutate();
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={`İlçe Koordinatörü Ata — ${district?.name ?? ''}`}
        >
            <form onSubmit={handleSubmit} className="space-y-4">
                {district?.coordinator && (
                    <div className="rounded-md bg-blue-50 p-3 text-sm text-blue-700">
                        Mevcut koordinatör:{' '}
                        <strong>
                            {district.coordinator.firstName} {district.coordinator.lastName}
                        </strong>
                    </div>
                )}

                <FormField label="Kullanıcı Ara">
                    <input
                        type="text"
                        placeholder="Ad, soyad veya e-posta..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                    />
                </FormField>

                <div className="max-h-60 overflow-y-auto border rounded-md divide-y">
                    {usersLoading ? (
                        <div className="p-4 flex justify-center">
                            <LoadingSpinner />
                        </div>
                    ) : filtered.length === 0 ? (
                        <div className="p-4 text-center text-sm text-gray-500">
                            {users.length === 0 ? 'Uygun kullanıcı bulunamadı' : 'Arama sonucu bulunamadı'}
                        </div>
                    ) : (
                        filtered.map((u: any) => (
                            <div
                                key={u.id}
                                onClick={() => setSelectedUserId(u.id)}
                                className={`p-3 cursor-pointer hover:bg-blue-50 transition-colors ${
                                    selectedUserId === u.id
                                        ? 'bg-blue-50 border-l-4 border-l-blue-600'
                                        : ''
                                }`}
                            >
                                <div className="text-sm font-medium text-gray-900">
                                    {u.firstName} {u.lastName}
                                </div>
                                <div className="text-xs text-gray-500 mt-0.5">
                                    {u.email}
                                    <span className="ml-2 text-gray-400">
                                        — {ROLE_TR[u.role] ?? u.role}
                                    </span>
                                    {u.district?.name && (
                                        <span className="ml-1 text-gray-400">
                                            ({u.district.name})
                                        </span>
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                </div>

                <div className="pt-4 flex justify-end gap-3 border-t border-gray-200">
                    <Button variant="ghost" onClick={onClose} type="button" disabled={mutation.isPending}>
                        İptal
                    </Button>
                    <Button type="submit" loading={mutation.isPending} disabled={!selectedUserId}>
                        Ata
                    </Button>
                </div>
            </form>
        </Modal>
    );
};
