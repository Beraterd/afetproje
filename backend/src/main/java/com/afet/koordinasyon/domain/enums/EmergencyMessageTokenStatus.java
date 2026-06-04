package com.afet.koordinasyon.domain.enums;

/**
 * §6 — Acil mesaj e-posta token'ının yaşam döngüsü.
 * ACTIVE: kullanılabilir. USED: tek-kullanım tüketildi. EXPIRED: süresi doldu.
 */
public enum EmergencyMessageTokenStatus {
    ACTIVE,
    USED,
    EXPIRED
}
