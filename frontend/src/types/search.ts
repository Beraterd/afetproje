export type SearchResultType = 'EVENT' | 'DAMAGE_ASSESSMENT' | 'TEAM' | 'RESOURCE_REQUEST' | 'USER' | 'LOCATION';

export interface SearchResultItem {
    type: SearchResultType;
    id: string;
    title: string;
    subtitle?: string;
    statusLabel?: string;
}

export interface GlobalSearchResponse {
    query: string;
    results: SearchResultItem[];
}
