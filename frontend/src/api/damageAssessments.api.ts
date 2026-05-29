import axiosInstance from './axiosInstance';
import {
    DamageAssessmentResponse,
    DamageAssessmentAssignmentInfo,
    EligibleAssigneeResponse,
    AssignDamageAssessmentRequest,
    CreateDamageAssessmentRequest,
    VerifyDamageAssessmentRequest,
    MapDamageSummaryResponse,
    DamagePointResponse,
    PagedResponse,
} from '@/types';

export const getDamageAssessments = async (params?: {
    page?: number;
    size?: number;
    districtId?: string;
    neighborhoodId?: string;
    damageLevel?: string;
    verificationStatus?: string;
}): Promise<PagedResponse<DamageAssessmentResponse>> => {
    const res = await axiosInstance.get<PagedResponse<DamageAssessmentResponse>>('/damage-assessments', { params });
    return res.data;
};

export const getMyDamageAssessments = async (params?: {
    page?: number;
    size?: number;
}): Promise<PagedResponse<DamageAssessmentResponse>> => {
    const res = await axiosInstance.get<PagedResponse<DamageAssessmentResponse>>('/damage-assessments/my', { params });
    return res.data;
};

export const getDamageAssessmentById = async (id: string): Promise<DamageAssessmentResponse> => {
    const res = await axiosInstance.get<DamageAssessmentResponse>(`/damage-assessments/${id}`);
    return res.data;
};

export const createDamageAssessment = async (
    data: CreateDamageAssessmentRequest,
    photos: File[]
): Promise<DamageAssessmentResponse> => {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    photos.forEach(photo => formData.append('photos', photo));
    const res = await axiosInstance.post<DamageAssessmentResponse>('/damage-assessments', formData);
    return res.data;
};

export const verifyDamageAssessment = async (id: string, data: VerifyDamageAssessmentRequest): Promise<DamageAssessmentResponse> => {
    const res = await axiosInstance.patch<DamageAssessmentResponse>(`/damage-assessments/${id}/verify`, data);
    return res.data;
};

export const getMapDamageSummary = async (): Promise<MapDamageSummaryResponse[]> => {
    const res = await axiosInstance.get<MapDamageSummaryResponse[]>('/map/damage-summary');
    return res.data;
};

export const getMapDamagePoints = async (): Promise<DamagePointResponse[]> => {
    const res = await axiosInstance.get<DamagePointResponse[]>('/map/damage-points');
    return res.data;
};

export const getDamagePointsByNeighborhood = async (neighborhoodId: string): Promise<DamagePointResponse[]> => {
    const res = await axiosInstance.get<DamagePointResponse[]>(`/damage-assessments/by-neighborhood/${neighborhoodId}`);
    return res.data;
};

// ── Assignment endpoints ───────────────────────────────────────────────────

export const listDamageAssignments = async (damageAssessmentId: string): Promise<DamageAssessmentAssignmentInfo[]> => {
    const res = await axiosInstance.get<DamageAssessmentAssignmentInfo[]>(`/damage-assessments/${damageAssessmentId}/assignments`);
    return res.data;
};

export const assignDamageAssessment = async (damageAssessmentId: string, data: AssignDamageAssessmentRequest): Promise<DamageAssessmentAssignmentInfo> => {
    const res = await axiosInstance.post<DamageAssessmentAssignmentInfo>(`/damage-assessments/${damageAssessmentId}/assignments`, data);
    return res.data;
};

export const removeDamageAssignment = async (damageAssessmentId: string, assignmentId: string): Promise<void> => {
    await axiosInstance.delete(`/damage-assessments/${damageAssessmentId}/assignments/${assignmentId}`);
};

export const getEligibleAssignees = async (damageAssessmentId: string): Promise<EligibleAssigneeResponse[]> => {
    const res = await axiosInstance.get<EligibleAssigneeResponse[]>(`/damage-assessments/${damageAssessmentId}/eligible-assignees`);
    return res.data;
};

export const triggerAiAnalysis = async (damageAssessmentId: string): Promise<{ assessmentId: string; aiAnalysisStatus: string } | null> => {
    const res = await axiosInstance.post<{ assessmentId: string; aiAnalysisStatus: string }>(
        `/damage-assessments/${damageAssessmentId}/ai-analysis`
    );
    return res.data ?? null;
};
