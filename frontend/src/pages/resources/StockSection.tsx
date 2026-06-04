import { useState, useEffect, useCallback } from 'react';
import { Boxes, Plus, RefreshCw, History, AlertTriangle, X, Filter } from 'lucide-react';
import {
    getResourceStocks,
    getStockSummary,
    createResourceStock,
    updateStockQuantity,
    getStockMovements,
    StockFilters,
} from '@/api/resourceStocks.api';
import { getDistricts } from '@/api/districts.api';
import { getNeighborhoods } from '@/api/neighborhoods.api';
import {
    ResourceStockResponse,
    ResourceStockSummaryResponse,
    ResourceStockMovementResponse,
    CreateResourceStockRequest,
    STOCK_STATUS_BADGE,
    NAME_REQUIRED_CATEGORIES,
    PRODUCT_NAME_PLACEHOLDER,
    RESOURCE_TYPES,
} from '@/types';
import { useToast } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';

const STATUS_OPTIONS = [
    { value: '', label: 'Tüm Durumlar' },
    { value: 'SUFFICIENT', label: 'Yeterli' },
    { value: 'DECREASING', label: 'Azalıyor' },
    { value: 'CRITICAL', label: 'Kritik' },
    { value: 'OUT_OF_STOCK', label: 'Tükendi' },
];

function daysLabel(s: ResourceStockResponse): string {
    if (s.status === 'OUT_OF_STOCK') return 'Tükendi';
    if (s.daysRemaining == null) return 'Bilinmiyor';
    return `${s.daysRemaining} gün`;
}

export function StockSection() {
    const { success: toastSuccess, error: toastError, warning: toastWarning } = useToast();
    const user = useAuthStore(s => s.user);
    const isAdmin = user?.role === 'ADMIN';
    const isDistrictCoord = user?.role === 'DISTRICT_COORDINATOR';
    const isNeighborhoodCoord = user?.role === 'NEIGHBORHOOD_COORDINATOR';
    const canManage = isAdmin || isDistrictCoord || isNeighborhoodCoord;

    const [stocks, setStocks] = useState<ResourceStockResponse[]>([]);
    const [summary, setSummary] = useState<ResourceStockSummaryResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [showFilters, setShowFilters] = useState(false);

    const [districts, setDistricts] = useState<any[]>([]);
    const [filterNeighborhoods, setFilterNeighborhoods] = useState<any[]>([]);
    const [filters, setFilters] = useState<StockFilters>({});

    const [showAddModal, setShowAddModal] = useState(false);
    const [showQtyModal, setShowQtyModal] = useState<ResourceStockResponse | null>(null);
    const [showMovements, setShowMovements] = useState<ResourceStockResponse | null>(null);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const [list, sum] = await Promise.all([
                getResourceStocks(filters),
                getStockSummary({ districtId: filters.districtId, neighborhoodId: filters.neighborhoodId }),
            ]);
            setStocks(list);
            setSummary(sum);
        } catch {
            toastError('Stok bilgileri yüklenemedi');
        } finally {
            setLoading(false);
        }
    }, [filters, toastError]);

    useEffect(() => { load(); }, [load]);
    useEffect(() => { getDistricts().then(setDistricts).catch(() => {}); }, []);

    // Filtre ilçesine göre mahalle listesi
    useEffect(() => {
        if (!filters.districtId) { setFilterNeighborhoods([]); return; }
        getNeighborhoods(filters.districtId).then(setFilterNeighborhoods).catch(() => {});
    }, [filters.districtId]);

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between gap-3 flex-wrap">
                <div className="flex items-center gap-3">
                    <Boxes className="h-6 w-6 text-emerald-600" />
                    <h2 className="text-xl font-bold text-gray-900">Stok Durumu</h2>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => setShowFilters(v => !v)}
                        className="flex items-center gap-1.5 border border-gray-300 text-gray-600 px-3 py-2 rounded-lg text-sm hover:bg-gray-50"
                    >
                        <Filter className="h-4 w-4" /> Filtrele
                    </button>
                    {canManage && (
                        <button
                            onClick={() => setShowAddModal(true)}
                            className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-2 rounded-lg text-sm font-medium"
                        >
                            <Plus className="h-4 w-4" /> Stok Ekle
                        </button>
                    )}
                </div>
            </div>

            {/* Özet kartları */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                <SummaryCard label="Toplam Stok Kalemi" value={summary?.totalItems ?? 0} tone="gray" />
                <SummaryCard label="Kritik Seviye" value={summary?.criticalCount ?? 0} tone="orange" />
                <SummaryCard label="Tükenen" value={summary?.outOfStockCount ?? 0} tone="red" />
                <SummaryCard
                    label="Ort. Yeterlilik"
                    value={summary?.averageDaysRemaining != null ? `${summary.averageDaysRemaining} gün` : '—'}
                    tone="green"
                />
            </div>

            {/* Filtre paneli */}
            {showFilters && (
                <div className="bg-white border border-gray-200 rounded-xl p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                    <select
                        className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={filters.districtId ?? ''}
                        onChange={e => setFilters(f => ({ ...f, districtId: e.target.value || undefined, neighborhoodId: undefined }))}
                    >
                        <option value="">Tüm İlçeler</option>
                        {districts.map((d: any) => <option key={d.id} value={d.id}>{d.name}</option>)}
                    </select>
                    <select
                        className="border border-gray-300 rounded-lg px-3 py-2 text-sm disabled:bg-gray-50"
                        value={filters.neighborhoodId ?? ''}
                        disabled={!filters.districtId}
                        onChange={e => setFilters(f => ({ ...f, neighborhoodId: e.target.value || undefined }))}
                    >
                        <option value="">Tüm Mahalleler</option>
                        {filterNeighborhoods.map((n: any) => <option key={n.id} value={n.id}>{n.name}</option>)}
                    </select>
                    <select
                        className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={filters.category ?? ''}
                        onChange={e => setFilters(f => ({ ...f, category: e.target.value || undefined }))}
                    >
                        <option value="">Tüm Kategoriler</option>
                        {RESOURCE_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                    </select>
                    <select
                        className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={filters.status ?? ''}
                        onChange={e => setFilters(f => ({ ...f, status: e.target.value || undefined }))}
                    >
                        {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                    <input
                        type="text"
                        placeholder="Ürün veya depo ara..."
                        className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={filters.search ?? ''}
                        onChange={e => setFilters(f => ({ ...f, search: e.target.value || undefined }))}
                    />
                    <label className="flex items-center gap-2 text-sm text-gray-700 px-1">
                        <input
                            type="checkbox"
                            checked={!!filters.criticalOnly}
                            onChange={e => setFilters(f => ({ ...f, criticalOnly: e.target.checked || undefined }))}
                        />
                        Sadece kritik stoklar
                    </label>
                </div>
            )}

            {/* Stok kartları (mobil uyumlu grid) */}
            {loading ? (
                <p className="text-center text-gray-500 py-8">Yükleniyor...</p>
            ) : stocks.length === 0 ? (
                <div className="text-center py-10 bg-white border border-gray-200 rounded-xl">
                    <Boxes className="h-10 w-10 text-gray-300 mx-auto mb-2" />
                    <p className="text-gray-500 text-sm">Stok kaydı bulunamadı</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
                    {stocks.map(s => {
                        const critical = s.status === 'CRITICAL' || s.status === 'OUT_OF_STOCK';
                        return (
                            <div
                                key={s.id}
                                className={`bg-white rounded-xl border p-4 ${critical ? 'border-red-300 ring-1 ring-red-100' : 'border-gray-200'}`}
                            >
                                <div className="flex items-start justify-between gap-2">
                                    <div className="min-w-0">
                                        <p className="font-semibold text-gray-900 break-words">{s.name}</p>
                                        <p className="text-xs text-gray-500">{s.categoryLabel}</p>
                                    </div>
                                    <span className={`shrink-0 px-2 py-0.5 rounded-full text-xs font-medium ${STOCK_STATUS_BADGE[s.status] ?? 'bg-gray-100 text-gray-600'}`}>
                                        {critical && <AlertTriangle className="inline h-3 w-3 mr-1 -mt-0.5" />}
                                        {s.statusLabel}
                                    </span>
                                </div>

                                <div className="mt-3 grid grid-cols-2 gap-y-1.5 text-sm">
                                    <span className="text-gray-500">Miktar</span>
                                    <span className="text-gray-900 font-medium text-right">{s.quantity} {s.unit}</span>
                                    <span className="text-gray-500">Günlük tüketim</span>
                                    <span className="text-gray-700 text-right">{s.dailyUsageEstimate != null ? `${s.dailyUsageEstimate} ${s.unit}` : '—'}</span>
                                    <span className="text-gray-500">Yeterlilik</span>
                                    <span className="text-gray-700 text-right">{daysLabel(s)}</span>
                                    <span className="text-gray-500">Kritik eşik</span>
                                    <span className="text-gray-700 text-right">{s.criticalThreshold} {s.unit}</span>
                                </div>

                                <div className="mt-3 pt-3 border-t border-gray-100 text-xs text-gray-500 space-y-0.5">
                                    <p>{s.neighborhoodName ? `${s.neighborhoodName}, ` : ''}{s.districtName}</p>
                                    {s.notes && <p>Not: {s.notes}</p>}
                                    <p>Son güncelleme: {new Date(s.updatedAt).toLocaleDateString('tr-TR')}{s.updatedBy ? ` — ${s.updatedBy}` : ''}</p>
                                </div>

                                <div className="mt-3 flex gap-2">
                                    <button
                                        onClick={() => setShowMovements(s)}
                                        className="flex items-center gap-1 text-xs text-gray-600 hover:text-gray-900 px-2 py-1 rounded hover:bg-gray-100"
                                    >
                                        <History className="h-3.5 w-3.5" /> Hareketler
                                    </button>
                                    {canManage && (
                                        <button
                                            onClick={() => setShowQtyModal(s)}
                                            className="flex items-center gap-1 text-xs text-emerald-700 hover:text-emerald-900 px-2 py-1 rounded hover:bg-emerald-50"
                                        >
                                            <RefreshCw className="h-3.5 w-3.5" /> Miktar Güncelle
                                        </button>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            {showAddModal && (
                <AddStockModal
                    districts={districts}
                    isAdmin={isAdmin}
                    isNeighborhoodCoord={isNeighborhoodCoord}
                    lockedDistrictId={!isAdmin ? user?.districtId : undefined}
                    lockedNeighborhoodId={isNeighborhoodCoord ? user?.neighborhoodId : undefined}
                    onClose={() => setShowAddModal(false)}
                    onSaved={() => { setShowAddModal(false); load(); }}
                    onError={toastError}
                    onSuccess={toastSuccess}
                    onWarn={toastWarning}
                />
            )}

            {showQtyModal && (
                <QuantityModal
                    stock={showQtyModal}
                    onClose={() => setShowQtyModal(null)}
                    onSaved={() => { setShowQtyModal(null); load(); }}
                    onError={toastError}
                    onSuccess={toastSuccess}
                />
            )}

            {showMovements && (
                <MovementsModal stock={showMovements} onClose={() => setShowMovements(null)} onError={toastError} />
            )}
        </div>
    );
}

function SummaryCard({ label, value, tone }: { label: string; value: React.ReactNode; tone: string }) {
    const tones: Record<string, string> = {
        gray: 'text-gray-900', orange: 'text-orange-600', red: 'text-red-600', green: 'text-emerald-600',
    };
    return (
        <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-xs text-gray-500">{label}</p>
            <p className={`text-2xl font-bold mt-1 ${tones[tone]}`}>{value}</p>
        </div>
    );
}

// ── Stok Ekle Modalı ─────────────────────────────────────────────────────────
function AddStockModal(props: {
    districts: any[];
    isAdmin: boolean;
    isNeighborhoodCoord: boolean;
    lockedDistrictId?: string | null;
    lockedNeighborhoodId?: string | null;
    onClose: () => void;
    onSaved: () => void;
    onError: (m: string) => void;
    onSuccess: (m: string) => void;
    onWarn: (m: string) => void;
}) {
    const { districts, isAdmin, isNeighborhoodCoord, lockedDistrictId, lockedNeighborhoodId } = props;
    const [form, setForm] = useState<Partial<CreateResourceStockRequest>>({
        unit: 'adet',
        districtId: lockedDistrictId ?? undefined,
        neighborhoodId: lockedNeighborhoodId ?? undefined,
    });
    const [neighborhoods, setNeighborhoods] = useState<any[]>([]);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (!form.districtId) { setNeighborhoods([]); return; }
        getNeighborhoods(form.districtId).then(setNeighborhoods).catch(() => {});
    }, [form.districtId]);

    const nameRequired = !!form.category && NAME_REQUIRED_CATEGORIES.includes(form.category);
    const nameVisible = nameRequired; // ürün adı yalnızca bu kategorilerde sorulur

    const save = async () => {
        if (!form.category || form.quantity == null || !form.unit || !form.districtId || !form.neighborhoodId) {
            props.onWarn('Kategori, miktar, birim, ilçe ve mahalle zorunludur');
            return;
        }
        if (form.category === 'OTHER' && !form.name?.trim() && !form.notes?.trim()) {
            props.onWarn('"Diğer" kategorisinde ürün adı veya açıklama zorunludur');
            return;
        }
        if (nameRequired && form.category !== 'OTHER' && !form.name?.trim()) {
            props.onWarn('Bu kategori için ürün adı zorunludur');
            return;
        }
        setSaving(true);
        try {
            await createResourceStock(form as CreateResourceStockRequest);
            props.onSuccess('Stok kaydı oluşturuldu');
            props.onSaved();
        } catch (err: any) {
            props.onError(err?.response?.data?.message || 'Stok eklenemedi');
        } finally {
            setSaving(false);
        }
    };

    return (
        <Modal title="Stok Ekle" onClose={props.onClose}>
            <div className="space-y-3">
                <Field label="Kategori / Kaynak Türü *">
                    <select className="modal-input" value={form.category ?? ''}
                        onChange={e => setForm(f => ({ ...f, category: e.target.value, name: undefined }))}>
                        <option value="">Seçin</option>
                        {RESOURCE_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                    </select>
                </Field>
                {nameVisible && (
                    <Field label={form.category === 'OTHER' ? 'Ürün Adı' : 'Ürün Adı *'}>
                        <input className="modal-input" value={form.name ?? ''}
                            placeholder={PRODUCT_NAME_PLACEHOLDER[form.category ?? ''] ?? ''}
                            onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
                    </Field>
                )}
                <div className="grid grid-cols-2 gap-3">
                    <Field label="Miktar *">
                        <input type="number" min="0" className="modal-input" value={form.quantity ?? ''}
                            onChange={e => setForm(f => ({ ...f, quantity: e.target.value ? parseInt(e.target.value) : undefined }))} />
                    </Field>
                    <Field label="Birim *">
                        <input className="modal-input" value={form.unit ?? ''}
                            onChange={e => setForm(f => ({ ...f, unit: e.target.value }))} placeholder="koli, adet..." />
                    </Field>
                </div>
                <div className="grid grid-cols-2 gap-3">
                    <Field label="Günlük Tüketim">
                        <input type="number" min="0" step="0.1" className="modal-input" value={form.dailyUsageEstimate ?? ''}
                            onChange={e => setForm(f => ({ ...f, dailyUsageEstimate: e.target.value ? parseFloat(e.target.value) : undefined }))} />
                    </Field>
                    <Field label="Kritik Eşik">
                        <input type="number" min="0" className="modal-input" value={form.criticalThreshold ?? ''}
                            onChange={e => setForm(f => ({ ...f, criticalThreshold: e.target.value ? parseInt(e.target.value) : undefined }))} />
                    </Field>
                </div>
                <Field label="İlçe *">
                    <select className="modal-input disabled:bg-gray-50" value={form.districtId ?? ''} disabled={!isAdmin}
                        onChange={e => setForm(f => ({ ...f, districtId: e.target.value || undefined, neighborhoodId: undefined }))}>
                        <option value="">Seçin</option>
                        {districts.map((d: any) => <option key={d.id} value={d.id}>{d.name}</option>)}
                    </select>
                </Field>
                <Field label="Mahalle *">
                    <select className="modal-input disabled:bg-gray-50" value={form.neighborhoodId ?? ''}
                        disabled={isNeighborhoodCoord || !form.districtId}
                        onChange={e => setForm(f => ({ ...f, neighborhoodId: e.target.value || undefined }))}>
                        <option value="">{form.districtId ? 'Seçin' : 'Önce ilçe seçin'}</option>
                        {neighborhoods.map((n: any) => <option key={n.id} value={n.id}>{n.name}</option>)}
                    </select>
                </Field>
                <Field label={form.category === 'OTHER' ? 'Açıklama / Not (önerilir)' : 'Açıklama / Not'}>
                    <textarea rows={form.category === 'OTHER' ? 3 : 2} className="modal-input resize-none"
                        value={form.notes ?? ''}
                        onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
                </Field>
                <div className="flex gap-3 pt-1">
                    <button onClick={props.onClose} className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">İptal</button>
                    <button onClick={save} disabled={saving} className="flex-1 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium disabled:opacity-60">
                        {saving ? 'Kaydediliyor...' : 'Kaydet'}
                    </button>
                </div>
            </div>
        </Modal>
    );
}

// ── Miktar Güncelle Modalı ─────────────────────────────────────────────────────
function QuantityModal(props: {
    stock: ResourceStockResponse;
    onClose: () => void;
    onSaved: () => void;
    onError: (m: string) => void;
    onSuccess: (m: string) => void;
}) {
    const [newQuantity, setNewQuantity] = useState<number>(props.stock.quantity);
    const [reason, setReason] = useState('');
    const [saving, setSaving] = useState(false);

    const save = async () => {
        setSaving(true);
        try {
            await updateStockQuantity(props.stock.id, { newQuantity, reason: reason || undefined });
            props.onSuccess('Stok miktarı güncellendi');
            props.onSaved();
        } catch (err: any) {
            props.onError(err?.response?.data?.message || 'Güncelleme başarısız');
        } finally {
            setSaving(false);
        }
    };

    return (
        <Modal title="Miktar Güncelle" onClose={props.onClose}>
            <p className="text-sm text-gray-600 mb-3">{props.stock.name} — mevcut: {props.stock.quantity} {props.stock.unit}</p>
            <Field label="Yeni Miktar">
                <input type="number" min="0" className="modal-input" value={newQuantity}
                    onChange={e => setNewQuantity(parseInt(e.target.value) || 0)} />
            </Field>
            <Field label="Açıklama (opsiyonel)">
                <input className="modal-input" value={reason} placeholder="örn. +50 koli su eklendi"
                    onChange={e => setReason(e.target.value)} />
            </Field>
            <div className="flex gap-3 pt-2">
                <button onClick={props.onClose} className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50">İptal</button>
                <button onClick={save} disabled={saving} className="flex-1 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium disabled:opacity-60">
                    {saving ? 'Güncelleniyor...' : 'Güncelle'}
                </button>
            </div>
        </Modal>
    );
}

// ── Hareket Geçmişi Modalı ──────────────────────────────────────────────────────
function MovementsModal(props: { stock: ResourceStockResponse; onClose: () => void; onError: (m: string) => void }) {
    const [movements, setMovements] = useState<ResourceStockMovementResponse[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        getStockMovements(props.stock.id)
            .then(setMovements)
            .catch(() => props.onError('Hareket geçmişi yüklenemedi'))
            .finally(() => setLoading(false));
    }, [props.stock.id]); // eslint-disable-line react-hooks/exhaustive-deps

    return (
        <Modal title={`Hareket Geçmişi — ${props.stock.name}`} onClose={props.onClose}>
            {loading ? (
                <p className="text-center text-gray-500 py-6 text-sm">Yükleniyor...</p>
            ) : movements.length === 0 ? (
                <p className="text-center text-gray-500 py-6 text-sm">Hareket kaydı yok</p>
            ) : (
                <div className="space-y-2 max-h-[60vh] overflow-y-auto">
                    {movements.map(m => (
                        <div key={m.id} className="border border-gray-100 rounded-lg p-3 text-sm">
                            <div className="flex items-center justify-between">
                                <span className="font-medium text-gray-800">{m.movementTypeLabel}</span>
                                <span className={m.quantityChange >= 0 ? 'text-emerald-600' : 'text-red-600'}>
                                    {m.quantityChange >= 0 ? '+' : ''}{m.quantityChange}
                                </span>
                            </div>
                            <p className="text-xs text-gray-500 mt-0.5">
                                {m.previousQuantity} → {m.newQuantity}
                                {m.reason ? ` · ${m.reason}` : ''}
                            </p>
                            <p className="text-xs text-gray-400 mt-0.5">
                                {new Date(m.createdAt).toLocaleString('tr-TR')}{m.createdBy ? ` — ${m.createdBy}` : ''}
                            </p>
                        </div>
                    ))}
                </div>
            )}
        </Modal>
    );
}

// ── Yardımcı modal/saha bileşenleri ─────────────────────────────────────────────
function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-5 sm:p-6">
                <div className="flex items-start justify-between mb-4">
                    <h3 className="text-lg font-semibold text-gray-900 break-words pr-2">{title}</h3>
                    <button onClick={onClose} aria-label="Kapat" className="text-gray-400 hover:text-gray-600 shrink-0">
                        <X className="h-5 w-5" />
                    </button>
                </div>
                {children}
            </div>
        </div>
    );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
    return (
        <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
            {children}
        </div>
    );
}
