import axiosInstance from './axiosInstance';

export interface OperationsAiRequest {
    prompt: string;
}

export interface OperationsAiResponse {
    answer: string;
    generatedAt: string;
}

export const queryOperationsAi = async (
    request: OperationsAiRequest
): Promise<OperationsAiResponse> => {
    const res = await axiosInstance.post<OperationsAiResponse>(
        '/ai/operations-assistant',
        request,
        { timeout: 90000 }
    );
    return res.data;
};
