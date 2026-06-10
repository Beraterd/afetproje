package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Deprem bildirimi e-postasındaki "Yakınlarıma Mesaj Gönder" hazır mesaj tipleri.
 * Mesaj metni SABİT'tir ve yalnızca bu enum üzerinden belirlenir; serbest metin link üzerinden
 * KABUL EDİLMEZ (güvenlik). {@code label} e-postadaki buton metnidir, {@code messageText}
 * yakınlara giden hazır cümledir.
 */
@Getter
@RequiredArgsConstructor
public enum EmergencyMessageType {
    DURUMUM_IYI(
        "Durumum İyi 👍",
        "Ben güvendeyim, endişelenmeyin."),
    TOPLANMA_ALANINA(
        "Toplanma Alanına Gidiyorum 📍",
        "Belirlenen toplanma alanına gidiyorum, gerekirse ulaşabilirsiniz."),
    ENKAZ_ALTINDA(
        "Enkaz Altındayım 🚨",
        "Enkaz altında kaldım, lütfen yardım isteyin!");

    private final String label;
    private final String messageText;

    /** Geçersiz/eksik değerlerde exception fırlatır — serbest metin engellenir. */
    public static EmergencyMessageType fromParam(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Mesaj tipi zorunludur");
        }
        return EmergencyMessageType.valueOf(value.trim().toUpperCase());
    }
}
