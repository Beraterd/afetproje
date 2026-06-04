export interface ResourceStockResponse {
    id: string;
    name: string;
    category: string;
    categoryLabel: string;
    quantity: number;
    unit: string;
    dailyUsageEstimate?: number | null;
    criticalThreshold: number;
    daysRemaining?: number | null;
    status: string;
    statusLabel: string;
    districtId: string;
    districtName: string;
    neighborhoodId?: string | null;
    neighborhoodName?: string | null;
    warehouseName?: string | null;
    notes?: string | null;
    active: boolean;
    updatedBy?: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface ResourceStockSummaryResponse {
    totalItems: number;
    criticalCount: number;
    outOfStockCount: number;
    averageDaysRemaining?: number | null;
}

export interface ResourceStockMovementResponse {
    id: string;
    movementType: string;
    movementTypeLabel: string;
    quantityChange: number;
    previousQuantity: number;
    newQuantity: number;
    reason?: string | null;
    createdBy?: string | null;
    createdAt: string;
}

export interface StockLookupResponse {
    hasStock: boolean;
    totalQuantity: number;
    unit?: string;
    status?: string;
    statusLabel?: string;
    daysRemaining?: number | null;
    message: string;
}

export interface CreateResourceStockRequest {
    /** Bazı kategorilerde zorunlu, bazılarında backend kategori label'ından türetir. */
    name?: string | null;
    category: string;
    quantity: number;
    unit: string;
    dailyUsageEstimate?: number | null;
    criticalThreshold?: number | null;
    districtId: string;
    neighborhoodId: string;
    notes?: string | null;
}

/** Ürün adı sorulan kategoriler (diğerlerinde isim kategori adından türetilir). */
export const NAME_REQUIRED_CATEGORIES = ['FOOD', 'HYGIENE', 'MEDICAL_SUPPORT', 'HEAVY_MACHINERY', 'OTHER'];

/** Kategoriye göre ürün adı placeholder'ı. */
export const PRODUCT_NAME_PLACEHOLDER: Record<string, string> = {
    FOOD: 'Örn. konserve, bebek maması, kuru gıda',
    HYGIENE: 'Örn. bebek bezi, ped, sabun',
    MEDICAL_SUPPORT: 'Örn. serum, ilk yardım çantası, ilaç',
    HEAVY_MACHINERY: 'Örn. ekskavatör, vinç, kepçe',
    OTHER: 'Ürün/kaynak adı',
};

export interface UpdateStockQuantityRequest {
    newQuantity: number;
    movementType?: string;
    reason?: string;
}

/** Stok durum rozet renkleri */
export const STOCK_STATUS_BADGE: Record<string, string> = {
    SUFFICIENT: 'bg-green-100 text-green-800',
    DECREASING: 'bg-yellow-100 text-yellow-800',
    CRITICAL: 'bg-orange-100 text-orange-800',
    OUT_OF_STOCK: 'bg-red-100 text-red-800',
};
