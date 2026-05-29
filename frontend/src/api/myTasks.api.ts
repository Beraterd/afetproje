import axiosInstance from './axiosInstance';
import { DamageAssessmentTaskResponse } from '@/types';

export const getMyDamageAssessmentTasks = async (): Promise<DamageAssessmentTaskResponse[]> => {
    const res = await axiosInstance.get<DamageAssessmentTaskResponse[]>('/my-tasks/damage-assessments');
    return res.data;
};

export const getMyDamageAssessmentTaskDetail = async (assignmentId: string): Promise<DamageAssessmentTaskResponse> => {
    const res = await axiosInstance.get<DamageAssessmentTaskResponse>(`/my-tasks/damage-assessments/${assignmentId}`);
    return res.data;
};

export const uploadFieldPhotos = async (assignmentId: string, photos: File[]): Promise<void> => {
    const formData = new FormData();
    photos.forEach(photo => formData.append('photos', photo));
    await axiosInstance.post(`/my-tasks/damage-assessments/${assignmentId}/photos`, formData);
};

export const markFieldVerified = async (assignmentId: string): Promise<DamageAssessmentTaskResponse> => {
    const res = await axiosInstance.post<DamageAssessmentTaskResponse>(
        `/my-tasks/damage-assessments/${assignmentId}/field-verified`
    );
    return res.data;
};

export const completeAssessmentTask = async (assignmentId: string): Promise<DamageAssessmentTaskResponse> => {
    const res = await axiosInstance.post<DamageAssessmentTaskResponse>(
        `/my-tasks/damage-assessments/${assignmentId}/complete`
    );
    return res.data;
};
