import axiosInstance from './axiosInstance';
import { NotificationResponse, NotificationType, UnreadCountResponse } from '@/types';
import { PagedResponse } from '@/types';

export const getNotifications = async (params?: {
    page?: number;
    size?: number;
    type?: NotificationType;
    isRead?: boolean;
}): Promise<PagedResponse<NotificationResponse>> => {
    const res = await axiosInstance.get<PagedResponse<NotificationResponse>>('/notifications', { params });
    return res.data;
};

export const getUnreadCount = async (): Promise<UnreadCountResponse> => {
    const res = await axiosInstance.get<UnreadCountResponse>('/notifications/unread-count');
    return res.data;
};

export const markNotificationRead = async (id: string): Promise<void> => {
    await axiosInstance.patch(`/notifications/${id}/read`);
};

export const markAllNotificationsRead = async (): Promise<void> => {
    await axiosInstance.patch('/notifications/read-all');
};
