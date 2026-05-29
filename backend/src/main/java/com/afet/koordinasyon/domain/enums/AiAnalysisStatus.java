package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiAnalysisStatus {
    NOT_STARTED("Başlatılmadı"),
    PROCESSING("Analiz ediliyor"),
    PENDING("Analiz ediliyor"),   // kept for backward compat with existing rows
    COMPLETED("Tamamlandı"),
    FAILED("Başarısız");

    private final String label;
}
