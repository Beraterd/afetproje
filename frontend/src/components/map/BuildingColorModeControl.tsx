import { useState } from 'react';
import { Palette, ChevronDown, ChevronUp } from 'lucide-react';

export type BuildingColorMode = 'damage' | 'height';

const OPTIONS: { value: BuildingColorMode; label: string }[] = [
    { value: 'damage', label: 'Hasar Durumu' },
    { value: 'height', label: 'Yüksekliğe Göre' },
];

interface BuildingColorModeControlProps {
    value: BuildingColorMode;
    onChange: (mode: BuildingColorMode) => void;
}

/** Bina renklendirme modu seçimi (yalnızca 3B operasyon görünümünde, `mode="operations"`) —
 *  `MapLayerControls`/`BuildingLegend` ile aynı collapsible floating-panel deseni. Varsayılan
 *  kapalı başlar (harita ilk açıldığında yer kaplamaz), `BuildingLegend`in karşı köşesinde durur. */
export const BuildingColorModeControl: React.FC<BuildingColorModeControlProps> = ({ value, onChange }) => {
    const [open, setOpen] = useState(false);

    return (
        <div className="absolute bottom-8 right-2.5 z-10 bg-white/95 backdrop-blur rounded-lg shadow-lg border border-slate-200 text-xs">
            <button
                type="button"
                onClick={() => setOpen((v) => !v)}
                aria-expanded={open}
                aria-controls="building-color-mode-panel"
                className="w-full flex items-center gap-1.5 px-3 py-2 font-medium text-slate-700"
            >
                <Palette size={13} className="text-slate-400" />
                Bina Renklendirme
                {open ? <ChevronDown size={13} className="ml-auto" /> : <ChevronUp size={13} className="ml-auto" />}
            </button>
            {open && (
                <div id="building-color-mode-panel" role="radiogroup" aria-label="Bina Renklendirme" className="px-3 pb-2.5 space-y-1.5">
                    {OPTIONS.map((opt) => (
                        <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
                            <input
                                type="radio"
                                name="building-color-mode"
                                checked={value === opt.value}
                                onChange={() => onChange(opt.value)}
                                className="text-blue-600 focus:ring-blue-500"
                            />
                            <span className="text-slate-700">{opt.label}</span>
                        </label>
                    ))}
                </div>
            )}
        </div>
    );
};
