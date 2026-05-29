import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getAuditLogs, exportAuditLogs } from '@/api/audit.api';
import { queryKeys } from '@/utils/queryKeys';
import { AuditLogFilter, AuditLogResponse } from '@/types/audit';
import { ClipboardList, Download, X, ChevronLeft, ChevronRight } from 'lucide-react';
import { useToast } from '@/components/shared/ToastProvider';
import { format } from 'date-fns';

const ACTION_OPTIONS = [
    'USER_LOGIN', 'USER_LOGOUT', 'USER_CREATED', 'USER_UPDATED',
    'USER_DEACTIVATED', 'USER_ACTIVATED', 'ROLE_CHANGED',
    'COORDINATOR_ASSIGNED',
    'EVENT_CREATED', 'EVENT_UPDATED', 'EVENT_CLOSED', 'EVENT_JOINED', 'EVENT_LEFT',
    'TEAM_ASSIGNED',
    'DOCUMENT_UPLOADED', 'DOCUMENT_APPROVED', 'DOCUMENT_REJECTED',
    'DAMAGE_REPORT_CREATED', 'RESOURCE_REQUEST_CREATED',
    'NOTIFICATION_SENT', 'SMS_SENT', 'MAIL_SENT',
    'EARTHQUAKE_AUTO_TRIGGER', 'SIMULATION_RESULT',
];

const ROLE_OPTIONS = ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'];

const ACTION_COLOR: Record<string, string> = {
    USER_LOGIN: 'bg-green-100 text-green-800',
    USER_LOGOUT: 'bg-gray-100 text-gray-700',
    USER_CREATED: 'bg-blue-100 text-blue-800',
    USER_DEACTIVATED: 'bg-red-100 text-red-800',
    USER_ACTIVATED: 'bg-green-100 text-green-800',
    ROLE_CHANGED: 'bg-yellow-100 text-yellow-800',
    COORDINATOR_ASSIGNED: 'bg-purple-100 text-purple-800',
    EVENT_CREATED: 'bg-blue-100 text-blue-800',
    EVENT_CLOSED: 'bg-orange-100 text-orange-800',
    DOCUMENT_APPROVED: 'bg-green-100 text-green-800',
    DOCUMENT_REJECTED: 'bg-red-100 text-red-800',
    EARTHQUAKE_AUTO_TRIGGER: 'bg-red-100 text-red-800',
};

function actionBadge(action: string) {
    const cls = ACTION_COLOR[action] ?? 'bg-gray-100 text-gray-700';
    return (
        <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${cls}`}>
            {action.replace(/_/g, ' ')}
        </span>
    );
}

function formatDate(iso: string) {
    try {
        return format(new Date(iso), 'dd.MM.yyyy HH:mm:ss');
    } catch {
        return iso;
    }
}

interface DetailModalProps {
    log: AuditLogResponse;
    onClose: () => void;
}

function DetailModal({ log, onClose }: DetailModalProps) {
    let metaPretty = '';
    if (log.metadata) {
        try {
            metaPretty = JSON.stringify(JSON.parse(log.metadata), null, 2);
        } catch {
            metaPretty = log.metadata;
        }
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl mx-4 max-h-[90vh] overflow-y-auto">
                <div className="flex items-center justify-between p-4 border-b">
                    <h3 className="text-lg font-semibold text-gray-900">Audit Log Detayı</h3>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
                        <X size={20} />
                    </button>
                </div>
                <div className="p-4 space-y-3 text-sm">
                    <Row label="ID" value={log.id} mono />
                    <Row label="Aksiyon" value={actionBadge(log.action)} />
                    <Row label="Zaman" value={formatDate(log.createdAt)} />
                    <Row label="Aktör" value={log.actorName ?? '—'} />
                    <Row label="Rol" value={log.actorRole ?? '—'} />
                    <Row label="Aktör ID" value={log.actorId ?? '—'} mono />
                    <Row label="Sistem Aksiyonu" value={log.isSystemAction ? 'Evet' : 'Hayır'} />
                    <Row label="Varlık Türü" value={log.entityType} />
                    <Row label="Varlık ID" value={log.entityId ?? '—'} mono />
                    <Row label="Açıklama" value={log.description ?? '—'} />
                    <Row label="IP Adresi" value={log.ipAddress ?? '—'} />
                    {log.userAgent && (
                        <div>
                            <span className="font-medium text-gray-500">User Agent</span>
                            <p className="mt-1 text-gray-700 break-all text-xs">{log.userAgent}</p>
                        </div>
                    )}
                    {metaPretty && (
                        <div>
                            <span className="font-medium text-gray-500">Metadata</span>
                            <pre className="mt-1 bg-gray-50 rounded p-2 text-xs overflow-x-auto">{metaPretty}</pre>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

function Row({ label, value, mono = false }: { label: string; value: React.ReactNode; mono?: boolean }) {
    return (
        <div className="flex gap-2">
            <span className="w-36 shrink-0 font-medium text-gray-500">{label}</span>
            <span className={`text-gray-900 break-all ${mono ? 'font-mono text-xs' : ''}`}>{value}</span>
        </div>
    );
}

export const AuditPage: React.FC = () => {
    const toast = useToast();

    const [page, setPage] = useState(0);
    const [filter, setFilter] = useState<AuditLogFilter>({});
    const [applied, setApplied] = useState<AuditLogFilter>({});
    const [selected, setSelected] = useState<AuditLogResponse | null>(null);
    const [exporting, setExporting] = useState(false);

    const { data, isLoading } = useQuery({
        queryKey: queryKeys.audit.list({ ...applied, page }),
        queryFn: () => getAuditLogs({ ...applied, page, size: 50 }),
    });

    function applyFilter() {
        setPage(0);
        setApplied({ ...filter });
    }

    function clearFilter() {
        setFilter({});
        setApplied({});
        setPage(0);
    }

    async function handleExport() {
        setExporting(true);
        try {
            await exportAuditLogs(applied);
        } catch {
            toast.error('CSV dışa aktarma başarısız oldu');
        } finally {
            setExporting(false);
        }
    }

    const totalPages = data?.totalPages ?? 1;

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                    <ClipboardList className="text-blue-600" size={24} />
                    <h1 className="text-2xl font-bold text-gray-900">Audit Log Yönetimi</h1>
                </div>
                <button
                    onClick={handleExport}
                    disabled={exporting}
                    className="flex items-center gap-1.5 px-3 py-2 text-sm bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
                >
                    <Download size={16} />
                    {exporting ? 'Hazırlanıyor...' : 'CSV İndir'}
                </button>
            </div>

            {/* Filter Panel */}
            <div className="bg-white rounded-xl shadow-sm border p-4 space-y-3">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                    <div>
                        <label className="block text-xs font-medium text-gray-600 mb-1">Aksiyon Türü</label>
                        <select
                            value={filter.actionType ?? ''}
                            onChange={e => setFilter(f => ({ ...f, actionType: e.target.value || undefined }))}
                            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                            <option value="">Tümü</option>
                            {ACTION_OPTIONS.map(a => (
                                <option key={a} value={a}>{a.replace(/_/g, ' ')}</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-gray-600 mb-1">Rol</label>
                        <select
                            value={filter.actorRole ?? ''}
                            onChange={e => setFilter(f => ({ ...f, actorRole: e.target.value || undefined }))}
                            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                            <option value="">Tümü</option>
                            {ROLE_OPTIONS.map(r => (
                                <option key={r} value={r}>{r}</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-gray-600 mb-1">Sistem Aksiyonu</label>
                        <select
                            value={filter.isSystemAction === undefined ? '' : String(filter.isSystemAction)}
                            onChange={e => setFilter(f => ({
                                ...f,
                                isSystemAction: e.target.value === '' ? undefined : e.target.value === 'true',
                            }))}
                            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                            <option value="">Tümü</option>
                            <option value="true">Evet</option>
                            <option value="false">Hayır</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-gray-600 mb-1">Varlık Türü</label>
                        <input
                            type="text"
                            placeholder="örn. Event, User"
                            value={filter.entityType ?? ''}
                            onChange={e => setFilter(f => ({ ...f, entityType: e.target.value || undefined }))}
                            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-gray-600 mb-1">Başlangıç Tarihi</label>
                        <input
                            type="datetime-local"
                            value={filter.fromDate ? filter.fromDate.substring(0, 16) : ''}
                            onChange={e => setFilter(f => ({ ...f, fromDate: e.target.value ? e.target.value + ':00Z' : undefined }))}
                            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-gray-600 mb-1">Bitiş Tarihi</label>
                        <input
                            type="datetime-local"
                            value={filter.toDate ? filter.toDate.substring(0, 16) : ''}
                            onChange={e => setFilter(f => ({ ...f, toDate: e.target.value ? e.target.value + ':00Z' : undefined }))}
                            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <div className="sm:col-span-2">
                        <label className="block text-xs font-medium text-gray-600 mb-1">Arama (aktör, açıklama, aksiyon)</label>
                        <input
                            type="text"
                            placeholder="Ara..."
                            value={filter.search ?? ''}
                            onChange={e => setFilter(f => ({ ...f, search: e.target.value || undefined }))}
                            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                </div>
                <div className="flex gap-2 pt-1">
                    <button
                        onClick={applyFilter}
                        className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700"
                    >
                        Filtrele
                    </button>
                    <button
                        onClick={clearFilter}
                        className="px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200"
                    >
                        Temizle
                    </button>
                </div>
            </div>

            {/* Table */}
            <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
                {isLoading ? (
                    <div className="p-8 text-center text-gray-500">Yükleniyor...</div>
                ) : !data || data.content.length === 0 ? (
                    <div className="p-8 text-center text-gray-500">Kayıt bulunamadı.</div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-100 text-sm">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Zaman</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Aksiyon</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Aktör</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Rol</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Varlık</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Açıklama</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">IP</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-50">
                                {data.content.map(log => (
                                    <tr
                                        key={log.id}
                                        className="hover:bg-blue-50 cursor-pointer"
                                        onClick={() => setSelected(log)}
                                    >
                                        <td className="px-4 py-3 text-gray-600 whitespace-nowrap font-mono text-xs">
                                            {formatDate(log.createdAt)}
                                        </td>
                                        <td className="px-4 py-3 whitespace-nowrap">
                                            {actionBadge(log.action)}
                                        </td>
                                        <td className="px-4 py-3 text-gray-900 whitespace-nowrap">
                                            {log.isSystemAction ? (
                                                <span className="text-gray-400 italic">SYSTEM</span>
                                            ) : (
                                                log.actorName ?? '—'
                                            )}
                                        </td>
                                        <td className="px-4 py-3 text-gray-600 whitespace-nowrap text-xs">
                                            {log.actorRole ?? '—'}
                                        </td>
                                        <td className="px-4 py-3 text-gray-600 whitespace-nowrap text-xs">
                                            {log.entityType}
                                        </td>
                                        <td className="px-4 py-3 text-gray-700 max-w-xs truncate">
                                            {log.description ?? '—'}
                                        </td>
                                        <td className="px-4 py-3 text-gray-500 font-mono text-xs whitespace-nowrap">
                                            {log.ipAddress ?? '—'}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* Pagination */}
                {data && data.totalPages > 1 && (
                    <div className="px-4 py-3 border-t flex items-center justify-between text-sm text-gray-600">
                        <span>
                            Toplam {data.totalElements.toLocaleString('tr-TR')} kayıt — Sayfa {page + 1} / {totalPages}
                        </span>
                        <div className="flex items-center gap-1">
                            <button
                                disabled={page === 0}
                                onClick={() => setPage(p => p - 1)}
                                className="p-1.5 rounded hover:bg-gray-100 disabled:opacity-40"
                            >
                                <ChevronLeft size={16} />
                            </button>
                            <button
                                disabled={page >= totalPages - 1}
                                onClick={() => setPage(p => p + 1)}
                                className="p-1.5 rounded hover:bg-gray-100 disabled:opacity-40"
                            >
                                <ChevronRight size={16} />
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {selected && <DetailModal log={selected} onClose={() => setSelected(null)} />}
        </div>
    );
};
