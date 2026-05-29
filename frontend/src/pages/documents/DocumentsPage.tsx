import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getMyDocuments, uploadDocument, getDocumentDownloadUrl } from '@/api/documents.api';
import { queryKeys } from '@/utils/queryKeys';
import { Button, Badge, DataTable, ColumnDef, FormField, Modal, LoadingSpinner } from '@/components/ui';
import { DocumentResponse, DocumentType } from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { Upload, Download } from 'lucide-react';
import { format } from 'date-fns';

const DOC_TYPE_TR: Record<string, string> = {
    SEARCH_RESCUE_CERTIFICATE: 'Arama Kurtarma Sertifikası',
    PSYCHOSOCIAL_GRADUATION_DOCUMENT: 'Psikososyal Mezuniyet Belgesi',
    OTHER: 'Diğer',
};

const STATUS_TR: Record<string, string> = {
    PENDING: 'Beklemede',
    APPROVED: 'Onaylandı',
    REJECTED: 'Reddedildi',
};

export const DocumentsPage: React.FC = () => {
    const queryClient = useQueryClient();
    const toast = useToast();
    const [uploadModalOpen, setUploadModalOpen] = useState(false);

    const { data: documents, isLoading } = useQuery({
        queryKey: queryKeys.documents.mine(),
        queryFn: getMyDocuments,
    });

    const columns: ColumnDef<DocumentResponse>[] = [
        {
            header: 'Dosya Adı',
            accessor: 'fileName',
            render: (row) => <div className="font-medium text-gray-900">{row.fileName}</div>,
        },
        {
            header: 'Belge Türü',
            accessor: 'documentType',
            render: (row) => DOC_TYPE_TR[row.documentType] || row.documentType,
        },
        {
            header: 'Durum',
            accessor: 'status',
            render: (row) => {
                const variants = {
                    PENDING: 'warning',
                    APPROVED: 'success',
                    REJECTED: 'danger',
                } as const;
                return (
                    <Badge variant={variants[row.status]}>
                        {STATUS_TR[row.status] || row.status}
                    </Badge>
                );
            },
        },
        {
            header: 'Yüklenme Tarihi',
            render: (row) => (
                <span className="text-gray-500">{format(new Date(row.createdAt), 'dd.MM.yyyy')}</span>
            ),
        },
        {
            header: 'İşlemler',
            render: (row) => (
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={async () => {
                        try {
                            const res = await getDocumentDownloadUrl(row.id);
                            window.open(res.presignedUrl, '_blank');
                        } catch {
                            toast.error('İndirme bağlantısı alınamadı');
                        }
                    }}
                    leftIcon={<Download className="h-4 w-4" />}
                >
                    İndir
                </Button>
            ),
        },
    ];

    return (
        <div className="space-y-6">
            <div className="sm:flex sm:items-center sm:justify-between mb-6">
                <div>
                    <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:tracking-tight">
                        Belgelerim
                    </h1>
                    <p className="mt-1 text-sm text-gray-500">
                        Gönüllülük için gerekli sertifika ve belgelerinizi yükleyin.
                    </p>
                </div>
                <div className="mt-4 sm:ml-16 sm:mt-0 sm:flex-none">
                    <Button onClick={() => setUploadModalOpen(true)} leftIcon={<Upload className="h-4 w-4" />}>
                        Belge Yükle
                    </Button>
                </div>
            </div>

            <DataTable
                columns={columns}
                data={documents || []}
                isLoading={isLoading}
                emptyMessage="Henüz belge yüklemediniz."
            />

            <UploadModal
                isOpen={uploadModalOpen}
                onClose={() => setUploadModalOpen(false)}
                onSuccess={() => {
                    setUploadModalOpen(false);
                    queryClient.invalidateQueries({ queryKey: queryKeys.documents.mine() });
                }}
            />
        </div>
    );
};

const UploadModal: React.FC<{ isOpen: boolean; onClose: () => void; onSuccess: () => void }> = ({
    isOpen,
    onClose,
    onSuccess,
}) => {
    const toast = useToast();
    const [file, setFile] = useState<File | null>(null);
    const [type, setType] = useState<DocumentType>('SEARCH_RESCUE_CERTIFICATE');
    const [progress, setProgress] = useState(0);

    const uploadMutation = useMutation({
        mutationFn: (data: { type: DocumentType; file: File }) =>
            uploadDocument(data.type, data.file, setProgress),
        onSuccess: () => {
            toast.success('Belge başarıyla yüklendi');
            setFile(null);
            setProgress(0);
            onSuccess();
        },
        onError: (err: any) => {
            toast.error(err.message || 'Belge yüklenemedi');
            setProgress(0);
        },
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!file) return;
        uploadMutation.mutate({ type, file });
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title="Belge Yükle">
            <form onSubmit={handleSubmit} className="space-y-4">
                <FormField label="Belge Türü" required>
                    <select
                        value={type}
                        onChange={(e) => setType(e.target.value as DocumentType)}
                        className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                        disabled={uploadMutation.isPending}
                    >
                        <option value="SEARCH_RESCUE_CERTIFICATE">Arama Kurtarma Sertifikası</option>
                        <option value="PSYCHOSOCIAL_GRADUATION_DOCUMENT">Psikososyal Mezuniyet Belgesi</option>
                        <option value="OTHER">Diğer</option>
                    </select>
                </FormField>

                <FormField label="Dosya (Maks. 10MB — PDF, PNG, JPEG)" required>
                    <input
                        type="file"
                        accept=".pdf,.png,.jpeg,.jpg"
                        onChange={(e) => setFile(e.target.files?.[0] || null)}
                        className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                        disabled={uploadMutation.isPending}
                    />
                </FormField>

                {uploadMutation.isPending && (
                    <div className="w-full bg-gray-200 rounded-full h-2.5">
                        <div className="bg-blue-600 h-2.5 rounded-full transition-all" style={{ width: `${progress}%` }} />
                    </div>
                )}

                <div className="pt-4 flex justify-end space-x-3">
                    <Button variant="ghost" onClick={onClose} type="button" disabled={uploadMutation.isPending}>
                        İptal
                    </Button>
                    <Button type="submit" loading={uploadMutation.isPending} disabled={!file}>
                        Yükle
                    </Button>
                </div>
            </form>
        </Modal>
    );
};
