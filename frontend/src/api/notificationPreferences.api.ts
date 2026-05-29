import axiosInstance from './axiosInstance';
import { NotificationPreferences, UpdateNotificationPreferencesRequest } from '@/types';

export const getNotificationPreferences = async (): Promise<NotificationPreferences> => {
    const res = await axiosInstance.get<NotificationPreferences>('/users/me/notification-preferences');
    return res.data;
};

export const updateNotificationPreferences = async (
    data: UpdateNotificationPreferencesRequest
): Promise<NotificationPreferences> => {
    const res = await axiosInstance.put<NotificationPreferences>('/users/me/notification-preferences', data);
    return res.data;
};
