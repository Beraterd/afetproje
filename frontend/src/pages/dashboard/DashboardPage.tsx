import React, { useEffect, useState } from 'react';
import { useAuthStore } from '@/store/authStore';
import { getEvents, getMyEvents } from '@/api/events.api';
import { getPendingDocuments, getMyDocuments } from '@/api/documents.api';
import { getDamageAssessments } from '@/api/damageAssessments.api';
import { getResourceRequests } from '@/api/resourceRequests.api';
import { getMe } from '@/api/users.api';
import { AlertTriangle, Building2, PackageOpen, FileText, Calendar, CheckCircle, Clock, Activity, FileBarChart2, ShieldAlert, TrendingUp, FileDown } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Badge, LoadingSpinner } from '@/components/ui';
import { UserResponse, EarthquakeEventResponse, EarthquakeRiskLevel } from '@/types';
import { getLatestEarthquakes } from '@/api/earthquakes.api';
import { getActiveEventCount } from '@/api/teamRecommendations.api';

const RISK_VARIANTS: Record<EarthquakeRiskLevel, 'neutral' | 'info' | 'warning' | 'danger'> = {
    LOW: 'neutral',
    MEDIUM: 'info',
    HIGH: 'warning',
    CRITICAL: 'danger',
};

const RISK_LABELS: Record<EarthquakeRiskLevel, string> = {
    LOW: 'Düşük',
    MEDIUM: 'Orta',
    HIGH: 'Yüksek',
    CRITICAL: 'Kritik',
};

export const DashboardPage: React.FC = () => {
    const user = useAuthStore((s) => s.user);

    if (!user) return null;

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold tracking-tight text-gray-900">
                    Hoş geldiniz, {user.firstName}!
                </h1>
                <p className="mt-1 text-sm text-gray-500">
                    Bugün bölgenizdeki son durumu buradan takip edebilirsiniz.
                </p>
            </div>

            {user.role === 'ADMIN' && <AdminDashboard />}
            {(user.role === 'DISTRICT_COORDINATOR' || user.role === 'NEIGHBORHOOD_COORDINATOR') && <CoordinatorDashboard />}
            {user.role === 'VOLUNTEER' && <VolunteerDashboard />}

            {user.role !== 'VOLUNTEER' && <ReportCenterModule />}

            <LatestEarthquakeCard />
        </div>
    );
};

// ─────────────────────────────────────────────────────
// Rapor Merkezi modülü — yönetici & koordinatörler (§10)
// ─────────────────────────────────────────────────────
const REPORT_SHORTCUTS = [
    { label: 'Günlük Rapor', focus: 'gunluk', icon: <FileBarChart2 className="h-5 w-5" />, color: 'bg-brand-100 text-brand-700' },
    { label: 'Operasyon', focus: 'operasyon', icon: <TrendingUp className="h-5 w-5" />, color: 'bg-emerald-100 text-emerald-700' },
    { label: 'Risk & Hasar', focus: 'hasar', icon: <ShieldAlert className="h-5 w-5" />, color: 'bg-orange-100 text-orange-700' },
    { label: 'Kaynak & Stok', focus: 'kaynak', icon: <PackageOpen className="h-5 w-5" />, color: 'bg-blue-100 text-blue-700' },
];

const ReportCenterModule = () => (
    <div className="glass-card px-4 py-5 sm:p-6">
        <div className="flex items-center justify-between mb-4">
            <h3 className="text-base font-semibold text-gray-900 flex items-center gap-2">
                <FileBarChart2 className="h-5 w-5 text-brand-700" />
                Rapor Merkezi
            </h3>
            <Link to="/reports" className="text-xs font-medium text-brand-700 hover:text-brand-900 transition-colors flex items-center gap-1">
                Tüm Raporlar <FileDown className="h-3.5 w-3.5" />
            </Link>
        </div>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            {REPORT_SHORTCUTS.map((r) => (
                <Link
                    key={r.focus}
                    to={`/reports/view?focus=${r.focus}`}
                    className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white/60 px-3 py-3 hover:border-brand-300 hover:shadow-sm transition-all group"
                >
                    <span className={`rounded-lg p-2 ${r.color} transition-transform group-hover:scale-110`}>{r.icon}</span>
                    <span className="text-sm font-medium text-gray-800">{r.label}</span>
                </Link>
            ))}
        </div>
    </div>
);

const StatCard = ({ title, value, icon, to, color = 'blue', index = 0 }: any) => {
    const bgColors = {
        blue:   'bg-brand-100 text-brand-700',
        green:  'bg-green-100 text-green-700',
        yellow: 'bg-yellow-100 text-yellow-700',
        red:    'bg-red-100 text-red-700',
        orange: 'bg-orange-100 text-orange-700',
    };

    const delayClass = `animate-stagger-${Math.min(index + 1, 4)}`;

    const Card = (
        <div className={`glass-card px-4 py-5 sm:p-6 group ${delayClass}`}>
            <div className="flex items-center">
                <div className={`flex-shrink-0 rounded-xl p-3 ${bgColors[color as keyof typeof bgColors]} shadow-sm transition-transform duration-300 group-hover:scale-110`}>
                    {icon}
                </div>
                <div className="ml-5 w-0 flex-1">
                    <dl>
                        <dt className="truncate text-sm font-medium text-gray-500">{title}</dt>
                        <dd>
                            <div className="text-2xl font-display font-semibold text-gray-900">{value}</div>
                        </dd>
                    </dl>
                </div>
            </div>
        </div>
    );

    return to ? <Link to={to} className="block">{Card}</Link> : Card;
};

// ─────────────────────────────────────────────────────
// Admin Dashboard — 4 kart
// ─────────────────────────────────────────────────────
const AdminDashboard = () => {
    const [stats, setStats] = useState({ events: 0, damage: 0, resources: 0, pendingDocs: 0 });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            getActiveEventCount(),
            getDamageAssessments({ size: 1 }),
            getResourceRequests({ size: 1, status: 'OPEN' }),
            getPendingDocuments({ size: 1 }),
        ]).then(([activeCount, damageRes, resourceRes, docsRes]) => {
            setStats({
                events:      activeCount,
                damage:      damageRes.totalElements,
                resources:   resourceRes.totalElements,
                pendingDocs: docsRes.totalElements,
            });
        }).catch(console.error).finally(() => setLoading(false));
    }, []);

    if (loading) return <LoadingSpinner />;

    return (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
                index={0}
                title="Ekip İhtiyacı"
                value={stats.events}
                icon={<AlertTriangle className="h-6 w-6" />}
                color="red"
                to="/events"
            />
            <StatCard
                index={1}
                title="Hasarlı Bina Sayısı"
                value={stats.damage}
                icon={<Building2 className="h-6 w-6" />}
                color="orange"
                to="/damage-assessments"
            />
            <StatCard
                index={2}
                title="Kaynak Talepleri"
                value={stats.resources}
                icon={<PackageOpen className="h-6 w-6" />}
                color="blue"
                to="/resource-requests"
            />
            <StatCard
                index={3}
                title="Onay Bekleyen Belgeler"
                value={stats.pendingDocs}
                icon={<FileText className="h-6 w-6" />}
                color="yellow"
                to="/documents/approvals"
            />
        </div>
    );
};

// ─────────────────────────────────────────────────────
// Koordinatör Dashboard — 4 kart
// ─────────────────────────────────────────────────────
const CoordinatorDashboard = () => {
    const [stats, setStats] = useState({ events: 0, damage: 0, resources: 0, pendingDocs: 0 });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            getActiveEventCount(),
            getDamageAssessments({ size: 1 }),
            getResourceRequests({ size: 1, status: 'OPEN' }),
            getPendingDocuments({ size: 1 }),
        ]).then(([activeCount, damageRes, resourceRes, docsRes]) => {
            setStats({
                events:      activeCount,
                damage:      damageRes.totalElements,
                resources:   resourceRes.totalElements,
                pendingDocs: docsRes.totalElements,
            });
        }).catch(console.error).finally(() => setLoading(false));
    }, []);

    if (loading) return <LoadingSpinner />;

    return (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
                index={0}
                title="Ekip İhtiyacı"
                value={stats.events}
                icon={<AlertTriangle className="h-6 w-6" />}
                color="red"
                to="/events"
            />
            <StatCard
                index={1}
                title="Hasarlı Bina Sayısı"
                value={stats.damage}
                icon={<Building2 className="h-6 w-6" />}
                color="orange"
                to="/damage-assessments"
            />
            <StatCard
                index={2}
                title="Kaynak Talepleri"
                value={stats.resources}
                icon={<PackageOpen className="h-6 w-6" />}
                color="blue"
                to="/resource-requests"
            />
            <StatCard
                index={3}
                title="Onay Bekleyen Belgeler"
                value={stats.pendingDocs}
                icon={<FileText className="h-6 w-6" />}
                color="yellow"
                to="/documents/approvals"
            />
        </div>
    );
};

// ─────────────────────────────────────────────────────
// Son AFAD Depremi Kartı — tüm roller
// ─────────────────────────────────────────────────────
const formatNumber = (value?: number | null, digits = 1): string =>
    typeof value === 'number' && Number.isFinite(value) ? value.toFixed(digits) : '-';

const formatEventTime = (eventTime?: string | null): string => {
    if (!eventTime) return '-';
    try {
        return new Date(eventTime).toLocaleString('tr-TR');
    } catch {
        return '-';
    }
};

const LatestEarthquakeCard = () => {
    const [latest, setLatest] = React.useState<EarthquakeEventResponse | null>(null);
    const [loading, setLoading] = React.useState(true);

    useEffect(() => {
        getLatestEarthquakes()
            .then((list) => setLatest(list[0] || null))
            .catch(() => setLatest(null))
            .finally(() => setLoading(false));
    }, []);

    if (loading) return null;

    if (!latest) {
        return (
            <div className="glass-card px-4 py-5 sm:p-6 animate-stagger-4">
                <div className="flex items-center justify-between mb-3">
                    <h3 className="text-base font-semibold text-gray-900 flex items-center gap-2">
                        <Activity className="h-5 w-5 text-red-600" />
                        Son AFAD Depremi
                    </h3>
                    <Link
                        to="/earthquakes"
                        className="text-xs font-medium text-brand-700 hover:text-brand-900 transition-colors"
                    >
                        Tümünü Gör →
                    </Link>
                </div>
                <p className="text-sm text-gray-400">Son deprem verisi bulunamadı.</p>
            </div>
        );
    }

    const riskVariant = latest.riskLevel && RISK_VARIANTS[latest.riskLevel] ? RISK_VARIANTS[latest.riskLevel] : 'neutral';
    const riskLabel = latest.riskLevel && RISK_LABELS[latest.riskLevel] ? RISK_LABELS[latest.riskLevel] : '-';
    const depthFormatted = formatNumber(latest.depth, 0);

    return (
        <div className="glass-card px-4 py-5 sm:p-6 animate-stagger-4">
            <div className="flex items-center justify-between mb-3">
                <h3 className="text-base font-semibold text-gray-900 flex items-center gap-2">
                    <Activity className="h-5 w-5 text-red-600" />
                    Son AFAD Depremi
                </h3>
                <Link
                    to="/earthquakes"
                    className="text-xs font-medium text-brand-700 hover:text-brand-900 transition-colors"
                >
                    Tümünü Gör →
                </Link>
            </div>
            <div className="flex flex-wrap items-center gap-4">
                <div className="text-center">
                    <div className="text-3xl font-bold text-gray-900">{formatNumber(latest.magnitude, 1)}</div>
                    <div className="text-xs text-gray-500 mt-0.5">Büyüklük</div>
                </div>
                <div className="flex-1 min-w-0">
                    <div className="font-medium text-gray-800 truncate">
                        {latest.location ||
                            [latest.province, latest.district].filter(Boolean).join(' / ') ||
                            (latest.latitude != null && latest.longitude != null
                                ? `${formatNumber(latest.latitude, 4)}, ${formatNumber(latest.longitude, 4)}`
                                : null) ||
                            'Konum bilgisi yok'}
                    </div>
                    {(latest.province || latest.district) && (
                        <div className="text-sm text-gray-500">
                            {[latest.province, latest.district].filter(Boolean).join(' / ')}
                        </div>
                    )}
                    <div className="text-xs text-gray-400 mt-1">
                        {formatEventTime(latest.eventTime)}
                    </div>
                </div>
                <div className="flex flex-col gap-1.5 items-end">
                    <Badge variant={riskVariant}>
                        {riskLabel}
                    </Badge>
                    {depthFormatted !== '-' && (
                        <span className="text-xs text-gray-400">Derinlik: {depthFormatted} km</span>
                    )}
                    {latest.source && (
                        <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                            {latest.source}
                        </span>
                    )}
                </div>
            </div>
        </div>
    );
};

// ─────────────────────────────────────────────────────
// Gönüllü Dashboard — kişisel özet (değişmedi)
// ─────────────────────────────────────────────────────
const VolunteerDashboard = () => {
    const [profile, setProfile] = useState<UserResponse | null>(null);
    const [myEvents, setMyEvents] = useState(0);
    const [myDocs, setMyDocs] = useState({ pending: 0, approved: 0 });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            getMe(),
            getMyEvents({ size: 1, volunteerStatus: 'ASSIGNED' }),
            getMyDocuments()
        ]).then(([me, events, docs]) => {
            setProfile(me);
            setMyEvents(events.totalElements);
            setMyDocs({
                pending:  docs.filter(d => d.status === 'PENDING').length,
                approved: docs.filter(d => d.status === 'APPROVED').length,
            });
        }).catch(console.error).finally(() => setLoading(false));
    }, []);

    if (loading) return <LoadingSpinner />;

    return (
        <div className="space-y-8">
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
                <StatCard
                    index={0}
                    title="Aktif Görevlerim"
                    value={myEvents}
                    icon={<Calendar className="h-6 w-6" />}
                    color="blue"
                    to="/my-tasks"
                />
                <StatCard
                    index={1}
                    title="Bekleyen Belgelerim"
                    value={myDocs.pending}
                    icon={<Clock className="h-6 w-6" />}
                    color="yellow"
                    to="/documents"
                />
                <StatCard
                    index={2}
                    title="Onaylanan Belgelerim"
                    value={myDocs.approved}
                    icon={<CheckCircle className="h-6 w-6" />}
                    color="green"
                    to="/documents"
                />
            </div>

            <div className="glass-card px-4 py-5 sm:p-6 animate-stagger-4">
                <h3 className="text-base font-semibold leading-6 text-gray-900 flex justify-between items-center">
                    Durumum
                    <Badge variant={profile?.active ? 'success' : 'warning'}>
                        {profile?.active ? 'Aktif' : 'Bilgi / Belge Eksik'}
                    </Badge>
                </h3>
                <div className="mt-2 text-sm text-gray-500">
                    <p>
                        Aktif bölge: <strong>{profile?.district?.name || 'Bilinmiyor'} / {profile?.neighborhood?.name || 'Bilinmiyor'}</strong>.
                        Acil olaylara atanabilmek için belgelerinizi yükleyin ve onaylattırın.
                    </p>
                </div>
            </div>
        </div>
    );
};
