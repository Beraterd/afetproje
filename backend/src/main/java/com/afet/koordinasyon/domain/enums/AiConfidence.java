package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiConfidence {
    LOW("Düşük"),
    MEDIUM("Orta"),
    HIGH("Yüksek");

    private final String label;
}
