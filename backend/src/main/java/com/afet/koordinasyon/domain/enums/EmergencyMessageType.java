package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * §4/§6/§7 — Deprem bildirimi e-postasındaki "Yakınlarıma Mesaj Gönder" hazır mesaj tipleri.
 * Mesaj metni SABİT'tir ve yalnızca bu enum üzerinden belirlenir; serbest metin link üzerinden
 * KABUL EDİLMEZ (güvenlik). {@code label} e-postadaki buton metnidir, {@code messageText}
 * yakınlara giden hazır cümledir.
 */
@Getter
@RequiredArgsConstructor
public enum EmergencyMessageType {
    DURUMUM_IYI("Durumum iyi", "Durumum iyi."),
    GUVENDEYIM("Güvendeyim", "Güvendeyim, endişelenmeyin."),
    YARDIM_LAZIM("Yardıma ihtiyacım var", "Yardıma ihtiyacım var."),
    ENKAZ_ALTINDA("Enkaz altındayım", "Enkaz altındayım."),
    TOPLANMA_ALANINA("Toplanma alanına gidiyorum", "Toplanma alanına gidiyorum."),
    TELEFON_KAPANABILIR("Telefonum kapanabilir", "Telefonum kapanabilir, konumumu takip edin.");

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
