import type { Role } from '@/types';

export interface LayerState {
    risk: boolean;
    damage: boolean;
    resources: boolean;
    districtCenters: boolean;
    neighborhoodCenters: boolean;
}

export type LayerKey = keyof LayerState;

export interface LayerMeta {
    key: LayerKey;
    label: string;
    color: string;
}

export const LAYER_ITEMS: LayerMeta[] = [
    { key: 'risk',                label: 'Risk',                             color: 'bg-green-500' },
    { key: 'damage',               label: 'Hasar Tespiti',                   color: 'bg-orange-500' },
    { key: 'resources',            label: 'Kaynak Talepleri',                color: 'bg-blue-500' },
    { key: 'districtCenters',      label: 'İlçe Merkezleri',                 color: 'bg-indigo-600' },
    { key: 'neighborhoodCenters',  label: 'Mahalle Merkezleri',              color: 'bg-violet-600' },
];

/**
 * Role'e göre haritanın açılış layer kombinasyonu. Amaç: kullanıcının gerçekten ihtiyaç
 * duyduğu bilgiyi ilk bakışta göstermek, ama tüm layerları birden açıp marker karmaşası
 * yaratmamak.
 *
 * - risk + damage evrensel açık: ikisi de temel durumsal farkındalık için gerekli ve risk
 *   katmanı (poligon dolgusu) marker kalabalığı yaratmıyor.
 * - resources + districtCenters yalnızca İlçe Koordinatörü'nde açık: ilçe çapında kaynak/merkez
 *   yönetimi onun asıl işi.
 * - neighborhoodCenters yalnızca Mahalle Koordinatörü'nde açık: kendi mahallesinin merkezini
 *   yönetiyor.
 * - Admin taslak/evrensel görünümle başlar (risk+damage) — zaten her katmana serbestçe
 *   erişebiliyor, varsayılanı kalabalıklaştırmaya gerek yok.
 */
export function getDefaultLayersForRole(role: Role | undefined): LayerState {
    const base: LayerState = {
        risk: true,
        damage: true,
        resources: false,
        districtCenters: false,
        neighborhoodCenters: false,
    };

    switch (role) {
        case 'DISTRICT_COORDINATOR':
            return { ...base, resources: true, districtCenters: true };
        case 'NEIGHBORHOOD_COORDINATOR':
            return { ...base, neighborhoodCenters: true };
        case 'VOLUNTEER':
        case 'ADMIN':
        default:
            return base;
    }
}
