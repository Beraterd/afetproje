import { DAMAGE_STATUS_COLORS, NO_REPORT_COLOR } from '@/utils/damageColors';
import { HEIGHT_COLOR_STOPS } from '@/config/map3d';
import type { BuildingColorMode } from './BuildingColorModeControl';
import type { BuildingMapMode } from './BuildingMap';

/** Hasar durumuna göre `fill-extrusion-color` match expression'ı. */
const damageColorExpression: unknown[] = [
    'match',
    ['get', 'damageStatus'],
    'COLLAPSED', DAMAGE_STATUS_COLORS.COLLAPSED,
    'HEAVY', DAMAGE_STATUS_COLORS.HEAVY,
    'MODERATE', DAMAGE_STATUS_COLORS.MODERATE,
    'LIGHT', DAMAGE_STATUS_COLORS.LIGHT,
    'UNASSESSED', DAMAGE_STATUS_COLORS.UNASSESSED,
    NO_REPORT_COLOR,
];

/** Tahmini yüksekliğe göre `fill-extrusion-color` interpolate expression'ı (madde 9). */
const heightColorExpression: unknown[] = [
    'interpolate',
    ['linear'],
    ['get', 'estimatedHeightM'],
    ...HEIGHT_COLOR_STOPS.flatMap((s) => [s.stop, s.color]),
];

/** mode="selector" (Hasar Tespiti bina seçici) için nötr tek renk — burada amaç mevcut hasar
 *  durumunu göstermek değil, kullanıcının rapor edeceği binayı seçmesidir. */
const SELECTOR_FILL_COLOR = '#93c5fd';

/** `mode`/`colorMode` kombinasyonuna göre extrusion dolgu rengi expression'ı — mount kurulumunda
 *  ve `colorMode` değiştiğinde `setPaintProperty` ile aynı fonksiyon çağrılır (tek kaynak). */
export function buildFillColorExpression(mode: BuildingMapMode, colorMode: BuildingColorMode): unknown {
    if (mode !== 'operations') return SELECTOR_FILL_COLOR;
    return colorMode === 'height' ? heightColorExpression : damageColorExpression;
}

/** `mode`/`colorMode` kombinasyonuna göre extrusion opaklığı — SABİT bir değer olmak ZORUNDA.
 *  MapLibre/Mapbox style-spec'te `fill-extrusion-opacity` data-driven (`['get', ...]`) expression
 *  DESTEKLEMEZ (yalnızca sabit değer veya zoom-only expression kabul eder) — önceki
 *  `['case', ['==', ['get','damageStatus'], null], 0.72, 0.88]` bu yüzden `addLayer`'ın MapLibre
 *  tarafından reddedilmesine yol açıyordu (canlı Kurtköy testinde bulunan, `@maplibre/maplibre-gl-
 *  style-spec`'in `validateStyleMin`'i ile doğrulanan gerçek bug: "data expressions not supported").
 *  "Rapor yok" binaların hasarlılardan daha soluk görünmesi artık YALNIZCA renkle (NO_REPORT_COLOR,
 *  damage renklerinden belirgin şekilde farklı) sağlanıyor, opacity ile değil. */
export function buildFillOpacityExpression(mode: BuildingMapMode, colorMode: BuildingColorMode): unknown {
    if (mode !== 'operations') return 0.75;
    if (colorMode === 'height') return 0.8;
    return 0.85;
}

/** Gerçek (production) yükseklik expression'ı — hem ilk layer kurulumunda hem debug panelinin
 *  "kapat" işleminde tek kaynaktan kullanılsın diye ayrı bir fonksiyon. */
export function buildHeightExpression(extrusionMinZoom: number): unknown {
    return ['step', ['zoom'], 0, extrusionMinZoom, ['get', 'estimatedHeightM']];
}
