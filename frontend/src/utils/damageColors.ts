/** Hasar durumu → renk — tek kaynak. `DamageLevel` backend enum'unun 5 değerini kapsar
 *  (UNASSESSED/LIGHT/MODERATE/HEAVY/COLLAPSED). Leaflet'e bağımlı DEĞİL — hem Leaflet pin'leri
 *  (`mapIcons.ts`) hem maplibre-gl 3B extrusion (`BuildingMap.tsx`) buradan import eder, böylece
 *  maplibre route'unun bundle chunk'ına gereksiz yere Leaflet dahil edilmez. */
export const DAMAGE_STATUS_COLORS: Record<string, string> = {
    UNASSESSED: '#9ca3af',
    LIGHT: '#facc15',
    MODERATE: '#f97316',
    HEAVY: '#dc2626',
    COLLAPSED: '#7f1d1d',
};

/** Binaya bağlı hiç hasar kaydı yoksa (damageStatus === null) kullanılan nötr renk —
 *  `UNASSESSED` ("bildirildi ama değerlendirilmedi") ile karıştırılmamalı.
 *
 *  Önceki değer (#e2e8f0, slate-200) CARTO Light basemap'e neredeyse aynı tonda olduğu için canlı
 *  Kurtköy testinde binalar (çoğunluğu "rapor yok") pratikte görünmez haldeydi — mavi tonlu, gri-400
 *  ile (UNASSESSED) karışmayacak kadar farklı, ama hasar renkleri kadar dikkat çekmeyen bir ton. */
export const NO_REPORT_COLOR = '#7b93b8';
