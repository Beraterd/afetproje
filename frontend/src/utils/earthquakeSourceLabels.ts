import { EarthquakeSource } from '@/types';

// Kaynak badge'leri KASITLI olarak eşit görsel ağırlıkta — biri diğerinden "daha güvenilir"
// gösterilmez, yalnızca ayırt edici renk kullanılır. Hem EarthquakesPage hem EarthquakeMap
// aynı eşlemeyi kullanır (tek kaynak).
export const SOURCE_BADGE_CLASSES: Record<EarthquakeSource, string> = {
    AFAD: 'bg-blue-50 text-blue-700',
    KANDILLI: 'bg-purple-50 text-purple-700',
};

export const SOURCE_LABELS: Record<EarthquakeSource, string> = {
    AFAD: 'AFAD',
    KANDILLI: 'Kandilli',
};
