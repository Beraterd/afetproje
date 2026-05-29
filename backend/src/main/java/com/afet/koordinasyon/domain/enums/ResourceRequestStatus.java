package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceRequestStatus {
    OPEN("Açık"),
    IN_PROGRESS("İşlemde"),
    FULFILLED("Karşılandı"),
    CANCELLED("İptal Edildi");

    private final String label;
}
