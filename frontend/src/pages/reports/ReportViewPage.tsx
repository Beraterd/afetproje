import React, { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
    FileDown, FileSpreadsheet, ArrowLeft, ShieldAlert, TrendingUp, TrendingDown,
    Minus, Gauge, ClipboardList, MapPinned, Sparkles, AlertOctagon,
} from 'lucide-react';
import { getDailyReport, downloadReportPdf, downloadReportExcel } from '@/api/reports.api';
import { getDistricts, getNeighborhoodsByDistrict } from '@/api/districts.api';
import { useAuthStore } from '@/store/authStore';
import { Badge, Button, LoadingSpinner, EmptyState } from '@/components/ui';
import {
    DailyReportResponse, ReportTrendMetric, ReportRiskLevel,
} from '@/types';
import { useToast } from '@/components/shared/ToastProvider';

const FOCUS_TITLES: Record<string, string> = {
    gunluk: 'Günlük Rapor',
    haftalik: 'Haftalık Rapor',
    aylik: 'Aylık Rapor',
    ilce: 'İlçe Raporu',
    mahalle: 'Mahalle Raporu',
    operasyon: 'Operasyon Raporu',
    kaynak: 'Kaynak Raporu',
    stok: 'Stok Raporu',
    hasar: 'Hasar Raporu',
    gonullu: 'Gönüllü Raporu',
};

const RISK_VARIANT: Record<ReportRiskLevel, 'success' | 'info' | 'warning' | 'danger'> = {
    'DÜŞÜK': 'success',
    'ORTA': 'info',
    'YÜKSEK': 'warning',
    'KRİTİK': 'danger',
};

const pct = (v: number) => `%${Math.round(v)}`;
const minutes = (m: number | null) => (m == null ? 'veri yok' : `${m} dk`);

export const ReportViewPage: React.FC = () => {
    const [params, setParams] = useSearchParams();
    const user = useAuthStore((s) => s.user);
    const toast = useToast();

    const focus = params.get('focus') || 'gunluk';
    const districtId = params.get('districtId') || undefined;
    const neighborhoodId = params.get('neighborhoodId') || undefined;
    const isAdmin = user?.role === 'ADMIN';

    const [exporting, setExporting] = useState<'pdf' | 'excel' | null>(null);

    const scope = { districtId, neighborhoodId };

    const { data: report, isLoading, isError } = useQuery({
        queryKey: ['report', 'daily', districtId, neighborhoodId],
        queryFn: () => getDailyReport(scope),
    });

    // Admin scope selectors
    const { data: districts } = useQuery({
        queryKey: ['report', 'districts'],
        queryFn: getDistricts,
        enabled: isAdmin,
        staleTime: 5 * 60 * 1000,
    });
    const { data: neighborhoods } = useQuery({
        queryKey: ['report', 'neighborhoods', districtId],
        queryFn: () => getNeighborhoodsByDistrict(districtId as string),
        enabled: isAdmin && !!districtId,
        staleTime: 5 * 60 * 1000,
    });

    const updateScope = (key: 'districtId' | 'neighborhoodId', value: string) => {
        const next = new URLSearchParams(params);
        if (value) next.set(key, value);
        else next.delete(key);
        if (key === 'districtId') next.delete('neighborhoodId');
        setParams(next, { replace: true });
    };

    const handleExport = async (kind: 'pdf' | 'excel') => {
        try {
            setExporting(kind);
            if (kind === 'pdf') await downloadReportPdf(scope);
            else await downloadReportExcel(scope);
            toast.success(`Rapor ${kind === 'pdf' ? 'PDF' : 'Excel'} olarak indirildi.`);
        } catch (e: any) {
            toast.error(e?.message || 'Rapor dışa aktarılamadı.');
        } finally {
            setExporting(null);
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <Link to="/reports" className="text-xs font-medium text-brand-700 hover:text-brand-900 flex items-center gap-1 mb-1">
                        <ArrowLeft className="h-3.5 w-3.5" /> Rapor Merkezi
                    </Link>
                    <h1 className="text-2xl font-bold tracking-tight text-gray-900">
                        {FOCUS_TITLES[focus] || 'Yönetim Raporu'}
                    </h1>
                    {report && (
                        <p className="mt-1 text-sm text-gray-500">
                            {report.meta.scopeLabel} • {report.meta.date} {report.meta.time} • {report.meta.generatedBy} ({report.meta.role})
                        </p>
                    )}
                </div>
                <div className="flex items-center gap-2">
                    <Button variant="secondary" size="sm" leftIcon={<FileDown className="h-4 w-4" />}
                        loading={exporting === 'pdf'} disabled={!report || !!exporting}
                        onClick={() => handleExport('pdf')}>
                        PDF
                    </Button>
                    <Button variant="secondary" size="sm" leftIcon={<FileSpreadsheet className="h-4 w-4" />}
                        loading={exporting === 'excel'} disabled={!report || !!exporting}
                        onClick={() => handleExport('excel')}>
                        Excel
                    </Button>
                </div>
            </div>

            {isAdmin && (
                <div className="glass-card px-4 py-3 flex flex-wrap items-center gap-3">
                    <span className="text-sm font-medium text-gray-600">Kapsam:</span>
                    <select
                        value={districtId || ''}
                        onChange={(e) => updateScope('districtId', e.target.value)}
                        className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:ring-brand-500"
                    >
                        <option value="">Sistem Geneli</option>
                        {districts?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                    </select>
                    <select
                        value={neighborhoodId || ''}
                        onChange={(e) => updateScope('neighborhoodId', e.target.value)}
                        disabled={!districtId}
                        className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:ring-brand-500 disabled:opacity-50"
                    >
                        <option value="">Tüm Mahalleler</option>
                        {neighborhoods?.map((n) => <option key={n.id} value={n.id}>{n.name}</option>)}
                    </select>
                </div>
            )}

            {isLoading && <LoadingSpinner />}
            {isError && <EmptyState title="Rapor yüklenemedi" description="Lütfen tekrar deneyin." />}

            {report && <ReportBody report={report} />}
        </div>
    );
};

// ─────────────────────────────────────────────────────────────────────────────

const SectionCard: React.FC<{ title: string; icon: React.ReactNode; children: React.ReactNode }> = ({ title, icon, children }) => (
    <section className="glass-card px-5 py-5">
        <h2 className="text-base font-semibold text-gray-900 flex items-center gap-2 mb-3">
            <span className="text-brand-700">{icon}</span>{title}
        </h2>
        {children}
    </section>
);

const ReportBody: React.FC<{ report: DailyReportResponse }> = ({ report }) => (
    <div className="space-y-6">
        {report.executiveSummary && (
            <SectionCard title="Genel Durum Özeti" icon={<ClipboardList className="h-5 w-5" />}>
                <ul className="space-y-1.5">
                    {report.executiveSummary.highlights.map((h, i) => (
                        <li key={i} className="flex gap-2 text-sm text-gray-700">
                            <span className="text-brand-500 mt-0.5">•</span>{h}
                        </li>
                    ))}
                </ul>
            </SectionCard>
        )}

        {report.riskAnalysis && <RiskSection risk={report.riskAnalysis} />}

        <SectionCard title="Operasyonel Göstergeler (KPI)" icon={<Gauge className="h-5 w-5" />}>
            <ul className="space-y-1.5 mb-4">
                {report.kpis.map((k, i) => (
                    <li key={i} className="flex gap-2 text-sm text-gray-700">
                        <span className="text-brand-500 mt-0.5">•</span>{k}
                    </li>
                ))}
            </ul>
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
                <Stat label="Toplam Talep" value={report.resources.total} />
                <Stat label="Karşılanan" value={report.resources.fulfilled} />
                <Stat label="Karşılama Oranı" value={pct(report.resources.fulfillmentRate)} />
                <Stat label="Kritik Stok" value={report.stock.criticalItems + report.stock.outOfStockItems} highlight />
                <Stat label="Toplam Görev" value={report.tasks.total} />
                <Stat label="Tamamlanan" value={report.tasks.completed} />
                <Stat label="Bildirilen Hasar" value={report.damage.reported} />
                <Stat label="Aktif Gönüllü" value={report.volunteers.active} />
            </div>
        </SectionCard>

        {report.operationPerformance && (
            <SectionCard title="Operasyon Performansı" icon={<Gauge className="h-5 w-5" />}>
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
                    <Stat label="Görev Tamamlama" value={pct(report.operationPerformance.taskCompletionRate)} />
                    <Stat label="Talep Karşılama" value={pct(report.operationPerformance.requestFulfillmentRate)} />
                    <Stat label="Dağıtım Perf." value={pct(report.operationPerformance.distributionPerformance)} />
                    <Stat label="Ort. Müdahale" value={minutes(report.operationPerformance.avgResponseMinutes)} />
                    <Stat label="Ort. Çözüm" value={minutes(report.operationPerformance.avgResolutionMinutes)} />
                </div>
            </SectionCard>
        )}

        {report.trendAnalysis && <TrendSection trend={report.trendAnalysis} />}

        {report.neighborhoodComparisons && report.neighborhoodComparisons.length > 0 && (
            <NeighborhoodSection rows={report.neighborhoodComparisons} />
        )}

        {report.aiCommentary && (
            <SectionCard title="Yapay Zeka Destekli Yönetici Değerlendirmesi" icon={<Sparkles className="h-5 w-5" />}>
                <p className="text-sm text-gray-700 leading-relaxed bg-brand-50/60 border border-brand-100 rounded-lg p-4 italic">
                    {report.aiCommentary}
                </p>
            </SectionCard>
        )}
    </div>
);

const Stat: React.FC<{ label: string; value: React.ReactNode; highlight?: boolean }> = ({ label, value, highlight }) => (
    <div className={`rounded-xl border px-3 py-2.5 ${highlight ? 'border-red-200 bg-red-50/60' : 'border-gray-200 bg-white/60'}`}>
        <div className="text-xs text-gray-500 truncate">{label}</div>
        <div className={`text-lg font-semibold ${highlight ? 'text-red-700' : 'text-gray-900'}`}>{value}</div>
    </div>
);

const RiskSection: React.FC<{ risk: NonNullable<DailyReportResponse['riskAnalysis']> }> = ({ risk }) => (
    <SectionCard title="Risk Analizi" icon={<ShieldAlert className="h-5 w-5" />}>
        <div className="flex items-center gap-4 mb-4">
            <div className="text-center">
                <div className="text-3xl font-bold text-gray-900">{risk.score}<span className="text-base text-gray-400">/100</span></div>
                <Badge variant={RISK_VARIANT[risk.level]}>{risk.level}</Badge>
            </div>
            <div className="flex-1 h-3 rounded-full bg-gray-100 overflow-hidden">
                <div
                    className={`h-full rounded-full ${risk.level === 'KRİTİK' ? 'bg-red-500' : risk.level === 'YÜKSEK' ? 'bg-orange-500' : risk.level === 'ORTA' ? 'bg-yellow-500' : 'bg-green-500'}`}
                    style={{ width: `${risk.score}%` }}
                />
            </div>
        </div>
        <div className="overflow-x-auto">
            <table className="w-full text-sm">
                <thead>
                    <tr className="text-left text-xs text-gray-500 border-b border-gray-200">
                        <th className="py-2 pr-3">Faktör</th>
                        <th className="py-2 pr-3 w-16">Puan</th>
                        <th className="py-2">Açıklama</th>
                    </tr>
                </thead>
                <tbody>
                    {risk.factors.map((f, i) => (
                        <tr key={i} className="border-b border-gray-100 last:border-0">
                            <td className="py-2 pr-3 font-medium text-gray-800">{f.label}</td>
                            <td className="py-2 pr-3 text-gray-700">{f.points}</td>
                            <td className="py-2 text-gray-600">{f.detail}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
        {risk.warnings.length > 0 && (
            <div className="mt-3 space-y-1.5">
                {risk.warnings.map((w, i) => (
                    <div key={i} className="flex gap-2 text-sm text-red-700 bg-red-50/60 border border-red-100 rounded-lg px-3 py-2">
                        <AlertOctagon className="h-4 w-4 mt-0.5 flex-shrink-0" />{w}
                    </div>
                ))}
            </div>
        )}
    </SectionCard>
);

const ChangeBadge: React.FC<{ rate: number | null }> = ({ rate }) => {
    if (rate == null) return <span className="text-gray-400 inline-flex items-center gap-1"><Minus className="h-3.5 w-3.5" />—</span>;
    if (rate > 0) return <span className="text-red-600 inline-flex items-center gap-1"><TrendingUp className="h-3.5 w-3.5" />%{rate.toFixed(1)}</span>;
    if (rate < 0) return <span className="text-green-600 inline-flex items-center gap-1"><TrendingDown className="h-3.5 w-3.5" />%{Math.abs(rate).toFixed(1)}</span>;
    return <span className="text-gray-500 inline-flex items-center gap-1"><Minus className="h-3.5 w-3.5" />%0</span>;
};

const TrendRow: React.FC<{ label: string; m: ReportTrendMetric }> = ({ label, m }) => (
    <tr className="border-b border-gray-100 last:border-0">
        <td className="py-2 pr-3 font-medium text-gray-800">{label}</td>
        <td className="py-2 pr-3 text-gray-700">{m.today}</td>
        <td className="py-2 pr-3 text-gray-700">{m.last7Days}</td>
        <td className="py-2 pr-3 text-gray-700">{m.last30Days}</td>
        <td className="py-2"><ChangeBadge rate={m.changeRate} /></td>
    </tr>
);

const TrendSection: React.FC<{ trend: NonNullable<DailyReportResponse['trendAnalysis']> }> = ({ trend }) => (
    <SectionCard title="Trend Analizi (Bugün / Son 7 Gün / Son 30 Gün)" icon={<TrendingUp className="h-5 w-5" />}>
        <div className="overflow-x-auto">
            <table className="w-full text-sm">
                <thead>
                    <tr className="text-left text-xs text-gray-500 border-b border-gray-200">
                        <th className="py-2 pr-3">Metrik</th>
                        <th className="py-2 pr-3">Bugün</th>
                        <th className="py-2 pr-3">Son 7 Gün</th>
                        <th className="py-2 pr-3">Son 30 Gün</th>
                        <th className="py-2">Değişim (7g)</th>
                    </tr>
                </thead>
                <tbody>
                    <TrendRow label="Talepler" m={trend.requests} />
                    <TrendRow label="Görevler" m={trend.tasks} />
                    <TrendRow label="Kaynak Tüketimi" m={trend.resourceConsumption} />
                    <TrendRow label="Stok Değişimi" m={trend.stockChange} />
                </tbody>
            </table>
        </div>
    </SectionCard>
);

const NeighborhoodSection: React.FC<{ rows: NonNullable<DailyReportResponse['neighborhoodComparisons']> }> = ({ rows }) => (
    <SectionCard title="Mahalle Karşılaştırmaları" icon={<MapPinned className="h-5 w-5" />}>
        <div className="overflow-x-auto">
            <table className="w-full text-sm">
                <thead>
                    <tr className="text-left text-xs text-gray-500 border-b border-gray-200">
                        <th className="py-2 pr-3">Mahalle</th>
                        <th className="py-2 pr-3">Talep</th>
                        <th className="py-2 pr-3">Kaynak Kul.</th>
                        <th className="py-2 pr-3">Görev</th>
                        <th className="py-2 pr-3">Hasar</th>
                        <th className="py-2">Risk</th>
                    </tr>
                </thead>
                <tbody>
                    {rows.map((c) => (
                        <tr key={c.neighborhoodId} className="border-b border-gray-100 last:border-0">
                            <td className="py-2 pr-3 font-medium text-gray-800">{c.neighborhoodName}</td>
                            <td className="py-2 pr-3 text-gray-700">{c.requestCount}</td>
                            <td className="py-2 pr-3 text-gray-700">{c.resourceUsage}</td>
                            <td className="py-2 pr-3 text-gray-700">{c.taskCount}</td>
                            <td className="py-2 pr-3 text-gray-700">{c.damageCount}</td>
                            <td className="py-2">
                                <span className={`inline-block px-2 py-0.5 rounded text-xs font-semibold ${c.riskScore >= 75 ? 'bg-red-100 text-red-700' : c.riskScore >= 50 ? 'bg-orange-100 text-orange-700' : c.riskScore >= 25 ? 'bg-yellow-100 text-yellow-700' : 'bg-green-100 text-green-700'}`}>
                                    {c.riskScore}
                                </span>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    </SectionCard>
);
