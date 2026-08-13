import { useState } from 'react';
import { ChevronDown, ChevronUp, Palette } from 'lucide-react';
import { DAMAGE_STATUS_COLORS, NO_REPORT_COLOR } from '@/utils/damageColors';
import { HEIGHT_COLOR_STOPS } from '@/config/map3d';
import type { BuildingColorMode } from './BuildingColorModeControl';

const LEGEND_ITEMS: { color: string; label: string }[] = [
    { color: DAMAGE_STATUS_COLORS.COLLAPSED, label: 'Yıkılmış' },
    { color: DAMAGE_STATUS_COLORS.HEAVY, label: 'Ağır hasar' },
    { color: DAMAGE_STATUS_COLORS.MODERATE, label: 'Orta hasar' },
    { color: DAMAGE_STATUS_COLORS.LIGHT, label: 'Hafif hasar' },
    { color: DAMAGE_STATUS_COLORS.UNASSESSED, label: 'Değerlendirilmedi' },
];

interface BuildingLegendProps {
    mode?: BuildingColorMode;
}

/** Bina renklendirme lejantı — `mode="damage"` (varsayılan) hasar durumu renklerini,
 *  `mode="height"` yükseklik gradient'ini gösterir. Mobilde varsayılan kapalı (sadece ikon),
 *  masaüstünde açık başlar. Harita üzerinde sürekli büyük yer kaplamaması için collapsible. */
export const BuildingLegend: React.FC<BuildingLegendProps> = ({ mode = 'damage' }) => {
    const [open, setOpen] = useState(() => window.innerWidth >= 640);

    return (
        <div className="absolute bottom-8 left-4 z-10 bg-white/95 backdrop-blur rounded-lg shadow-lg border border-slate-200 text-xs">
            <button
                type="button"
                onClick={() => setOpen((v) => !v)}
                aria-expanded={open}
                className="w-full flex items-center gap-1.5 px-3 py-2 font-medium text-slate-700"
            >
                <Palette size={13} className="text-slate-400" />
                {mode === 'height' ? 'Yükseklik' : 'Hasar Durumu'}
                {open ? <ChevronDown size={13} className="ml-auto" /> : <ChevronUp size={13} className="ml-auto" />}
            </button>
            {open && mode === 'damage' && (
                <div className="px-3 pb-2.5 space-y-1">
                    {LEGEND_ITEMS.map((item) => (
                        <div key={item.label} className="flex items-center gap-2">
                            <span className="w-3 h-3 rounded-sm inline-block shrink-0" style={{ backgroundColor: item.color }} />
                            {item.label}
                        </div>
                    ))}
                    <div className="flex items-center gap-2">
                        <span className="w-3 h-3 rounded-sm inline-block shrink-0" style={{ backgroundColor: NO_REPORT_COLOR }} />
                        Rapor yok
                    </div>
                </div>
            )}
            {open && mode === 'height' && (
                <div className="px-3 pb-2.5">
                    <div
                        className="h-2.5 rounded-sm w-full"
                        style={{
                            background: `linear-gradient(to right, ${HEIGHT_COLOR_STOPS.map((s) => s.color).join(', ')})`,
                        }}
                    />
                    <div className="flex justify-between mt-1 text-[10px] text-slate-500">
                        {HEIGHT_COLOR_STOPS.map((s) => (
                            <span key={s.stop}>{s.stop}{s.stop === HEIGHT_COLOR_STOPS[HEIGHT_COLOR_STOPS.length - 1].stop ? '+' : ''} m</span>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};
