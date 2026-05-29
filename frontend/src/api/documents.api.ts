import axiosInstance from './axiosInstance';
import {
    PagedResponse,
    DocumentResponse,
    DocumentDownloadResponse,
    PendingDocumentResponse,
    DocumentType,
} from '@/types';

export const getMyDocuments = async (): Promise<DocumentResponse[]> => {
    const res = await axiosInstance.get<DocumentResponse[]>('/users/me/documents');
    return res.data;
};

export const getDocumentDownloadUrl = async (id: string): Promise<DocumentDownloadResponse> => {
    const res = await axiosInstance.get<DocumentDownloadResponse>(`/users/me/documents/${id}/download`);
    return res.data;
};

export const uploadDocument = async (
    type: DocumentType,
    file: File,
    onProgress?: (pct: number) => void
): Promise<DocumentResponse> => {
    const form = new FormData();
    form.append('documentType', type);
    form.append('file', file);

    const res = await axiosInstance.post<DocumentResponse>('/users/me/documents', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e) => {
            if (onProgress && e.total) {
                onProgress(Math.round((e.loaded / e.total) * 100));
            }
        },
    });

    return res.data;
};

export const getPendingDocuments = async (params?: any): Promise<PagedResponse<PendingDocumentResponse>> => {
    const res = await axiosInstance.get<PagedResponse<PendingDocumentResponse>>('/admin/documents/pending', { params });
    return res.data;
};

export const getAdminDocumentViewUrl = async (id: string): Promise<DocumentDownloadResponse> => {
    const res = await axiosInstance.get<DocumentDownloadResponse>(`/admin/documents/${id}/view-url`);
    return res.data;
};

export const approveDocument = async (id: string): Promise<void> => {
    await axiosInstance.post(`/admin/documents/${id}/approve`);
};

export const rejectDocument = async (id: string, reason: string): Promise<void> => {
    await axiosInstance.post(`/admin/documents/${id}/reject`, { reason });
};
