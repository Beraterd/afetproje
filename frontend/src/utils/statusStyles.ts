import type { BadgeVariant } from '@/components/ui/Badge';

/**
 * Uygulama çapında semantic durum renk sözlüğü — CRITICAL/HIGH/PENDING/SUCCESS/INFO.
 * Harita risk skalası (RiskColor), doğrulama durumu ve AI güven skoru gibi kendi
 * çok-seviyeli/ayrı anlam taşıyan domain renkleri kasıtlı olarak buraya dahil değildir.
 */
export type SemanticStatus = 'critical' | 'high' | 'pending' | 'success' | 'info';

export const SEMANTIC_BADGE_VARIANT: Record<SemanticStatus, BadgeVariant> = {
    critical: 'danger',
    high: 'high',
    pending: 'warning',
    success: 'success',
    info: 'info',
};

export const SEMANTIC_STATUS_LABEL_TR: Record<SemanticStatus, string> = {
    critical: 'Kritik',
    high: 'Yüksek',
    pending: 'Bekliyor',
    success: 'Tamamlandı',
    info: 'Bilgi',
};

/** Badge kullanmayan (raw Tailwind class string döndüren) yerler için aynı renklerin karşılığı. */
export const SEMANTIC_BADGE_CLASSES: Record<SemanticStatus, string> = {
    critical: 'bg-red-100 text-red-800',
    high: 'bg-orange-100 text-orange-800',
    pending: 'bg-yellow-100 text-yellow-800',
    success: 'bg-green-100 text-green-800',
    info: 'bg-blue-100 text-blue-800',
};
