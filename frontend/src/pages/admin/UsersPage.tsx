import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getUsers, assignRole, deleteUser, adminUpdateUser } from '@/api/users.api';
import { getDistricts } from '@/api/districts.api';
import { getNeighborhoods } from '@/api/neighborhoods.api';
import { queryKeys } from '@/utils/queryKeys';
import { Button, DataTable, ColumnDef, FormField, ConfirmationDialog } from '@/components/ui';
import { UserResponse, Role } from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { Users, Shield, Pencil, Trash2 } from 'lucide-react';

/** Bu e-postalar korunuyor — silinemez. */
const PROTECTED_EMAILS = ['admin.gmail.com', 'admin@afetkoordinasyon.istanbul'];

const ROLE_TR: Record<string, string> = {
    ADMIN: 'Yönetici',
    DISTRICT_COORDINATOR: 'İlçe Koordinatörü',
    NEIGHBORHOOD_COORDINATOR: 'Mahalle Koordinatörü',
    VOLUNTEER: 'Gönüllü',
};

interface EditFormState {
    firstName: string;
    lastName: string;
    email: string;
    districtId: string;
    neighborhoodId: string;
}

export const UsersPage: React.FC = () => {
    const queryClient = useQueryClient();
    const toast = useToast();

    const [page, setPage] = useState(0);
    const [roleFilter, setRoleFilter] = useState<Role | ''>('');

    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

    // Edit modal state
    const [editModalOpen, setEditModalOpen] = useState(false);
    const [editUser, setEditUser] = useState<UserResponse | null>(null);
    const [editForm, setEditForm] = useState<EditFormState>({ firstName: '', lastName: '', email: '', districtId: '', neighborhoodId: '' });
    const [editDistricts, setEditDistricts] = useState<any[]>([]);
    const [editNeighborhoods, setEditNeighborhoods] = useState<any[]>([]);

    const { data, isLoading } = useQuery({
        queryKey: queryKeys.users.list({ page, role: roleFilter }),
        queryFn: () => getUsers({ page, size: 10, role: roleFilter || undefined }),
    });

    // Load districts for edit modal
    useEffect(() => {
        if (editModalOpen) {
            getDistricts().then(setEditDistricts).catch(() => {});
        }
    }, [editModalOpen]);

    // Load neighborhoods when district changes in edit modal
    useEffect(() => {
        if (editForm.districtId) {
            getNeighborhoods(editForm.districtId).then(setEditNeighborhoods).catch(() => {});
        } else {
            setEditNeighborhoods([]);
        }
    }, [editForm.districtId]);

    const roleMutation = useMutation({
        mutationFn: ({ userId, role }: { userId: string; role: Role }) => assignRole(userId, { role }),
        onSuccess: () => {
            toast.success('Kullanıcı rolü güncellendi');
            queryClient.invalidateQueries({ queryKey: ['users'] });
        },
        onError: (err: any) => toast.error(err.message || 'Rol güncellenemedi'),
    });

    const deleteMutation = useMutation({
        mutationFn: (userId: string) => deleteUser(userId),
        onSuccess: () => {
            toast.success('Kullanıcı silindi');
            setDeleteModalOpen(false);
            setSelectedUserId(null);
            queryClient.invalidateQueries({ queryKey: ['users'] });
        },
        onError: (err: any) => toast.error(err?.response?.data?.message || err.message || 'Kullanıcı silinemedi'),
    });

    const editMutation = useMutation({
        mutationFn: ({ userId, data }: { userId: string; data: any }) => adminUpdateUser(userId, data),
        onSuccess: () => {
            toast.success('Kullanıcı bilgileri güncellendi');
            setEditModalOpen(false);
            setEditUser(null);
            queryClient.invalidateQueries({ queryKey: ['users'] });
        },
        onError: (err: any) => toast.error(err?.response?.data?.message || err.message || 'Güncelleme başarısız'),
    });

    const handleRoleChange = (userId: string, newRole: Role) => {
        roleMutation.mutate({ userId, role: newRole });
    };

    const openEditModal = (user: UserResponse) => {
        setEditUser(user);
        setEditForm({
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
            districtId: user.district?.id ?? '',
            neighborhoodId: user.neighborhood?.id ?? '',
        });
        setEditModalOpen(true);
    };

    const handleEditSave = () => {
        if (!editUser) return;
        const payload: any = {};
        if (editForm.firstName.trim()) payload.firstName = editForm.firstName.trim();
        if (editForm.lastName.trim()) payload.lastName = editForm.lastName.trim();
        if (editForm.email.trim()) payload.email = editForm.email.trim();
        if (editForm.districtId) payload.districtId = editForm.districtId;
        if (editForm.neighborhoodId) payload.neighborhoodId = editForm.neighborhoodId;
        editMutation.mutate({ userId: editUser.id, data: payload });
    };

    const columns: ColumnDef<UserResponse>[] = [
        {
            header: 'Ad Soyad',
            render: (row) => (
                <div className="font-medium text-gray-900">{row.firstName} {row.lastName}</div>
            ),
        },
        {
            header: 'E-posta',
            accessor: 'email',
        },
        {
            header: 'Rol',
            render: (row) => (
                <select
                    value={row.role}
                    onChange={(e) => handleRoleChange(row.id, e.target.value as Role)}
                    disabled={roleMutation.isPending}
                    className="block w-full rounded-md border-0 py-1 pl-3 pr-8 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-xs sm:leading-6 disabled:opacity-50"
                >
                    <option value="VOLUNTEER">Gönüllü</option>
                    <option value="NEIGHBORHOOD_COORDINATOR">Mahalle Koordinatörü</option>
                    <option value="DISTRICT_COORDINATOR">İlçe Koordinatörü</option>
                    <option value="ADMIN">Yönetici</option>
                </select>
            ),
        },
        {
            header: 'Konum',
            render: (row) => (
                <div className="text-xs text-gray-500">
                    {row.district?.name || 'Belirsiz'} / {row.neighborhood?.name || 'Belirsiz'}
                </div>
            ),
        },
        {
            header: 'İşlemler',
            render: (row) => {
                const isProtected = PROTECTED_EMAILS.includes(row.email);
                return (
                    <div className="flex items-center gap-1">
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEditModal(row)}
                            leftIcon={<Pencil className="h-4 w-4 text-blue-500" />}
                            title="Düzenle"
                        >
                            <span className="sr-only">Düzenle</span>
                        </Button>
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => { setSelectedUserId(row.id); setDeleteModalOpen(true); }}
                            leftIcon={<Trash2 className="h-4 w-4 text-red-500" />}
                            title={isProtected ? 'Bu admin kullanıcı korunuyor' : 'Kullanıcıyı Sil'}
                            disabled={isProtected}
                            className={isProtected ? 'opacity-30 cursor-not-allowed' : ''}
                        >
                            <span className="sr-only">Sil</span>
                        </Button>
                    </div>
                );
            },
        },
    ];

    return (
        <div className="space-y-6">
            <div className="sm:flex sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:tracking-tight flex items-center">
                        <Users className="h-6 w-6 mr-2 text-blue-600" />
                        Kullanıcı Yönetimi
                    </h1>
                    <p className="mt-1 text-sm text-gray-500">
                        İstanbul genelindeki gönüllü ve koordinatörleri yönetin.
                    </p>
                </div>
            </div>

            <div className="flex items-center space-x-4 bg-white p-4 shadow rounded-lg mb-4">
                <FormField label="Role Göre Filtrele" className="w-64">
                    <div className="relative flex items-center">
                        <Shield className="absolute left-3 w-4 h-4 text-gray-400" />
                        <select
                            value={roleFilter}
                            onChange={(e) => { setRoleFilter(e.target.value as Role | ''); setPage(0); }}
                            className="block w-full rounded-md border-0 py-1.5 pl-10 pr-10 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-blue-600 sm:text-sm sm:leading-6"
                        >
                            <option value="">Tüm Roller</option>
                            <option value="VOLUNTEER">Gönüllü</option>
                            <option value="NEIGHBORHOOD_COORDINATOR">Mahalle Koordinatörü</option>
                            <option value="DISTRICT_COORDINATOR">İlçe Koordinatörü</option>
                            <option value="ADMIN">Yönetici</option>
                        </select>
                    </div>
                </FormField>
            </div>

            <DataTable
                columns={columns}
                data={data?.content || []}
                isLoading={isLoading}
                emptyMessage="Kullanıcı bulunamadı."
                pagination={data ? { page: data.number, totalPages: data.totalPages } : undefined}
                onPageChange={setPage}
            />

            {/* ── Edit Modal ── */}
            {editModalOpen && editUser && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl w-full max-w-lg">
                        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-gray-200">
                            <h2 className="text-lg font-semibold text-gray-900">Kullanıcı Düzenle</h2>
                            <button onClick={() => setEditModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                                ✕
                            </button>
                        </div>
                        <div className="p-6 space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Ad</label>
                                    <input
                                        type="text"
                                        value={editForm.firstName}
                                        onChange={e => setEditForm(f => ({ ...f, firstName: e.target.value }))}
                                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Soyad</label>
                                    <input
                                        type="text"
                                        value={editForm.lastName}
                                        onChange={e => setEditForm(f => ({ ...f, lastName: e.target.value }))}
                                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">E-posta</label>
                                <input
                                    type="email"
                                    value={editForm.email}
                                    onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))}
                                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">İlçe</label>
                                    <select
                                        value={editForm.districtId}
                                        onChange={e => setEditForm(f => ({ ...f, districtId: e.target.value, neighborhoodId: '' }))}
                                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="">Seçin</option>
                                        {editDistricts.map((d: any) => (
                                            <option key={d.id} value={d.id}>{d.name}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Mahalle</label>
                                    <select
                                        value={editForm.neighborhoodId}
                                        onChange={e => setEditForm(f => ({ ...f, neighborhoodId: e.target.value }))}
                                        disabled={!editForm.districtId}
                                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
                                    >
                                        <option value="">Seçin</option>
                                        {editNeighborhoods.map((n: any) => (
                                            <option key={n.id} value={n.id}>{n.name}</option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                            <p className="text-xs text-gray-400">Mevcut rol: <span className="font-medium">{ROLE_TR[editUser.role] || editUser.role}</span></p>
                        </div>
                        <div className="px-6 pb-5 pt-4 border-t border-gray-200 flex gap-3 justify-end">
                            <button
                                onClick={() => setEditModalOpen(false)}
                                className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                            >
                                İptal
                            </button>
                            <button
                                onClick={handleEditSave}
                                disabled={editMutation.isPending}
                                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium disabled:opacity-60"
                            >
                                {editMutation.isPending ? 'Kaydediliyor...' : 'Kaydet'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <ConfirmationDialog
                isOpen={deleteModalOpen}
                title="Kullanıcıyı Sil"
                message="Bu kullanıcıyı kalıcı olarak silmek istediğinizden emin misiniz? Bu işlem geri alınamaz. Kullanıcıya ait tüm bağlı kayıtlar temizlenecek."
                confirmLabel="Sil"
                confirmVariant="danger"
                onConfirm={() => selectedUserId && deleteMutation.mutate(selectedUserId)}
                onCancel={() => setDeleteModalOpen(false)}
                isLoading={deleteMutation.isPending}
            />
        </div>
    );
};
