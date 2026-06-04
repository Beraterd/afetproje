package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Kaynak talebi önceliği.
 */
@Getter
@RequiredArgsConstructor
public enum RequestPriority {
    LOW("Düşük"),
    MEDIUM("Orta"),
    HIGH("Yüksek"),
    CRITICAL("Kritik");

    private final String label;
}
