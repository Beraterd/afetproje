import axiosInstance from './axiosInstance';
import {
    DailyReportResponse,
    ReportTypeKey,
    ComprehensiveReportResponse,
    OperationReportResponse,
    ResourceReportResponse,
    StockReportResponse,
    DamageReportResponse,
    VolunteerReportResponse,
} from '@/types';

export interface ReportScopeParams {
    districtId?: string;
    neighborhoodId?: string;
}

export interface ReportFilterParams {
    districtId?: string;
    neighborhoodId?: string;
    date?: string; // YYYY-MM-DD
}

export const getDailyReport = async (params?: ReportScopeParams): Promise<DailyReportResponse> => {
    const res = await axiosInstance.get<DailyReportResponse>('/reports/daily', { params });
    return res.data;
};

/** Tarayıcıda dosya indirmesini tetikler (PDF/Excel blob). */
const triggerDownload = (data: Blob, fallbackName: string, contentDisposition?: string) => {
    let fileName = fallbackName;
    if (contentDisposition) {
        const match = /filename="?([^"]+)"?/.exec(contentDisposition);
        if (match?.[1]) fileName = decodeURIComponent(match[1]);
    }
    const url = window.URL.createObjectURL(data);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
};

export const downloadReportPdf = async (params?: ReportScopeParams): Promise<void> => {
    const res = await axiosInstance.get('/reports/daily/pdf', { params, responseType: 'blob' });
    triggerDownload(res.data, 'afet-yonetim-raporu.pdf', res.headers['content-disposition']);
};

export const downloadReportExcel = async (params?: ReportScopeParams): Promise<void> => {
    const res = await axiosInstance.get('/reports/daily/excel', { params, responseType: 'blob' });
    triggerDownload(res.data, 'afet-yonetim-raporu.xlsx', res.headers['content-disposition']);
};

// ─────────────────────────────────────────────────────────────────────────────
// Faz 2 — 8 rapor türü endpoint'leri (mevcut /daily korunur)
// ─────────────────────────────────────────────────────────────────────────────

const COMPREHENSIVE_TYPE: Partial<Record<ReportTypeKey, 'DAILY' | 'WEEKLY' | 'MONTHLY'>> = {
    daily: 'DAILY', weekly: 'WEEKLY', monthly: 'MONTHLY',
};

/** key → backend endpoint segmenti (export ve veri için). */
const endpointSegment = (key: ReportTypeKey): string =>
    COMPREHENSIVE_TYPE[key] ? 'comprehensive' : key;

const buildParams = (key: ReportTypeKey, p?: ReportFilterParams) => {
    const params: Record<string, string> = {};
    if (p?.districtId) params.districtId = p.districtId;
    if (p?.neighborhoodId) params.neighborhoodId = p.neighborhoodId;
    if (p?.date) params.date = p.date;
    const compType = COMPREHENSIVE_TYPE[key];
    if (compType) params.type = compType;
    return params;
};

export const getComprehensiveReport = async (
    key: 'daily' | 'weekly' | 'monthly', p?: ReportFilterParams,
): Promise<ComprehensiveReportResponse> => {
    const res = await axiosInstance.get<ComprehensiveReportResponse>('/reports/comprehensive', { params: buildParams(key, p) });
    return res.data;
};

export const getOperationReport = async (p?: ReportFilterParams): Promise<OperationReportResponse> =>
    (await axiosInstance.get<OperationReportResponse>('/reports/operation', { params: buildParams('operation', p) })).data;

export const getResourceReport = async (p?: ReportFilterParams): Promise<ResourceReportResponse> =>
    (await axiosInstance.get<ResourceReportResponse>('/reports/resource', { params: buildParams('resource', p) })).data;

export const getStockReport = async (p?: ReportFilterParams): Promise<StockReportResponse> =>
    (await axiosInstance.get<StockReportResponse>('/reports/stock', { params: buildParams('stock', p) })).data;

export const getDamageReport = async (p?: ReportFilterParams): Promise<DamageReportResponse> =>
    (await axiosInstance.get<DamageReportResponse>('/reports/damage', { params: buildParams('damage', p) })).data;

export const getVolunteerReport = async (p?: ReportFilterParams): Promise<VolunteerReportResponse> =>
    (await axiosInstance.get<VolunteerReportResponse>('/reports/volunteer', { params: buildParams('volunteer', p) })).data;

/** Herhangi bir rapor türü için PDF/Excel indirir. */
export const downloadReportExport = async (
    key: ReportTypeKey, format: 'pdf' | 'excel', p?: ReportFilterParams,
): Promise<void> => {
    const segment = endpointSegment(key);
    const ext = format === 'pdf' ? 'pdf' : 'xlsx';
    const res = await axiosInstance.get(`/reports/${segment}/${format}`, {
        params: buildParams(key, p),
        responseType: 'blob',
    });
    triggerDownload(res.data, `afet-rapor-${key}.${ext}`, res.headers['content-disposition']);
};
