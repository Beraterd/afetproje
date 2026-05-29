package com.afet.koordinasyon.domain.enums;

/**
 * Canonical template keys for emergency status messages.
 * These are the authoritative values shared between backend and frontend
 * via the GET /api/users/me/emergency-status-messages/templates endpoint.
 */
public enum EmergencyStatusTemplateKey {
    SAFE_AND_WELL,
    AREA_UNCERTAIN,
    OUTSIDE_AND_SAFE,
    HOME_DAMAGED_BUT_SAFE,
    NEED_HELP,
    TRAPPED_UNDER_RUBBLE
}
