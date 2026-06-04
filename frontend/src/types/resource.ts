export interface ResourceRequestResponse {
    id: string;
    districtId: string;
    districtName: string;
    neighborhoodId?: string;
    neighborhoodName?: string;
    requestScope: string;
    resourceType: string;
    resourceTypeLabel: string;
    productName?: string;
    quantity?: number;
    unit?: string;
    priority?: string;
    priorityLabel?: string;
    status: string;
    statusLabel: string;
    description?: string;
    createdBy?: string;
    closedBy?: string;
    createdAt: string;
    updatedAt: string;
    closedAt?: string;
}

export interface CreateResourceRequestRequest {
    districtId: string;
    neighborhoodId: string;
    resourceType: string;
    name?: string;
    quantity: number;
    unit?: string;
    description?: string;
}

export interface UpdateResourceStatusRequest {
    status: string;
}

export interface ResourceSummaryResponse {
    neighborhoodId: string;
    districtId: string;
    openCount: number;
}

export const RESOURCE_TYPES = [
    { value: 'WATER', label: 'Su' },
    { value: 'FOOD', label: 'Gıda' },
    { value: 'TENT', label: 'Çadır' },
    { value: 'BLANKET', label: 'Battaniye' },
    { value: 'GENERATOR', label: 'Jeneratör' },
    { value: 'HEATING', label: 'Isıtma Ekipmanı' },
    { value: 'HEAVY_MACHINERY', label: 'Ağır İş Makinesi' },
    { value: 'MEDICAL_SUPPORT', label: 'Tıbbi Destek' },
    { value: 'HYGIENE', label: 'Hijyen Malzemesi' },
    { value: 'OTHER', label: 'Diğer' },
];

export const RESOURCE_STATUSES = [
    { value: 'OPEN', label: 'Açık' },
    { value: 'IN_PROGRESS', label: 'Devam Ediyor' },
    { value: 'FULFILLED', label: 'Karşılandı' },
    { value: 'CANCELLED', label: 'İptal Edildi' },
];
