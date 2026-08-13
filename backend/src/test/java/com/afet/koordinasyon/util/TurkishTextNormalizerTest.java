package com.afet.koordinasyon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TurkishTextNormalizerTest {

    @Test
    @DisplayName("Türkçe I/İ/ı/i varyantları aynı token'a katlanır")
    void foldsTurkishIVariants() {
        assertThat(TurkishTextNormalizer.normalize("İstanbul")).isEqualTo("istanbul");
        assertThat(TurkishTextNormalizer.normalize("Isparta")).isEqualTo("isparta");
        assertThat(TurkishTextNormalizer.normalize("ıspanak")).isEqualTo("ispanak");
    }

    @Test
    @DisplayName("ş/ç/ğ/ö/ü ASCII karşılıklarına katlanır")
    void foldsOtherTurkishLetters() {
        assertThat(TurkishTextNormalizer.normalize("Güzelyalı Çeşme Sokağı")).isEqualTo("guzelyali cesme sokagi");
    }

    @Test
    @DisplayName("Sokak/Sok/Sk varyantları aynı normalize metne düşer")
    void streetTypeAbbreviationsAreEquivalent() {
        String base = TurkishTextNormalizer.normalize("Selvi Boylum");
        assertThat(TurkishTextNormalizer.normalize("Selvi Boylum Sokak")).isEqualTo(base);
        assertThat(TurkishTextNormalizer.normalize("Selvi Boylum Sk")).isEqualTo(base);
        assertThat(TurkishTextNormalizer.normalize("Selvi Boylum Sk.")).isEqualTo(base);
        assertThat(TurkishTextNormalizer.normalize("SELVI BOYLUM")).isEqualTo(base);
        assertThat(TurkishTextNormalizer.normalize("selvi boylum")).isEqualTo(base);
    }

    @Test
    @DisplayName("Noktalama ve ayraçlar boşluğa çevrilir")
    void punctuationBecomesSpace() {
        assertThat(TurkishTextNormalizer.normalize("Adnan Menderes Bulvarı No:45")).isEqualTo("adnan menderes 45");
    }

    @Test
    @DisplayName("null girişte boş string döner")
    void nullInputReturnsEmpty() {
        assertThat(TurkishTextNormalizer.normalize(null)).isEmpty();
    }
}
