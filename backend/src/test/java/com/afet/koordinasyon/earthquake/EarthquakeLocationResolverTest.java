package com.afet.koordinasyon.earthquake;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Item 5/14 — il/ilçe çözümlemesinin hem yapılandırılmış (AFAD) hem serbest metin
 * (Kandilli) girdiler için doğru davrandığını, belirsiz durumda UYDURMADIĞINI doğrular.
 */
class EarthquakeLocationResolverTest {

    private final EarthquakeLocationResolver resolver = new EarthquakeLocationResolver(new LocationNormalizer());

    // ── AFAD (yapılandırılmış alan) ──────────────────────────────────────────

    @Test
    void fromStructuredFields_validProvinceAndDistrict_passThroughWithCanonicalProvince() {
        var result = resolver.fromStructuredFields("Kütahya", "Simav");
        assertThat(result.province()).isEqualTo("Kütahya");
        assertThat(result.district()).isEqualTo("Simav");
        assertThat(result.provinceNormalized()).isEqualTo(LocationNormalizer.normalize("Kütahya"));
        assertThat(result.districtNormalized()).isEqualTo(LocationNormalizer.normalize("Simav"));
    }

    @Test
    void fromStructuredFields_unrecognizedProvince_doesNotFabricate() {
        var result = resolver.fromStructuredFields("Gürcistan", "Akyaka");
        assertThat(result.province()).isNull();
        assertThat(result.provinceNormalized()).isNull();
    }

    // ── Kandilli (serbest metin "Yer") ───────────────────────────────────────

    @Test
    void fromFreeText_placeDistrictProvincePattern_resolvesBoth() {
        var result = resolver.fromFreeText("SOGUCAK-AFSIN (KAHRAMANMARAS)");
        assertThat(result.province()).isEqualTo("Kahramanmaraş");
        assertThat(result.district()).isEqualTo("Afsin");
    }

    @Test
    void fromFreeText_multiWordPlaceWithDistrict_lastDashSegmentIsDistrict() {
        // "Milli Park" yer tanımı, "Kusadasi" gerçek ilçe — ilçe başka bir ile ait olamaz,
        // (Aydın), doğru şekilde Aydın'a bağlanmalı.
        var result = resolver.fromFreeText("MILLI PARK-KUSADASI (AYDIN)");
        assertThat(result.province()).isEqualTo("Aydın");
        assertThat(result.district()).isEqualTo("Kusadasi");
    }

    @Test
    void fromFreeText_noParentheses_foreignOrUnstructured_returnsNullBoth() {
        var result = resolver.fromFreeText("YUNANISTAN");
        assertThat(result.province()).isNull();
        assertThat(result.district()).isNull();
    }

    @Test
    void fromFreeText_parenthesesContentNotARealProvince_doesNotFabricateProvince() {
        // "Ege Denizi" bir il değildir — whitelist'te yok, uydurulmamalı.
        var result = resolver.fromFreeText("SISAM ADASI (EGE DENIZI)");
        assertThat(result.province()).isNull();
        assertThat(result.district()).isNull();
    }

    @Test
    void fromFreeText_emptyDistrictSegment_provinceKeptButDistrictLeftNull() {
        // "SAMLI-(BALIKESIR)": parantezden önceki "-" optional-dash olarak tüketilir,
        // ilçe segmenti boş kalır -> il biliniyor ama ilçe BELİRSİZ, uydurulmaz.
        var result = resolver.fromFreeText("SAMLI-(BALIKESIR)");
        assertThat(result.province()).isEqualTo("Balıkesir");
        assertThat(result.district()).isNull();
    }

    @Test
    void fromFreeText_nullOrBlank_returnsEmptyResolution() {
        assertThat(resolver.fromFreeText(null).province()).isNull();
        assertThat(resolver.fromFreeText("").province()).isNull();
        assertThat(resolver.fromFreeText("   ").province()).isNull();
    }

    @Test
    void fromFreeText_districtNeverAssignedToWrongProvince() {
        // İki farklı satırda aynı ilçe adı farklı illerde geçse bile (örn. "Merkez"),
        // resolver HER ZAMAN parantez içindeki ile bağlar — çapraz-il karışıklığı olmaz.
        var kusadasi = resolver.fromFreeText("MILLI PARK-KUSADASI (AYDIN)");
        var pinarbasi = resolver.fromFreeText("YUKARIKARAGOZ-PINARBASI (KAYSERI)");
        assertThat(kusadasi.province()).isEqualTo("Aydın");
        assertThat(pinarbasi.province()).isEqualTo("Kayseri");
        assertThat(kusadasi.district()).isNotEqualTo(pinarbasi.district());
    }
}
