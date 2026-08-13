import type { SearchResultItem, SearchResultType } from '@/types';

/**
 * Arama sonucu türü → hedef route eşlemesi. `notificationNavigation.ts` ile aynı desen: backend
 * route bilmez, yalnızca entity tipi + id döner; eşleme burada, tek merkezi yerde yapılır.
 * TEAM ve LOCATION için ayrı bir detay sayfası olmadığından en yakın operasyonel yüzeye yönlendirilir.
 */
const TYPE_ROUTE_BUILDERS: Record<SearchResultType, (item: SearchResultItem) => string> = {
    EVENT: (item) => `/events/${item.id}`,
    DAMAGE_ASSESSMENT: (item) => `/damage-assessments?selected=${item.id}`,
    RESOURCE_REQUEST: () => '/resource-requests',
    USER: () => '/admin/users',
    TEAM: () => '/coordination-center',
    LOCATION: () => '/map',
};

/** Bir arama sonucu için gidilecek uygulama içi rotayı döner; bilinmeyen tür için undefined. */
export function getSearchResultTarget(item: SearchResultItem): string | undefined {
    const builder = TYPE_ROUTE_BUILDERS[item.type];
    if (!builder) return undefined;
    return builder(item);
}

export const SEARCH_CATEGORY_LABEL_TR: Record<SearchResultType, string> = {
    EVENT: 'Olaylar',
    DAMAGE_ASSESSMENT: 'Hasar Tespitleri',
    TEAM: 'Ekipler',
    RESOURCE_REQUEST: 'Kaynak Talepleri',
    USER: 'Kullanıcılar',
    LOCATION: 'Mahalle / İlçe',
};
