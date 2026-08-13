package com.afet.koordinasyon.dto.response;

import java.util.UUID;

/**
 * Global search sonuç öğesi. Route bilgisi kasıtlı olarak yok — tip→route eşlemesi frontend'de
 * merkezi bir yerde (notificationNavigation.ts deseni) yapılır.
 */
public record SearchResultItem(
        String type,
        UUID id,
        String title,
        String subtitle,
        String statusLabel
) {}
