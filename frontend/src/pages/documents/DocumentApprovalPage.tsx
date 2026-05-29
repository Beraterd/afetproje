import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getPendingDocuments, approveDocument, rejectDocument, getAdminDocumentViewUrl } from '@/api/documents.api';
import { queryKeys } from '@/utils/queryKeys';
import { Button, DataTable, ColumnDef, Modal, FormField } from '@/components/ui';
import { PendingDocumentResponse } from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { Check, X, Eye, Loader2 } from 'lucide-react';
import { format } from 'date-fns';

const DOC_TYPE_TR: Record<string, string> = {
    SEARCH_RESCUE_CERTIFICATE: 'Arama Kurtarma Sertifikası',
    PSYCHOSOCIAL_GRADUATION_DOCUMENT: 'Psikososyal Mezuniyet Belgesi',
    OTHER: 'Diğer',
};

// İkon-only aksiyon butonu
const IconBtn: React.FC<{
    title: string;
    onClick: () => void;
    disabled?: boolean;
    loading?: boolean;
    colorClass: string;
    children: React.ReactNode;
}> = ({ title, onClick, disabled, loading, colorClass, children }) => (
    <button
        type="button"
        title={title}
        onClick={onClick}
        disabled={disabled || loading}
        className={`inline-flex items-center justify-center rounded-md p-1.5 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-1 disabled:opacity-40 disabled:pointer-events-none ${colorClass}`}
    >
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : children}
    </button>
);

export const DocumentApprovalPage: React.FC = () => {
    const queryClient = useQueryClient();
    const toast = useToast();
    const [page, setPage] = useState(0);
    const [rejectModalOpen, setRejectModalOpen] = useState(false);
    const [selectedDocId, setSelectedDocId] = useState<string | null>(null);
    const [rejectReason, setRejectReason] = useState('');
    const [viewingDocId, setViewingDocId] = useState<string | null>(null);
    const [approvingDocId, setApprovingDocId] = useState<string | null>(null);

    const { data, isLoading } = useQuery({
        queryKey: queryKeys.documents.pending({ page }),
        queryFn: () => getPendingDocuments({ page, size: 10 }),
    });

    const approveMutation = useMutation({
        mutationFn: (id: string) => approveDocument(id),
        onSuccess: () => {
            toast.success('Belge onaylandı');
            setApprovingDocId(null);
            queryClient.invalidateQueries({ queryKey: ['documents', 'pending'] });
        },
        onError: (err: any) => {
            toast.error(err.message || 'Belge onaylanamadı');
            setApprovingDocId(null);
        },
    });

    const rejectMutation = useMutation({
        mutationFn: ({ id, reason }: { id: string; reason: string }) => rejectDocument(id, reason),
        onSuccess: () => {
            toast.success('Belge reddedildi');
            setRejectModalOpen(false);
            setSelectedDocId(null);
            setRejectReason('');
            queryClient.invalidateQueries({ queryKey: ['documents', 'pending'] });
        },
        onError: (err: any) => toast.error(err.message || 'Belge reddedilemedi'),
    });

    const columns: ColumnDef<PendingDocumentResponse>[] = [
        {
            header: 'Gönüllü Adı',
            render: (row) => (
                <div className="font-medium text-gray-900">
                    {row.owner.firstName} {row.owner.lastName}
                </div>
            ),
        },
        {
            header: 'İlçe',
            accessor: 'owner.district' as any,
            render: (row) => row.owner.district,
        },
        {
            header: 'Dosya Adı',
            accessor: 'fileName',
            render: (row) => (
                <span className="font-mono text-xs text-gray-700">{row.fileName}</span>
            ),
        },
        {
            header: 'Belge Türü',
            render: (row) => DOC_TYPE_TR[row.documentType] || row.documentType,
        },
        {
            header: 'Yüklenme Tarihi',
            render: (row) => (
                <span className="text-gray-500">{format(new Date(row.createdAt), 'dd.MM.yyyy HH:mm')}</span>
            ),
        },
        {
            header: 'İşlemler',
            render: (row) => (
                <div className="flex items-center gap-1">
                    {/* Görüntüle */}
                    <IconBtn
                        title="Belgeyi görüntüle"
                        colorClass="text-gray-500 hover:bg-gray-100 hover:text-gray-800 focus:ring-gray-400"
                        loading={viewingDocId === row.id}
                        onClick={async () => {
                            setViewingDocId(row.id);
                            try {
                                const res = await getAdminDocumentViewUrl(row.id);
                                window.open(res.presignedUrl, '_blank', 'noopener,noreferrer');
                            } catch {
                                toast.error('Belge açılamadı');
                            } finally {
                                setViewingDocId(null);
                            }
                        }}
                    >
                        <Eye className="h-4 w-4" />
                    </IconBtn>

                    {/* Onayla */}
                    <IconBtn
                        title="Onayla"
                        colorClass="text-green-600 hover:bg-green-50 hover:text-green-700 focus:ring-green-400"
                        loading={approvingDocId === row.id}
                        onClick={() => {
                            setApprovingDocId(row.id);
                            approveMutation.mutate(row.id);
                        }}
                        disabled={viewingDocId === row.id}
                    >
                        <Check className="h-4 w-4" />
                    </IconBtn>

                    {/* Reddet */}
                    <IconBtn
                        title="Reddet"
                        colorClass="text-red-500 hover:bg-red-50 hover:text-red-700 focus:ring-red-400"
                        onClick={() => {
                            setSelectedDocId(row.id);
                            setRejectReason('');
                            setRejectModalOpen(true);
                        }}
                        disabled={viewingDocId === row.id || approvingDocId === row.id}
                    >
                        <X className="h-4 w-4" />
                    </IconBtn>
                </div>
            ),
        },
    ];

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:tracking-tight">
                    Belge Onayları
                </h1>
                <p className="mt-1 text-sm text-gray-500">
                    Yetki alanınızdaki gönüllülerin yüklediği sertifika ve belgelerini inceleyin.
                </p>
            </div>

            <DataTable
                columns={columns}
                data={data?.content || []}
                isLoading={isLoading}
                emptyMessage="İncelenecek belge bulunmuyor."
                pagination={data ? { page: data.number, totalPages: data.totalPages } : undefined}
                onPageChange={setPage}
            />

            <Modal
                isOpen={rejectModalOpen}
                onClose={() => setRejectModalOpen(false)}
                title="Belgeyi Reddet"
            >
                <div className="space-y-4">
                    <FormField label="Red Sebebi" required>
                        <textarea
                            value={rejectReason}
                            onChange={(e) => setRejectReason(e.target.value)}
                            rows={3}
                            placeholder="Belgenin reddedilme sebebini yazın..."
                            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                        />
                    </FormField>
                    <div className="flex justify-end space-x-3 pt-2">
                        <Button
                            variant="ghost"
                            onClick={() => setRejectModalOpen(false)}
                            disabled={rejectMutation.isPending}
                        >
                            İptal
                        </Button>
                        <Button
                            variant="danger"
                            loading={rejectMutation.isPending}
                            disabled={!rejectReason.trim()}
                            onClick={() =>
                                selectedDocId && rejectMutation.mutate({ id: selectedDocId, reason: rejectReason })
                            }
                        >
                            Reddet
                        </Button>
                    </div>
                </div>
            </Modal>
        </div>
    );
};
