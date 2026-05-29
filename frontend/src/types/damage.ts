export interface DamageAssessmentResponse {
    id: string;
    districtId: string;
    districtName: string;
    neighborhoodId: string;
    neighborhoodName: string;
    streetName?: string;
    buildingNo?: string;
    address: string;
    locationSource?: string;
    locationVerified: boolean;
    latitude?: number;
    longitude?: number;
    buildingType?: string;
    floorCount?: number;
    occupancyType?: string;
    damageLevel: string;
    damageLevelLabel: string;
    collapseRisk: boolean;
    emergencyEvacuationNeeded: boolean;
    casualtiesSuspected: boolean;
    blockedRoad: boolean;
    gasLeakRisk: boolean;
    note?: string;
    verificationStatus: string;
    verificationStatusLabel: string;
    reportedBy?: string;
    verifiedBy?: string;
    verifiedAt?: string;
    approvedBy?: string;
    approvedAt?: string;
    photoUrls?: string[];
    reporterPhotoUrls?: string[];
    fieldPhotoUrls?: string[];
    assignments?: DamageAssessmentAssignmentInfo[];
    aiComment?: string;
    aiConfidence?: 'LOW' | 'MEDIUM' | 'HIGH';
    aiConfidenceLabel?: string;
    aiModel?: string;
    aiAnalyzedAt?: string;
    aiAnalysisStatus?: 'NOT_STARTED' | 'PROCESSING' | 'PENDING' | 'COMPLETED' | 'FAILED';
    createdAt: string;
    updatedAt: string;
}

export interface DamageAssessmentAssignmentInfo {
    id: string;
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    phone?: string;
    assignedById: string;
    assignedByFirstName: string;
    assignedByLastName: string;
    assignedAt: string;
}

export interface EligibleAssigneeResponse {
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    phone?: string;
    eventId: string;
    eventTitle: string;
}

export interface DamageAssessmentTaskResponse {
    assignmentId: string;
    damageAssessmentId: string;
    address: string;
    districtName: string;
    neighborhoodName: string;
    damageLevel: string;
    damageLevelLabel: string;
    latitude?: number;
    longitude?: number;
    note?: string;
    collapseRisk: boolean;
    emergencyEvacuationNeeded: boolean;
    casualtiesSuspected: boolean;
    gasLeakRisk: boolean;
    assignmentStatus: 'ACTIVE' | 'FIELD_VERIFIED' | 'COMPLETED';
    assignmentStatusLabel: string;
    assignedByName: string;
    assignedAt: string;
    active: boolean;
    reporterPhotoUrls: string[];
    fieldPhotoUrls: string[];
    aiComment?: string;
    aiConfidence?: 'LOW' | 'MEDIUM' | 'HIGH';
    aiConfidenceLabel?: string;
    aiAnalysisStatus?: 'NOT_STARTED' | 'PROCESSING' | 'PENDING' | 'COMPLETED' | 'FAILED';
}

export interface AssignDamageAssessmentRequest {
    userId: string;
}

export interface CreateDamageAssessmentRequest {
    neighborhoodId: string;
    streetName?: string;
    buildingNo?: string;
    address: string;
    latitude: number;
    longitude: number;
    locationSource?: string;
    locationVerified?: boolean;
    buildingType?: string;
    floorCount?: number;
    occupancyType?: string;
    damageLevel?: string;
    collapseRisk?: boolean;
    emergencyEvacuationNeeded?: boolean;
    casualtiesSuspected?: boolean;
    blockedRoad?: boolean;
    gasLeakRisk?: boolean;
    note?: string;
    /** Offline-queue idempotency key — optional, set by offline sync layer */
    clientGeneratedId?: string;
}

export interface VerifyDamageAssessmentRequest {
    verificationStatus: string;
    note?: string;
}

export interface MapDamageSummaryResponse {
    neighborhoodId: string;
    totalCount: number;
    lightCount: number;
    moderateCount: number;
    heavyCount: number;
    collapsedCount: number;
}

export interface DamagePointResponse {
    id: string;
    latitude: number;
    longitude: number;
    damageLevel: string;
    damageLevelLabel: string;
    address?: string;
    verificationStatus: string;
    verificationStatusLabel: string;
    note?: string;
    districtId: string;
    districtName?: string;
    neighborhoodId: string;
    neighborhoodName?: string;
    gasLeakRisk: boolean;
    collapseRisk: boolean;
    emergencyEvacuationNeeded: boolean;
    casualtiesSuspected: boolean;
    createdAt: string;
}

export const DAMAGE_LEVELS = [
    { value: 'UNASSESSED', label: 'Değerlendirilmedi' },
    { value: 'LIGHT', label: 'Hafif' },
    { value: 'MODERATE', label: 'Orta' },
    { value: 'HEAVY', label: 'Ağır' },
    { value: 'COLLAPSED', label: 'Yıkılmış' },
];

export const VERIFICATION_STATUSES = [
    { value: 'INCELEME_GEREKIYOR', label: 'İnceleme Gerekiyor' },
    { value: 'ASSIGNED', label: 'Görevli Yönlendirildi' },
    { value: 'SAHADA_DOGRULANDI', label: 'Sahada Doğrulandı' },
    { value: 'KOORDINATOR_ONAYLADI', label: 'Koordinatör Onayladı' },
];
