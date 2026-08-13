import axiosInstance from './axiosInstance';
import { GlobalSearchResponse } from '@/types';

export const searchGlobal = async (q: string): Promise<GlobalSearchResponse> => {
    const res = await axiosInstance.get<GlobalSearchResponse>('/search', { params: { q } });
    return res.data;
};
