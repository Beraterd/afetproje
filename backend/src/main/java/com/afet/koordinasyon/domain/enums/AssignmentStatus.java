package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssignmentStatus {
    ACTIVE("Görevli Yönlendirildi"),
    FIELD_VERIFIED("Sahada Doğrulandı"),
    COMPLETED("Tamamlandı");

    private final String label;
}
