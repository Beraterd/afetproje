package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DamageLevel {
    UNASSESSED("Değerlendirilmedi"),
    LIGHT("Hafif Hasar"),
    MODERATE("Orta Hasar"),
    HEAVY("Ağır Hasar"),
    COLLAPSED("Yıkılmış");

    private final String label;
}
