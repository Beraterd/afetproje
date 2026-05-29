import { UserSummaryResponse } from './auth';
import { DocumentStatus, DocumentType } from './common';

export interface DocumentResponse {
    id: string;
    documentType: DocumentType;
    status: DocumentStatus;
    fileName: string;
    fileSizeBytes: number;
    mimeType: string;
    rejectionReason?: string;
    reviewedBy?: UserSummaryResponse;
    reviewedAt?: string;
    createdAt: string;
}

export interface DocumentDownloadResponse {
    presignedUrl: string;
    expiresAt: string;
}

export interface DocumentOwnerResponse {
    id: string;
    firstName: string;
    lastName: string;
    district: string;
}

export interface PendingDocumentResponse {
    id: string;
    documentType: DocumentType;
    status: DocumentStatus;
    fileName: string;
    owner: DocumentOwnerResponse;
    createdAt: string;
}
