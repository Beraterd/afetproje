import React, { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
    FileDown, FileSpreadsheet, ArrowLeft,
    CalendarDays, CalendarRange, CalendarClock,
    Activity, PackageOpen, Boxes, AlertTriangle, HeartHandshake,
    BarChart3, ClipboardList,
} from 'lucide-react';
import {
    getComprehensiveReport, getOperationReport, getResourceReport,
    getStockReport, getDamageReport, getVolunteerReport, downloadReportExport,
} from '@/api/reports.api';
import { getDistricts, getNeighborhoodsByDistrict } from '@/api/districts.api';
import { useAuthStore } from '@/store/authStore';
import { Button, LoadingSpinner, EmptyState } from '@/components/ui';
import { useToast } from '@/components/shared/ToastProvider';
import { KpiGrid, BarChartH, DonutChart } from '@/components/reports/ReportCharts';
import type {
    ReportTypeKey, ReportMetaV2, ReportNeighborhoodMetric,
    TeamModule, DamageModule, ResourceModule, StockModule, VolunteerModule,
    ComprehensiveReportResponse, OperationReportResponse, ResourceReportResponse,
    StockReportResponse, DamageReportResponse, VolunteerReportResponse,
} from '@/types';
import type { KpiItem } from '@/components/reports/ReportCharts';

// ── Config ───────────────────────────────────────────────────────────────────

interface ReportConfig {
    title: string;
    icon: React.ReactNode;
    datePicker: 'daily' | 'weekly' | 'monthly' | 'none';
}

const CONFIG: Record<ReportTypeKey, ReportConfig> = {
    daily:     { title: 'Günlük Rapor',       icon: <CalendarDays className="h-5 w-5" />,   datePicker: 'daily'   },
    weekly:    { title: 'Haftalık Rapor',      icon: <CalendarRange className="h-5 w-5" />,  datePicker: 'weekly'  },
    monthly:   { title: 'Aylık Rapor',         icon: <CalendarClock className="h-5 w-5" />,  datePicker: 'monthly' },
    operation: { title: 'Ekip Raporu',         icon: <Activity className="h-5 w-5" />,       datePicker: 'none'    },
    resource:  { title: 'Kaynak Talep Raporu', icon: <PackageOpen className="h-5 w-5" />,    datePicker: 'none'    },
    stock:     { title: 'Stok Raporu',         icon: <Boxes className="h-5 w-5" />,           datePicker: 'none'    },
    damage:    { title: 'Hasar Raporu',        icon: <AlertTriangle className="h-5 w-5" />,  datePicker: 'none'    },
    volunteer: { title: 'Gönüllü Raporu',      icon: <HeartHandshake className="h-5 w-5" />, datePicker: 'none'    },
};

// ── Helpers ───────────────────────────────────────────────────────────────────

const fmt = (v: number | null | undefined): string => (v == null ? '—' : v.toLocaleString('tr-TR'));
const pct = (v: number | null | undefined): string => (v == null ? '—' : `%${Math.round(v)}`);
const mins = (v: number | null | undefined): string => (v == null ? 'veri yok' : `${v} dk`);
const today = (): string => new Date().toISOString().substring(0, 10);
const nbDatum = (n: ReportNeighborhoodMetric) => ({
    label: n.neighborhoodName,
    sublabel: n.districtName ?? undefined,
    value: n.value,
});

// ── Date picker ───────────────────────────────────────────────────────────────

const DatePicker: React.FC<{
    type: 'daily' | 'weekly' | 'monthly';
    value: string;
    onChange: (v: string) => void;
}> = ({ type, value, onChange }) => {
    if (type === 'monthly') {
        const monthVal = value ? value.substring(0, 7) : today().substring(0, 7);
        return (
            <div className="flex items-center gap-2">
                <label className="text-sm font-medium text-gray-600">Ay:</label>
                <input type="month" value={monthVal}
                    onChange={(e) => onChange(e.target.value + '-01')}
                    className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:outline-none" />
            </div>
        );
    }
    const endLabel = type === 'weekly' && value
        ? new Date(new Date(value).getTime() + 6 * 86400000).toLocaleDateString('tr-TR')
        : null;
    return (
        <div className="flex items-center gap-2 flex-wrap">
            <label className="text-sm font-medium text-gray-600">
                {type === 'weekly' ? 'Hafta başlangıcı:' : 'Tarih:'}
            </label>
            <input type="date" value={value || today()}
                onChange={(e) => onChange(e.target.value)}
                className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:outline-none" />
            {endLabel && <span className="text-sm text-gray-500">→ {endLabel}</span>}
        </div>
    );
};

// ── Shared UI ─────────────────────────────────────────────────────────────────

const SectionCard: React.FC<{ title: string; icon: React.ReactNode; children: React.ReactNode }> = ({ title, icon, children }) => (
    <section className="glass-card px-5 py-5">
        <h2 className="text-base font-semibold text-gray-900 flex items-center gap-2 mb-4">
            <span className="text-brand-700">{icon}</span>{title}
        </h2>
        {children}
    </section>
);

const MetaStrip: React.FC<{ meta: ReportMetaV2 }> = ({ meta }) => (
    <div className="glass-card px-4 py-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-gray-600">
        <span className="font-medium text-gray-900">{meta.typeLabel}</span>
        <span>·</span>
        <span>{meta.periodLabel}</span>
        {meta.districtName && <><span>·</span><span>{meta.districtName}</span></>}
        {meta.neighborhoodName && <><span>·</span><span>{meta.neighborhoodName}</span></>}
        <span>·</span>
        <span>{meta.generatedBy} ({meta.role})</span>
        <span>·</span>
        <span className="text-gray-400 text-xs">{meta.generatedAt}</span>
    </div>
);

const ManagerSummary: React.FC<{ text: string }> = ({ text }) => (
    <SectionCard title="Yönetici Değerlendirmesi" icon={<ClipboardList className="h-5 w-5" />}>
        <p className="text-sm text-gray-700 leading-relaxed bg-brand-50/60 border border-brand-100 rounded-lg p-4 italic">
            {text}
        </p>
    </SectionCard>
);

// ── Module sections ───────────────────────────────────────────────────────────

const TeamSection: React.FC<{ team: TeamModule }> = ({ team }) => {
    const kpis: KpiItem[] = [
        { label: 'Toplam Ekip',    value: fmt(team.totalTeams) },
        { label: 'Aktif Ekip',     value: fmt(team.activeTeams),     tone: team.activeTeams > 0 ? 'success' : 'default' },
        { label: 'Müsait Ekip',    value: fmt(team.availableTeams) },
        { label: 'Toplam Görev',   value: fmt(team.totalTasks) },
        { label: 'Tamamlanan',     value: fmt(team.completedTasks),  tone: 'success' },
        { label: 'Devam Eden',     value: fmt(team.inProgressTasks) },
        { label: 'Bekleyen',       value: fmt(team.openTasks) },
        { label: 'Personel Açığı', value: fmt(team.shortage),        tone: team.shortage > 0 ? 'danger' : 'success' },
        { label: 'Ort. Tamamlama', value: mins(team.avgCompletionMinutes) },
    ];
    return (
        <SectionCard title="Ekip Durumu" icon={<Activity className="h-5 w-5" />}>
            <KpiGrid items={kpis} />
            {team.teamBreakdown?.length > 0 && (
                <div className="mt-4">
                    <BarChartH title="Ekip Türüne Göre Görev Dağılımı"
                        data={team.teamBreakdown.map((c) => ({ label: c.label, value: c.count }))} />
                </div>
            )}
        </SectionCard>
    );
};

const ResourceSection: React.FC<{ resource: ResourceModule }> = ({ resource }) => {
    const kpis: KpiItem[] = [
        { label: 'Toplam Talep',      value: fmt(resource.total) },
        { label: 'Karşılanan',        value: fmt(resource.fulfilled),      tone: 'success' },
        { label: 'Bekleyen',          value: fmt(resource.pending),        tone: resource.pending > 0 ? 'warning' : 'success' },
        { label: 'Kısmen Karşılanan', value: fmt(resource.partial) },
        { label: 'İptal Edilen',      value: fmt(resource.rejected) },
        { label: 'Karşılama Oranı',   value: pct(resource.fulfillmentRate), tone: resource.fulfillmentRate >= 70 ? 'success' : resource.fulfillmentRate >= 40 ? 'warning' : 'danger' },
        { label: 'Kritik Bekleyen',   value: fmt(resource.criticalPending), tone: resource.criticalPending > 0 ? 'danger' : 'default' },
        { label: 'Ort. Çözüm Süresi', value: mins(resource.avgResolutionMinutes) },
    ];
    return (
        <SectionCard title="Kaynak Talepleri" icon={<PackageOpen className="h-5 w-5" />}>
            <KpiGrid items={kpis} />
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
                {resource.topCategories?.length > 0 && (
                    <BarChartH title="Kategori Dağılımı"
                        data={resource.topCategories.map((c) => ({ label: c.label, value: c.count }))} />
                )}
                {resource.topNeighborhoods?.length > 0 && (
                    <BarChartH title="Mahalle Dağılımı"
                        data={resource.topNeighborhoods.map(nbDatum)} />
                )}
            </div>
        </SectionCard>
    );
};

const StockSection: React.FC<{ stock: StockModule; recommendations?: string[] }> = ({ stock, recommendations }) => {
    const kpis: KpiItem[] = [
        { label: 'Toplam Kalem',      value: fmt(stock.totalItems) },
        { label: 'Toplam Miktar',     value: fmt(stock.totalQuantity) },
        { label: 'Kritik Kalem',      value: fmt(stock.criticalItems),   tone: stock.criticalItems > 0 ? 'warning' : 'success' },
        { label: 'Tükenmiş Kalem',    value: fmt(stock.outOfStockItems), tone: stock.outOfStockItems > 0 ? 'danger' : 'success' },
        { label: 'Dönem Girişi',      value: fmt(stock.periodInflow) },
        { label: 'Dönem Çıkışı',      value: fmt(stock.periodOutflow) },
        {
            label: 'Tahmini Kalan',
            value: stock.estimatedDaysRemaining != null ? `${stock.estimatedDaysRemaining} gün` : '—',
            tone: stock.estimatedDaysRemaining != null && stock.estimatedDaysRemaining <= 3 ? 'danger' : 'default',
        },
    ];
    return (
        <SectionCard title="Stok Durumu" icon={<Boxes className="h-5 w-5" />}>
            <KpiGrid items={kpis} />
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
                {stock.categoryDistribution?.length > 0 && (
                    <DonutChart title="Kategori Bazlı Dağılım"
                        data={stock.categoryDistribution.map((c) => ({ label: c.label, value: c.count }))} />
                )}
                {stock.neighborhoodDistribution?.length > 0 && (
                    <BarChartH title="Mahalle Bazlı Stok"
                        data={stock.neighborhoodDistribution.map(nbDatum)} />
                )}
            </div>
            {recommendations && recommendations.length > 0 && (
                <div className="mt-4 space-y-2">
                    <h4 className="text-sm font-semibold text-gray-700">Tedarik Önerileri</h4>
                    {recommendations.map((r, i) => (
                        <div key={i} className="flex gap-2 text-sm text-amber-700 bg-amber-50/60 border border-amber-100 rounded-lg px-3 py-2">
                            <Boxes className="h-4 w-4 mt-0.5 shrink-0" />{r}
                        </div>
                    ))}
                </div>
            )}
        </SectionCard>
    );
};

const DamageSection: React.FC<{ damage: DamageModule }> = ({ damage }) => {
    const kpis: KpiItem[] = [
        { label: 'Toplam Hasar Kaydı',  value: fmt(damage.total) },
        { label: 'Dönemde Yeni',         value: fmt(damage.newInPeriod) },
        { label: 'İnceleme Bekliyor',    value: fmt(damage.pendingReview),       tone: damage.pendingReview > 0 ? 'warning' : 'default' },
        { label: 'Sahada Doğrulanan',    value: fmt(damage.fieldVerified),       tone: 'success' },
        { label: 'Koordinatör Onaylı',   value: fmt(damage.coordinatorApproved), tone: 'success' },
        { label: 'Hafif Hasarlı',        value: fmt(damage.lightRisk) },
        { label: 'Orta Hasarlı',         value: fmt(damage.moderateRisk),        tone: 'warning' },
        { label: 'Ağır Hasarlı',         value: fmt(damage.heavyRisk),           tone: 'danger' },
        { label: 'Yıkık',                value: fmt(damage.criticalRisk),        tone: damage.criticalRisk > 0 ? 'danger' : 'default' },
        { label: 'Ekip İhtiyacı',        value: fmt(damage.teamNeeded),          tone: damage.teamNeeded > 0 ? 'warning' : 'default' },
    ];
    return (
        <SectionCard title="Hasar Değerlendirme" icon={<AlertTriangle className="h-5 w-5" />}>
            <KpiGrid items={kpis} />
            {damage.topNeighborhoods?.length > 0 && (
                <div className="mt-4">
                    <BarChartH title="Mahalle Bazlı Hasar Dağılımı"
                        data={damage.topNeighborhoods.map(nbDatum)} />
                </div>
            )}
        </SectionCard>
    );
};

const VolunteerSection: React.FC<{ volunteer: VolunteerModule }> = ({ volunteer }) => {
    const kpis: KpiItem[] = [
        { label: 'Toplam Gönüllü',  value: fmt(volunteer.total) },
        { label: 'Aktif',           value: fmt(volunteer.active),      tone: 'success' },
        { label: 'Pasif',           value: fmt(volunteer.passive) },
        { label: 'Onaylı',          value: fmt(volunteer.approved),    tone: 'success' },
        { label: 'Bekleyen',        value: fmt(volunteer.pending),     tone: volunteer.pending > 0 ? 'warning' : 'default' },
        { label: 'Dönemde Yeni',    value: fmt(volunteer.newInPeriod), tone: 'success' },
        { label: 'Göreve Atanan',   value: fmt(volunteer.assigned) },
    ];
    return (
        <SectionCard title="Gönüllü Durumu" icon={<HeartHandshake className="h-5 w-5" />}>
            <KpiGrid items={kpis} />
            {volunteer.neighborhoodDistribution?.length > 0 && (
                <div className="mt-4">
                    <BarChartH title="Mahalle Bazlı Gönüllü Dağılımı"
                        data={volunteer.neighborhoodDistribution.map(nbDatum)} />
                </div>
            )}
        </SectionCard>
    );
};

// ── Report body renderers ─────────────────────────────────────────────────────

const ComprehensiveBody: React.FC<{ report: ComprehensiveReportResponse }> = ({ report }) => (
    <div className="space-y-6">
        <MetaStrip meta={report.meta} />
        {report.kpis?.length > 0 && (
            <SectionCard title="Operasyonel Göstergeler" icon={<BarChart3 className="h-5 w-5" />}>
                <ul className="space-y-1.5">
                    {report.kpis.map((k, i) => (
                        <li key={i} className="flex gap-2 text-sm text-gray-700">
                            <span className="text-brand-500 mt-0.5">•</span>{k}
                        </li>
                    ))}
                </ul>
            </SectionCard>
        )}
        <TeamSection team={report.team} />
        <ResourceSection resource={report.resource} />
        <StockSection stock={report.stock} />
        <DamageSection damage={report.damage} />
        <VolunteerSection volunteer={report.volunteer} />
        <ManagerSummary text={report.managerSummary} />
    </div>
);

const OperationBody: React.FC<{ report: OperationReportResponse }> = ({ report }) => (
    <div className="space-y-6">
        <MetaStrip meta={report.meta} />
        <TeamSection team={report.team} />
        <ManagerSummary text={report.managerSummary} />
    </div>
);

const ResourceBody: React.FC<{ report: ResourceReportResponse }> = ({ report }) => (
    <div className="space-y-6">
        <MetaStrip meta={report.meta} />
        <ResourceSection resource={report.resource} />
        <ManagerSummary text={report.managerSummary} />
    </div>
);

const StockBody: React.FC<{ report: StockReportResponse }> = ({ report }) => (
    <div className="space-y-6">
        <MetaStrip meta={report.meta} />
        <StockSection stock={report.stock} recommendations={report.supplyRecommendations} />
        <ManagerSummary text={report.managerSummary} />
    </div>
);

const DamageBody: React.FC<{ report: DamageReportResponse }> = ({ report }) => (
    <div className="space-y-6">
        <MetaStrip meta={report.meta} />
        <DamageSection damage={report.damage} />
        <ManagerSummary text={report.managerSummary} />
    </div>
);

const VolunteerBody: React.FC<{ report: VolunteerReportResponse }> = ({ report }) => (
    <div className="space-y-6">
        <MetaStrip meta={report.meta} />
        <VolunteerSection volunteer={report.volunteer} />
        <ManagerSummary text={report.managerSummary} />
    </div>
);

// ── Main page ─────────────────────────────────────────────────────────────────

export const ReportViewPage: React.FC = () => {
    const [searchParams, setSearchParams] = useSearchParams();
    const user = useAuthStore((s) => s.user);
    const toast = useToast();

    const key = (searchParams.get('key') ?? 'daily') as ReportTypeKey;
    const date = searchParams.get('date') ?? '';
    const districtId = searchParams.get('districtId') ?? undefined;
    const neighborhoodId = searchParams.get('neighborhoodId') ?? undefined;

    const config = CONFIG[key] ?? CONFIG.daily;
    const isAdmin = user?.role === 'ADMIN';
    const isDC = user?.role === 'DISTRICT_COORDINATOR';

    const [exporting, setExporting] = useState<'pdf' | 'excel' | null>(null);

    const filterParams = { districtId, neighborhoodId, date: date || undefined };

    const { data, isLoading, isError } = useQuery<
        ComprehensiveReportResponse | OperationReportResponse | ResourceReportResponse |
        StockReportResponse | DamageReportResponse | VolunteerReportResponse
    >({
        queryKey: ['report', key, date, districtId, neighborhoodId],
        queryFn: () => {
            switch (key) {
                case 'daily': case 'weekly': case 'monthly':
                    return getComprehensiveReport(key, filterParams);
                case 'operation': return getOperationReport(filterParams);
                case 'resource':  return getResourceReport(filterParams);
                case 'stock':     return getStockReport(filterParams);
                case 'damage':    return getDamageReport(filterParams);
                case 'volunteer': return getVolunteerReport(filterParams);
                default: return getComprehensiveReport('daily', filterParams);
            }
        },
    });

    const { data: districts } = useQuery({
        queryKey: ['report', 'districts'],
        queryFn: getDistricts,
        enabled: isAdmin,
        staleTime: 5 * 60 * 1000,
    });

    const dcDistrictId = isDC ? user?.districtId : undefined;
    const { data: neighborhoods } = useQuery({
        queryKey: ['report', 'neighborhoods', isAdmin ? districtId : dcDistrictId],
        queryFn: () => getNeighborhoodsByDistrict((isAdmin ? districtId : dcDistrictId) as string),
        enabled: (isAdmin && !!districtId) || (isDC && !!dcDistrictId),
        staleTime: 5 * 60 * 1000,
    });

    const setParam = (k: string, v: string) => {
        const next = new URLSearchParams(searchParams);
        if (v) next.set(k, v); else next.delete(k);
        if (k === 'districtId') next.delete('neighborhoodId');
        setSearchParams(next, { replace: true });
    };

    const handleExport = async (format: 'pdf' | 'excel') => {
        try {
            setExporting(format);
            await downloadReportExport(key, format, filterParams);
            toast.success(`Rapor ${format === 'pdf' ? 'PDF' : 'Excel'} olarak indirildi.`);
        } catch {
            toast.error('Rapor dışa aktarılamadı.');
        } finally {
            setExporting(null);
        }
    };

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <Link to="/reports"
                        className="text-xs font-medium text-brand-700 hover:text-brand-900 flex items-center gap-1 mb-1">
                        <ArrowLeft className="h-3.5 w-3.5" /> Raporlar
                    </Link>
                    <h1 className="text-2xl font-bold tracking-tight text-gray-900 flex items-center gap-2">
                        <span className="text-brand-700">{config.icon}</span>
                        {config.title}
                    </h1>
                </div>
                <div className="flex items-center gap-2">
                    <Button variant="secondary" size="sm" leftIcon={<FileDown className="h-4 w-4" />}
                        loading={exporting === 'pdf'} disabled={!data || !!exporting}
                        onClick={() => handleExport('pdf')}>PDF</Button>
                    <Button variant="secondary" size="sm" leftIcon={<FileSpreadsheet className="h-4 w-4" />}
                        loading={exporting === 'excel'} disabled={!data || !!exporting}
                        onClick={() => handleExport('excel')}>Excel</Button>
                </div>
            </div>

            {/* Filter bar */}
            <div className="glass-card px-4 py-3 flex flex-wrap items-center gap-3">
                {config.datePicker !== 'none' && (
                    <DatePicker type={config.datePicker} value={date} onChange={(v) => setParam('date', v)} />
                )}
                {isAdmin && (
                    <>
                        <select value={districtId ?? ''} onChange={(e) => setParam('districtId', e.target.value)}
                            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:outline-none">
                            <option value="">Sistem Geneli</option>
                            {districts?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                        </select>
                        <select value={neighborhoodId ?? ''} onChange={(e) => setParam('neighborhoodId', e.target.value)}
                            disabled={!districtId}
                            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:outline-none disabled:opacity-50">
                            <option value="">Tüm Mahalleler</option>
                            {neighborhoods?.map((n) => <option key={n.id} value={n.id}>{n.name}</option>)}
                        </select>
                    </>
                )}
                {isDC && (
                    <select value={neighborhoodId ?? ''} onChange={(e) => setParam('neighborhoodId', e.target.value)}
                        className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:outline-none">
                        <option value="">İlçe Geneli</option>
                        {neighborhoods?.map((n) => <option key={n.id} value={n.id}>{n.name}</option>)}
                    </select>
                )}
            </div>

            {/* Body */}
            {isLoading && <LoadingSpinner />}
            {isError && <EmptyState title="Rapor yüklenemedi" description="Lütfen tekrar deneyin veya filtreleri değiştirin." />}

            {data && !isLoading && (() => {
                switch (key) {
                    case 'daily': case 'weekly': case 'monthly':
                        return <ComprehensiveBody report={data as ComprehensiveReportResponse} />;
                    case 'operation':
                        return <OperationBody report={data as OperationReportResponse} />;
                    case 'resource':
                        return <ResourceBody report={data as ResourceReportResponse} />;
                    case 'stock':
                        return <StockBody report={data as StockReportResponse} />;
                    case 'damage':
                        return <DamageBody report={data as DamageReportResponse} />;
                    case 'volunteer':
                        return <VolunteerBody report={data as VolunteerReportResponse} />;
                    default:
                        return null;
                }
            })()}
        </div>
    );
};
