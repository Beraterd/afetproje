import React, { useState } from 'react';
import { Layers, ChevronDown, ChevronUp } from 'lucide-react';
import { LAYER_ITEMS, LayerKey, LayerState } from './operationMapLayers';

interface MapLayerControlsProps {
    layers: LayerState;
    /** Her layer için opsiyonel sayı rozeti (ör. görünür pin sayısı). */
    counts?: Partial<Record<LayerKey, number>>;
    onChange: (key: LayerKey) => void;
}

/**
 * Kompakt katman kontrol paneli. Masaüstünde haritanın sağ üstünde küçük bir floating panel,
 * mobilde alttan açılan bir sheet gibi davranır (ayrı bir media-query hook'una gerek kalmadan
 * sadece responsive class'larla). Varsayılan olarak kapalı başlar — harita ilk açıldığında
 * gereksiz yer kaplamaz.
 */
export const MapLayerControls: React.FC<MapLayerControlsProps> = ({ layers, counts, onChange }) => {
    const [open, setOpen] = useState(false);
    const activeCount = LAYER_ITEMS.filter((item) => layers[item.key]).length;

    return (
        <div
            className="absolute z-[1000] left-1/2 -translate-x-1/2 bottom-3 w-[calc(100%-24px)] max-w-sm
                       sm:left-auto sm:translate-x-0 sm:bottom-auto sm:top-3 sm:right-3 sm:w-auto sm:max-w-none"
        >
            <div className="bg-white rounded-lg shadow-lg border border-gray-200 overflow-hidden sm:min-w-[240px]">
                <button
                    onClick={() => setOpen((v) => !v)}
                    aria-expanded={open}
                    aria-controls="map-layer-panel"
                    className="flex items-center gap-2 px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 w-full"
                >
                    <Layers className="h-4 w-4 text-blue-600" aria-hidden="true" />
                    Katmanlar
                    <span className="text-xs font-normal text-gray-400">({activeCount})</span>
                    {open ? (
                        <ChevronUp className="ml-auto h-4 w-4 text-gray-400" aria-hidden="true" />
                    ) : (
                        <ChevronDown className="ml-auto h-4 w-4 text-gray-400" aria-hidden="true" />
                    )}
                </button>
                {open && (
                    <div id="map-layer-panel" className="border-t border-gray-100 p-2 space-y-1">
                        {LAYER_ITEMS.map(({ key, label, color }) => {
                            const count = counts?.[key];
                            return (
                                <label
                                    key={key}
                                    className="flex items-center gap-2 px-2 py-1.5 rounded hover:bg-gray-50 cursor-pointer"
                                >
                                    <input
                                        type="checkbox"
                                        checked={layers[key]}
                                        onChange={() => onChange(key)}
                                        className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                    />
                                    <span className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${color}`} aria-hidden="true" />
                                    <span className="text-xs text-gray-700 flex-1">{label}</span>
                                    {typeof count === 'number' && count > 0 && (
                                        <span className="text-[11px] font-medium text-gray-500 bg-gray-100 rounded-full px-1.5 py-0.5">
                                            {count}
                                        </span>
                                    )}
                                </label>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
};
