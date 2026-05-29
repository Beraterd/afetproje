import axiosInstance from './axiosInstance';
import { AuditLogFilter, AuditLogResponse } from '@/types/audit';
import { PagedResponse } from '@/types';

export const getAuditLogs = async (filter: AuditLogFilter): Promise<PagedResponse<AuditLogResponse>> => {
    const params: Record<string, any> = {};
    if (filter.actionType) params.actionType = filter.actionType;
    if (filter.actorId) params.actorId = filter.actorId;
    if (filter.actorRole) params.actorRole = filter.actorRole;
    if (filter.entityType) params.entityType = filter.entityType;
    if (filter.isSystemAction !== undefined) params.isSystemAction = filter.isSystemAction;
    if (filter.fromDate) params.fromDate = filter.fromDate;
    if (filter.toDate) params.toDate = filter.toDate;
    if (filter.search) params.search = filter.search;
    params.page = filter.page ?? 0;
    params.size = filter.size ?? 50;
    const res = await axiosInstance.get<PagedResponse<AuditLogResponse>>('/audit', { params });
    return res.data;
};

export const getAuditLog = async (id: string): Promise<AuditLogResponse> => {
    const res = await axiosInstance.get<AuditLogResponse>(`/audit/${id}`);
    return res.data;
};

export const exportAuditLogs = async (filter: AuditLogFilter): Promise<void> => {
    const params: Record<string, any> = {};
    if (filter.actionType) params.actionType = filter.actionType;
    if (filter.actorId) params.actorId = filter.actorId;
    if (filter.actorRole) params.actorRole = filter.actorRole;
    if (filter.entityType) params.entityType = filter.entityType;
    if (filter.isSystemAction !== undefined) params.isSystemAction = filter.isSystemAction;
    if (filter.fromDate) params.fromDate = filter.fromDate;
    if (filter.toDate) params.toDate = filter.toDate;
    if (filter.search) params.search = filter.search;
    const res = await axiosInstance.get('/audit/export', {
        params,
        responseType: 'blob',
    });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'audit_logs.csv';
    a.click();
    URL.revokeObjectURL(url);
};
