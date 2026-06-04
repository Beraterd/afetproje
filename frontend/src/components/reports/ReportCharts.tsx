import React from 'react';

/**
 * Faz 2 — Bağımlılıksız, responsive rapor grafik & KPI bileşenleri (SVG/CSS).
 * Veri yoksa "Veri bulunamadı" gösterir; sahte veri üretmez.
 */

export const DataEmpty: React.FC<{ label?: string }> = ({ label = 'Veri bulunamadı' }) => (
    <div className="flex items-center justify-center py-6 text-sm text-gray-400 italic">{label}</div>
);

// ── KPI kartları ──────────────────────────────────────────────────────────────

export interface KpiItem {
    label: string;
    value: React.ReactNode;
    tone?: 'default' | 'danger' | 'success' | 'warning';
}

const KPI_TONE: Record<NonNullable<KpiItem['tone']>, string> = {
    default: 'border-gray-200 bg-white/70 text-gray-900',
    danger: 'border-red-200 bg-red-50/70 text-red-700',
    success: 'border-green-200 bg-green-50/70 text-green-700',
    warning: 'border-amber-200 bg-amber-50/70 text-amber-700',
};

export const KpiGrid: React.FC<{ items: KpiItem[] }> = ({ items }) => (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
        {items.map((k, i) => (
            <div key={i} className={`rounded-xl border px-3 py-2.5 ${KPI_TONE[k.tone ?? 'default']}`}>
                <div className="text-xs text-gray-500 truncate">{k.label}</div>
                <div className="text-xl font-semibold">{k.value}</div>
            </div>
        ))}
    </div>
);

// ── Yatay çubuk grafik ────────────────────────────────────────────────────────

export interface ChartDatum { label: string; value: number; color?: string; }

const PALETTE = ['#2563eb', '#16a34a', '#d97706', '#dc2626', '#7c3aed', '#0891b2', '#db2777', '#65a30d'];

export const BarChartH: React.FC<{ title: string; data: ChartDatum[]; unit?: string }> = ({ title, data, unit }) => {
    const valid = (data || []).filter((d) => Number.isFinite(d.value));
    const max = Math.max(1, ...valid.map((d) => d.value));
    return (
        <div className="rounded-lg border border-gray-100 p-3 bg-white/60 break-inside-avoid">
            <h4 className="text-sm font-semibold text-gray-700 mb-2">{title}</h4>
            {valid.length === 0 || valid.every((d) => d.value <= 0) ? (
                <DataEmpty />
            ) : (
                <div className="space-y-1.5">
                    {valid.map((d, i) => (
                        <div key={i} className="flex items-center gap-2 text-xs">
                            <span className="w-28 shrink-0 truncate text-gray-600" title={d.label}>{d.label}</span>
                            <div className="flex-1 h-4 rounded bg-gray-100 overflow-hidden">
                                <div
                                    className="h-full rounded"
                                    style={{ width: `${Math.max(2, (d.value / max) * 100)}%`, backgroundColor: d.color ?? PALETTE[i % PALETTE.length] }}
                                />
                            </div>
                            <span className="w-12 text-right font-medium text-gray-800">
                                {d.value}{unit ?? ''}
                            </span>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

// ── Donut / pasta grafik (SVG) ────────────────────────────────────────────────

export const DonutChart: React.FC<{ title: string; data: ChartDatum[] }> = ({ title, data }) => {
    const valid = (data || []).filter((d) => Number.isFinite(d.value) && d.value > 0);
    const total = valid.reduce((s, d) => s + d.value, 0);
    const radius = 42, circ = 2 * Math.PI * radius;
    let offset = 0;
    return (
        <div className="rounded-lg border border-gray-100 p-3 bg-white/60 break-inside-avoid">
            <h4 className="text-sm font-semibold text-gray-700 mb-2">{title}</h4>
            {total === 0 ? (
                <DataEmpty />
            ) : (
                <div className="flex items-center gap-4">
                    <svg viewBox="0 0 100 100" className="w-28 h-28 shrink-0 -rotate-90">
                        <circle cx="50" cy="50" r={radius} fill="none" stroke="#f1f5f9" strokeWidth="12" />
                        {valid.map((d, i) => {
                            const frac = d.value / total;
                            const dash = frac * circ;
                            const seg = (
                                <circle
                                    key={i} cx="50" cy="50" r={radius} fill="none"
                                    stroke={d.color ?? PALETTE[i % PALETTE.length]} strokeWidth="12"
                                    strokeDasharray={`${dash} ${circ - dash}`} strokeDashoffset={-offset}
                                />
                            );
                            offset += dash;
                            return seg;
                        })}
                    </svg>
                    <ul className="space-y-1 text-xs">
                        {valid.map((d, i) => (
                            <li key={i} className="flex items-center gap-2">
                                <span className="inline-block w-3 h-3 rounded-sm" style={{ backgroundColor: d.color ?? PALETTE[i % PALETTE.length] }} />
                                <span className="text-gray-600">{d.label}</span>
                                <span className="font-medium text-gray-800">
                                    {d.value} (%{Math.round((d.value / total) * 100)})
                                </span>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
};

// ── Oran göstergesi (gauge) ───────────────────────────────────────────────────

export const RatioBar: React.FC<{ title: string; percent: number }> = ({ title, percent }) => {
    const p = Math.max(0, Math.min(100, Math.round(percent)));
    const color = p >= 70 ? '#16a34a' : p >= 40 ? '#d97706' : '#dc2626';
    return (
        <div className="rounded-lg border border-gray-100 p-3 bg-white/60 break-inside-avoid">
            <h4 className="text-sm font-semibold text-gray-700 mb-2">{title}</h4>
            <div className="flex items-center gap-3">
                <div className="flex-1 h-3 rounded-full bg-gray-100 overflow-hidden">
                    <div className="h-full rounded-full" style={{ width: `${p}%`, backgroundColor: color }} />
                </div>
                <span className="text-lg font-bold" style={{ color }}>%{p}</span>
            </div>
        </div>
    );
};
