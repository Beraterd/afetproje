package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceType {
    WATER("Su"),
    FOOD("Gıda"),
    TENT("Çadır"),
    BLANKET("Battaniye"),
    GENERATOR("Jeneratör"),
    HEATING("Isınma"),
    HEAVY_MACHINERY("İş Makinesi"),
    MEDICAL_SUPPORT("Tıbbi Destek"),
    HYGIENE("Hijyen"),
    OTHER("Diğer");

    private final String label;
}
