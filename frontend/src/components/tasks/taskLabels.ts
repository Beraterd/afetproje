/** Görevlerim ekranında paylaşılan saf etiket/renk/URL yardımcıları — API çağrısı yok. */

import { damageLevelColor } from '@/components/damage/damageDisplay';

export const TEAM_TR: Record<string, string> = {
    SEARCH_RESCUE: 'Arama Kurtarma Ekibi',
    FOOD_WATER: 'Yemek ve İçme Suyu Dağıtım Ekibi',
    EVACUATION: 'Tahliye Ekibi',
    COMMUNICATION: 'İletişim Ekibi',
    PSYCHOSOCIAL: 'Psikososyal Destek Ekibi',
    HASAR_TESPIT_EKIBI: 'Hasar Tespit Ekibi',
    OTHER: 'Diğer',
};

export const VOLUNTEER_STATUS_TR: Record<string, string> = {
    ASSIGNED: 'Aktif',
    COMPLETED: 'Tamamlandı',
    WITHDRAWN: 'Bırakıldı',
};

export const VOLUNTEER_STATUS_VARIANT: Record<string, 'success' | 'neutral' | 'warning'> = {
    ASSIGNED: 'success',
    COMPLETED: 'neutral',
    WITHDRAWN: 'warning',
};

export const ASSIGNMENT_STATUS_VARIANT: Record<string, 'success' | 'info' | 'neutral'> = {
    ACTIVE: 'success',
    FIELD_VERIFIED: 'info',
    COMPLETED: 'neutral',
};

/** damageDisplay.ts'teki damageLevelColor() ile aynı renk kaynağını paylaşır — iki yerde ayrı ayrı tanımlanmaz. */
export const DAMAGE_LEVEL_COLOR: Record<string, string> = {
    UNASSESSED: damageLevelColor('UNASSESSED'),
    LIGHT: damageLevelColor('LIGHT'),
    MODERATE: damageLevelColor('MODERATE'),
    HEAVY: damageLevelColor('HEAVY'),
    COLLAPSED: damageLevelColor('COLLAPSED'),
};

export const ACCEPTED_PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

export function mapsUrl(lat?: number, lng?: number, address?: string): string {
    if (lat && lng) {
        return `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
    }
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address || '')}`;
}
