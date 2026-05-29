export type AuditActionType =
    | 'USER_LOGIN' | 'USER_LOGOUT' | 'USER_CREATED' | 'USER_UPDATED'
    | 'USER_DEACTIVATED' | 'USER_ACTIVATED' | 'ROLE_CHANGED'
    | 'COORDINATOR_ASSIGNED'
    | 'EVENT_CREATED' | 'EVENT_UPDATED' | 'EVENT_CLOSED' | 'EVENT_JOINED' | 'EVENT_LEFT'
    | 'TEAM_ASSIGNED'
    | 'DOCUMENT_UPLOADED' | 'DOCUMENT_APPROVED' | 'DOCUMENT_REJECTED'
    | 'DAMAGE_REPORT_CREATED' | 'RESOURCE_REQUEST_CREATED'
    | 'NOTIFICATION_SENT' | 'SMS_SENT' | 'MAIL_SENT'
    | 'EARTHQUAKE_AUTO_TRIGGER' | 'SIMULATION_RESULT';

export interface AuditLogResponse {
    id: string;
    actorId: string | null;
    actorName: string | null;
    actorRole: string | null;
    action: AuditActionType;
    entityType: string;
    entityId: string | null;
    description: string | null;
    metadata: string | null;
    ipAddress: string | null;
    userAgent: string | null;
    isSystemAction: boolean;
    createdAt: string;
}

export interface AuditLogFilter {
    actionType?: string;
    actorId?: string;
    actorRole?: string;
    entityType?: string;
    isSystemAction?: boolean;
    fromDate?: string;
    toDate?: string;
    search?: string;
    page?: number;
    size?: number;
}
