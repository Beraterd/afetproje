export type NotificationType =
    | 'DOCUMENT_APPROVAL'
    | 'RESOURCE_REQUEST'
    | 'TEAM_NEED'
    | 'DAMAGE_REPORT'
    | 'NEW_EARTHQUAKE'
    | 'SIMULATION_RESULT'
    | 'MESSAGE_DELIVERY_STATUS';

export interface NotificationResponse {
    id: string;
    type: NotificationType;
    title: string;
    message?: string;
    relatedEntityType?: string;
    relatedEntityId?: string;
    isRead: boolean;
    createdAt: string;
    readAt?: string;
}

export interface UnreadCountResponse {
    unreadCount: number;
}
