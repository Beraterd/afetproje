/** 3B bina katmanı — kamera/renk sabitleri, tüm desteklenen ilçelerde ortak. `BuildingMap.tsx`'in
 *  kendi içine gömdüğü magic number'ları tek yere toplar (bkz. navigation.ts ile aynı config/
 *  konvansiyonu). */

/** `initialBounds` hiç yoksa (normal akışta hep dolu gelir — yalnızca beklenmeyen bir durumda
 *  kullanılır) haritanın açılacağı generic İstanbul merkezi — herhangi bir ilçeye özgü değil. */
export const ISTANBUL_FALLBACK_CENTER: [number, number] = [29.02, 41.0];

export const MAP3D_CAMERA = {
    /** Mahalleye girişte / 3B moduna dönüşte kullanılan varsayılan eğik açı. */
    pitch: 55,
    bearing: -20,
    /** "Üstten gör (2B)" toggle'ının düz açısı. */
    flatPitch: 0,
    flatBearing: 0,
    /** flyTo/reset animasyon süresi (ms) — `prefers-reduced-motion` aktifken 0'a düşürülür. */
    flyDurationMs: 900,
    easeDurationMs: 500,
    /** Mahalle bounds'una fitBounds sonrası zoom üst sınırı — çok küçük mahallelerde (ör. birkaç
     *  düzine binalı) fitBounds aşırı yakınlaşabilir, bina seçimi için genel bağlam kaybolur.
     *  extrusionMinZoom (alt sınır) her zaman korunur, yalnızca ÜST sınır burada sabitlenir. */
    maxInitialZoom: 19,
} as const;

/** Seçili mahalle sınırı — teal ton, mevcut palete uygun, neon/glow değil. */
export const NEIGHBORHOOD_BOUNDARY_COLOR = '#0f766e';
export const NEIGHBORHOOD_BOUNDARY_FILL_OPACITY = 0.08;
export const NEIGHBORHOOD_BOUNDARY_LINE_WIDTH = 2.5;

/** "Yüksekliğe Göre" renk modu — düşük→açık mavi, orta→sarı, yüksek→turuncu/kırmızı gradient
 *  durakları (metre). `fill-extrusion-color`'da bir `interpolate` expression'ına beslenir. */
export const HEIGHT_COLOR_STOPS: { stop: number; color: string }[] = [
    { stop: 6, color: '#7dd3fc' },
    { stop: 15, color: '#facc15' },
    { stop: 30, color: '#fb923c' },
    { stop: 60, color: '#dc2626' },
];
