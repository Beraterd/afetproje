package com.afet.koordinasyon.domain.enums;

public enum AuditActionType {
    // Kullanıcı işlemleri
    USER_LOGIN,
    USER_LOGOUT,
    USER_CREATED,
    USER_UPDATED,
    USER_DEACTIVATED,
    USER_ACTIVATED,
    ROLE_CHANGED,

    // Koordinatör atama
    COORDINATOR_ASSIGNED,

    // Olay yönetimi
    EVENT_CREATED,
    EVENT_UPDATED,
    EVENT_CLOSED,
    EVENT_JOINED,
    EVENT_LEFT,

    // Ekip ataması
    TEAM_ASSIGNED,

    // Belge işlemleri
    DOCUMENT_UPLOADED,
    DOCUMENT_APPROVED,
    DOCUMENT_REJECTED,

    // Hasar / Kaynak
    DAMAGE_REPORT_CREATED,
    RESOURCE_REQUEST_CREATED,

    // Bildirim / İletişim
    NOTIFICATION_SENT,
    SMS_SENT,
    MAIL_SENT,

    // Deprem / Sistem
    EARTHQUAKE_AUTO_TRIGGER,
    SIMULATION_RESULT,
}
