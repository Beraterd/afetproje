package com.afet.koordinasyon.earthquake;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Item 14 — Türkçe karakter/case farklılıklarının filtrelemeyi bozmadığını doğrular. */
class LocationNormalizerTest {

    private final LocationNormalizer normalizer = new LocationNormalizer();

    @Test
    void differentCasingsOfIstanbul_normalizeToSameKey() {
        assertThat(LocationNormalizer.normalize("İstanbul"))
                .isEqualTo(LocationNormalizer.normalize("ISTANBUL"))
                .isEqualTo(LocationNormalizer.normalize("istanbul"))
                .isEqualTo(LocationNormalizer.normalize("İSTANBUL"));
    }

    @Test
    void canonicalizeProvince_matchesAllCasingVariants_toProperTurkishSpelling() {
        assertThat(normalizer.canonicalizeProvince("ISTANBUL")).isEqualTo("İstanbul");
        assertThat(normalizer.canonicalizeProvince("istanbul")).isEqualTo("İstanbul");
        assertThat(normalizer.canonicalizeProvince("İstanbul")).isEqualTo("İstanbul");
        assertThat(normalizer.canonicalizeProvince("KAHRAMANMARAS")).isEqualTo("Kahramanmaraş");
        assertThat(normalizer.canonicalizeProvince("kütahya")).isEqualTo("Kütahya");
    }

    @Test
    void canonicalizeProvince_unknownValue_returnsNull_doesNotFabricate() {
        assertThat(normalizer.canonicalizeProvince("EGE DENIZI")).isNull();
        assertThat(normalizer.canonicalizeProvince("YUNANISTAN")).isNull();
        assertThat(normalizer.canonicalizeProvince(null)).isNull();
        assertThat(normalizer.canonicalizeProvince("")).isNull();
        assertThat(normalizer.canonicalizeProvince("   ")).isNull();
    }

    @Test
    void allEightyOneProvinces_areCanonicalizable() {
        for (String province : TurkishProvinces.ALL) {
            assertThat(normalizer.canonicalizeProvince(province.toUpperCase(java.util.Locale.ROOT)))
                    .as("province=%s", province)
                    .isEqualTo(province);
        }
        assertThat(TurkishProvinces.ALL).hasSize(81);
    }
}
