package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VerificationStatus {
    INCELEME_GEREKIYOR("İnceleme Gerekiyor"),
    ASSIGNED("Görevli Yönlendirildi"),
    SAHADA_DOGRULANDI("Sahada Doğrulandı"),
    KOORDINATOR_ONAYLADI("Koordinatör Onayladı");

    private final String label;
}
