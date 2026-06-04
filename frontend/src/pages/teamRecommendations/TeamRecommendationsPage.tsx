import React, { useEffect, useState } from 'react';
import { useAuthStore } from '@/store/authStore';
import { LoadingSpinner, Badge } from '@/components/ui';
import {
    TeamRecommendationResponse,
    TeamRecommendationRequest,
    TeamTypeResponse,
    DistrictResponse,
    NeighborhoodSummaryResponse,
} from '@/types';
import {
    createTeamRecommendation,
    listTeamRecommendations,
    approveTeamRecommendation,
    rejectTeamRecommendation,
} from '@/api/teamRecommendations.api';
import { getTeamTypes } from '@/api/teams.api';
import { getDistricts, getNeighborhoodsByDistrict } from '@/api/districts.api';
import { Users, Sparkles, CheckCircle, XCircle, ChevronDown, ChevronUp, AlertTriangle } from 'lucide-react';

const PRIORITY_LABELS: Record<string, string> = {
    HIGH: 'Yüksek',
    MEDIUM: 'Orta',
    LOW: 'Düşük',
};

const STATUS_VARIANTS: Record<string, 'info' | 'success' | 'danger' | 'neutral'> = {
    DRAFT: 'info',
    APPROVED: 'success',
    REJECTED: 'danger',
};

const STATUS_LABELS: Record<string, string> = {
    DRAFT: 'Taslak',
    APPROVED: 'Onaylandı',
    REJECTED: 'Reddedildi',
};

export const TeamRecommendationsPage: React.FC = () => {
    const user = useAuthStore((s) => s.user);
    const [recommendations, setRecommendations] = useState<TeamRecommendationResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [expanded, setExpanded] = useState<string | null>(null);

    const fetchList = () => {
        setLoading(true);
        listTeamRecommendations({ size: 50 })
            .then((res) => setRecommendations(res.content))
            .catch(console.error)
            .finally(() => setLoading(false));
    };

    useEffect(() => { fetchList(); }, []);

    const canApprove = user?.role === 'ADMIN' || user?.role === 'DISTRICT_COORDINATOR';

    const handleApprove = async (id: string) => {
        const rec = recommendations.find((r) => r.id === id);
        const selectedUserIds = rec?.recommendedPersonnel?.map((m) => m.userId) ?? [];
        try {
            await approveTeamRecommendation(id, { selectedUserIds });
            fetchList();
        } catch (e: any) {
            alert(e?.message || 'Onaylama başarısız.');
        }
    };

    const handleReject = async (id: string) => {
        if (!confirm('Öneriyi reddetmek istediğinizden emin misiniz?')) return;
        try {
            await rejectTeamRecommendation(id);
            fetchList();
        } catch (e: any) {
            alert(e?.message || 'Reddetme başarısız.');
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-gray-900 flex items-center gap-2">
                        <Sparkles className="h-6 w-6 text-brand-600" />
                        AI Ekip Önerileri
                    </h1>
                    <p className="mt-1 text-sm text-gray-500">
                        Geçmiş görev verileri ve personel skorlamaya göre otomatik ekip önerileri.
                    </p>
                </div>
                {user?.role !== 'VOLUNTEER' && (
                    <button
                        onClick={() => setShowForm(!showForm)}
                        className="btn-primary flex items-center gap-2"
                    >
                        <Users className="h-4 w-4" />
                        Yeni Öneri Oluştur
                    </button>
                )}
            </div>

            <div className="glass-card px-4 py-3 bg-amber-50 border border-amber-200 rounded-lg flex items-start gap-2">
                <AlertTriangle className="h-5 w-5 text-amber-600 flex-shrink-0 mt-0.5" />
                <p className="text-sm text-amber-700">
                    Bu öneri <strong>karar destek amaçlıdır</strong>. Nihai atama yönetici onayıyla yapılır.
                </p>
            </div>

            {showForm && (
                <RecommendationForm
                    onCreated={() => { setShowForm(false); fetchList(); }}
                    onCancel={() => setShowForm(false)}
                />
            )}

            {loading ? (
                <LoadingSpinner />
            ) : recommendations.length === 0 ? (
                <div className="glass-card p-8 text-center text-gray-400">
                    Henüz ekip önerisi bulunmamaktadır.
                </div>
            ) : (
                <div className="space-y-4">
                    {recommendations.map((rec) => (
                        <div key={rec.id} className="glass-card overflow-hidden">
                            <div
                                className="px-5 py-4 flex items-center justify-between cursor-pointer"
                                onClick={() => setExpanded(expanded === rec.id ? null : rec.id)}
                            >
                                <div className="flex items-center gap-3 min-w-0">
                                    <div>
                                        <div className="font-semibold text-gray-900">
                                            {rec.teamTypeLabel} — {rec.districtName}
                                            {rec.neighborhoodName && ` / ${rec.neighborhoodName}`}
                                        </div>
                                        <div className="text-xs text-gray-500 mt-0.5">
                                            {rec.requiredTeamSize} kişi •{' '}
                                            {PRIORITY_LABELS[rec.priority] ?? rec.priority} •{' '}
                                            {new Date(rec.createdAt).toLocaleDateString('tr-TR')}
                                        </div>
                                    </div>
                                </div>
                                <div className="flex items-center gap-3 flex-shrink-0">
                                    <Badge variant={STATUS_VARIANTS[rec.status]}>
                                        {STATUS_LABELS[rec.status]}
                                    </Badge>
                                    {expanded === rec.id ? (
                                        <ChevronUp className="h-4 w-4 text-gray-400" />
                                    ) : (
                                        <ChevronDown className="h-4 w-4 text-gray-400" />
                                    )}
                                </div>
                            </div>

                            {expanded === rec.id && (
                                <div className="border-t border-gray-100 px-5 py-4 space-y-4">
                                    {rec.aiExplanation && (
                                        <div className="bg-brand-50 border border-brand-100 rounded-lg p-3">
                                            <div className="text-xs font-semibold text-brand-700 mb-1 flex items-center gap-1">
                                                <Sparkles className="h-3 w-3" /> AI Açıklaması
                                            </div>
                                            <p className="text-sm text-gray-700 whitespace-pre-wrap">{rec.aiExplanation}</p>
                                        </div>
                                    )}

                                    <div>
                                        <h4 className="text-sm font-semibold text-gray-700 mb-2">
                                            Önerilen Personel ({rec.recommendedPersonnel.length}/{rec.requiredTeamSize})
                                        </h4>
                                        <div className="space-y-2">
                                            {rec.recommendedPersonnel.map((m) => (
                                                <div
                                                    key={m.userId}
                                                    className="flex items-start justify-between p-3 bg-gray-50 rounded-lg"
                                                >
                                                    <div>
                                                        <div className="font-medium text-gray-800 text-sm">
                                                            {m.orderId}. {m.firstName} {m.lastName}
                                                        </div>
                                                        <div className="text-xs text-gray-500 mt-0.5">{m.email}</div>
                                                        {m.reasons.length > 0 && (
                                                            <ul className="mt-1 space-y-0.5">
                                                                {m.reasons.map((r, i) => (
                                                                    <li key={i} className="text-xs text-gray-500">• {r}</li>
                                                                ))}
                                                            </ul>
                                                        )}
                                                    </div>
                                                    <div className="text-right flex-shrink-0 ml-4">
                                                        <div className="text-lg font-bold text-brand-700">{m.score}</div>
                                                        <div className="text-xs text-gray-400">puan</div>
                                                        <div className="text-xs text-gray-500 mt-1">
                                                            {m.completedSimilarTasks} benzer görev
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>

                                    {canApprove && rec.status === 'DRAFT' && (
                                        <div className="flex gap-3 pt-2 border-t border-gray-100">
                                            <button
                                                onClick={() => handleApprove(rec.id)}
                                                className="flex items-center gap-1.5 px-4 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700 transition-colors"
                                            >
                                                <CheckCircle className="h-4 w-4" />
                                                Onayla ve Bildir
                                            </button>
                                            <button
                                                onClick={() => handleReject(rec.id)}
                                                className="flex items-center gap-1.5 px-4 py-2 bg-red-50 text-red-600 text-sm font-medium rounded-lg hover:bg-red-100 transition-colors"
                                            >
                                                <XCircle className="h-4 w-4" />
                                                Reddet
                                            </button>
                                        </div>
                                    )}

                                    {rec.status === 'APPROVED' && rec.approvedByName && (
                                        <div className="text-xs text-gray-400 pt-1 border-t border-gray-100">
                                            Onaylayan: {rec.approvedByName} •{' '}
                                            {rec.approvedAt && new Date(rec.approvedAt).toLocaleString('tr-TR')}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

// ── Yeni öneri formu ─────────────────────────────────────────────────────────

const RecommendationForm: React.FC<{
    onCreated: () => void;
    onCancel: () => void;
}> = ({ onCreated, onCancel }) => {
    const [teamTypes, setTeamTypes] = useState<TeamTypeResponse[]>([]);
    const [districts, setDistricts] = useState<DistrictResponse[]>([]);
    const [neighborhoods, setNeighborhoods] = useState<NeighborhoodSummaryResponse[]>([]);
    const [form, setForm] = useState<TeamRecommendationRequest>({
        teamType: '',
        districtId: '',
        neighborhoodId: undefined,
        requiredTeamSize: 5,
        priority: 'MEDIUM',
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        Promise.all([getTeamTypes(), getDistricts()]).then(([types, dists]) => {
            setTeamTypes(types);
            setDistricts(dists);
        });
    }, []);

    useEffect(() => {
        if (form.districtId) {
            getNeighborhoodsByDistrict(form.districtId).then(setNeighborhoods);
        } else {
            setNeighborhoods([]);
        }
    }, [form.districtId]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form.teamType || !form.districtId) {
            setError('Ekip tipi ve ilçe zorunludur.');
            return;
        }
        setLoading(true);
        setError(null);
        try {
            await createTeamRecommendation(form);
            onCreated();
        } catch (e: any) {
            setError(e?.message || 'Öneri oluşturulamadı.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="glass-card px-5 py-5">
            <h3 className="text-base font-semibold text-gray-900 mb-4">Yeni Ekip Önerisi</h3>
            <form onSubmit={handleSubmit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Ekip Tipi *</label>
                    <select
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={form.teamType}
                        onChange={(e) => setForm({ ...form, teamType: e.target.value })}
                        required
                    >
                        <option value="">Seçin...</option>
                        {teamTypes.map((t) => (
                            <option key={t.value} value={t.value}>{t.label}</option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">İlçe *</label>
                    <select
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={form.districtId}
                        onChange={(e) => setForm({ ...form, districtId: e.target.value, neighborhoodId: undefined })}
                        required
                    >
                        <option value="">Seçin...</option>
                        {districts.map((d) => (
                            <option key={d.id} value={d.id}>{d.name}</option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Mahalle (isteğe bağlı)</label>
                    <select
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={form.neighborhoodId ?? ''}
                        onChange={(e) => setForm({ ...form, neighborhoodId: e.target.value || undefined })}
                        disabled={!form.districtId}
                    >
                        <option value="">Tüm mahalleler</option>
                        {neighborhoods.map((n) => (
                            <option key={n.id} value={n.id}>{n.name}</option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Gerekli Kişi Sayısı</label>
                    <input
                        type="number"
                        min={1}
                        max={50}
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={form.requiredTeamSize}
                        onChange={(e) => setForm({ ...form, requiredTeamSize: parseInt(e.target.value) || 1 })}
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Öncelik</label>
                    <select
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={form.priority}
                        onChange={(e) => setForm({ ...form, priority: e.target.value })}
                    >
                        <option value="HIGH">Yüksek</option>
                        <option value="MEDIUM">Orta</option>
                        <option value="LOW">Düşük</option>
                    </select>
                </div>

                <div className="sm:col-span-2">
                    {error && <p className="text-sm text-red-600 mb-2">{error}</p>}
                    <div className="flex gap-3">
                        <button
                            type="submit"
                            disabled={loading}
                            className="btn-primary flex items-center gap-2"
                        >
                            {loading ? <LoadingSpinner /> : <Sparkles className="h-4 w-4" />}
                            {loading ? 'Analiz ediliyor...' : 'Öneri Oluştur'}
                        </button>
                        <button type="button" onClick={onCancel} className="btn-secondary">
                            İptal
                        </button>
                    </div>
                </div>
            </form>
        </div>
    );
};
