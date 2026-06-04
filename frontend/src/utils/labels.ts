/**
 * Merkezi Türkçe etiket sistemi.
 * Backend enum değerleri İngilizce kalır; kullanıcıya burada Türkçe label gösterilir.
 * Aynı metinlerin farklı ekranlarda tekrar etmemesi için tek kaynak budur.
 */
import type { AuditActionType } from '@/types/audit';
import type { NotificationType } from '@/types/notification';

/** Bilinmeyen enum için okunabilir fallback: USER_LOGIN -> "Kullanıcı login" değil, "User login" tarzı yerine
 *  alt çizgileri boşluğa çevirip cümle düzenine getirir (ilk harf büyük, kalanı küçük). */
export function humanizeEnum(value: string | null | undefined): string {
    if (!value) return '—';
    const text = value.replace(/_/g, ' ').trim().toLocaleLowerCase('tr-TR');
    if (!text) return '—';
    return text.charAt(0).toLocaleUpperCase('tr-TR') + text.slice(1);
}

// ── Audit / işlem kaydı aksiyonları ─────────────────────────────────────────
export const auditActionLabels: Record<AuditActionType, string> = {
    USER_LOGIN: 'Kullanıcı girişi',
    USER_LOGOUT: 'Kullanıcı çıkışı',
    USER_CREATED: 'Kullanıcı kaydı',
    USER_UPDATED: 'Kullanıcı güncellendi',
    USER_DEACTIVATED: 'Kullanıcı pasifleştirildi',
    USER_ACTIVATED: 'Kullanıcı aktifleştirildi',
    ROLE_CHANGED: 'Rol değiştirildi',
    COORDINATOR_ASSIGNED: 'Koordinatör atandı',
    EVENT_CREATED: 'Olay oluşturuldu',
    EVENT_UPDATED: 'Olay güncellendi',
    EVENT_CLOSED: 'Olay kapatıldı',
    EVENT_JOINED: 'Olaya katılım',
    EVENT_LEFT: 'Olaydan ayrılma',
    TEAM_ASSIGNED: 'Ekip atandı',
    DOCUMENT_UPLOADED: 'Belge yüklendi',
    DOCUMENT_APPROVED: 'Belge onaylandı',
    DOCUMENT_REJECTED: 'Belge reddedildi',
    DAMAGE_REPORT_CREATED: 'Hasar bildirimi oluşturuldu',
    RESOURCE_REQUEST_CREATED: 'Kaynak talebi oluşturuldu',
    NOTIFICATION_SENT: 'Bildirim gönderildi',
    SMS_SENT: 'SMS gönderildi',
    MAIL_SENT: 'E-posta gönderildi',
    EARTHQUAKE_AUTO_TRIGGER: 'Deprem otomatik tetikleme',
    SIMULATION_RESULT: 'Simülasyon sonucu',
};

export function formatAuditAction(action: string): string {
    return auditActionLabels[action as AuditActionType] ?? humanizeEnum(action);
}

// ── Roller ───────────────────────────────────────────────────────────────────
export const roleLabels: Record<string, string> = {
    ADMIN: 'Yönetici',
    DISTRICT_COORDINATOR: 'İlçe Koordinatörü',
    NEIGHBORHOOD_COORDINATOR: 'Mahalle Koordinatörü',
    VOLUNTEER: 'Gönüllü',
};

export function formatRole(role: string | null | undefined): string {
    if (!role) return '—';
    return roleLabels[role] ?? humanizeEnum(role);
}

// ── Varlık türleri (audit entityType) ─────────────────────────────────────────
export const entityTypeLabels: Record<string, string> = {
    USER: 'Kullanıcı',
    EVENT: 'Olay',
    EARTHQUAKE: 'Deprem',
    EarthquakeEvent: 'Deprem',
    DOCUMENT: 'Belge',
    TEAM: 'Ekip',
    DAMAGE_REPORT: 'Hasar Bildirimi',
    DamageAssessment: 'Hasar Bildirimi',
    RESOURCE_REQUEST: 'Kaynak Talebi',
    ResourceRequest: 'Kaynak Talebi',
    SIMULATION: 'Simülasyon',
    NOTIFICATION: 'Bildirim',
};

export function formatEntityType(type: string | null | undefined): string {
    if (!type) return '—';
    return entityTypeLabels[type] ?? humanizeEnum(type);
}

// ── Bildirim türleri ──────────────────────────────────────────────────────────
export const notificationTypeLabels: Record<NotificationType, string> = {
    DOCUMENT_APPROVAL: 'Belge Onayı',
    RESOURCE_REQUEST: 'Kaynak Talebi',
    TEAM_NEED: 'Ekip İhtiyacı',
    DAMAGE_REPORT: 'Hasar Tespiti',
    NEW_EARTHQUAKE: 'Yeni Deprem',
    SIMULATION_RESULT: 'Simülasyon',
    MESSAGE_DELIVERY_STATUS: 'SMS/Mail Durumu',
};

export function formatNotificationType(type: string): string {
    return notificationTypeLabels[type as NotificationType] ?? humanizeEnum(type);
}
