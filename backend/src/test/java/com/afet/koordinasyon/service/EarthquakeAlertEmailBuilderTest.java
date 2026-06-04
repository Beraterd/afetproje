package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.AssemblyArea;
import com.afet.koordinasyon.domain.enums.EarthquakeSourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EarthquakeAlertEmailBuilderTest {

    private final EarthquakeAlertEmailBuilder builder = new EarthquakeAlertEmailBuilder();

    private EarthquakeAlert realAlert() {
        return new EarthquakeAlert(UUID.randomUUID(), EarthquakeSourceType.REAL,
                5.2, "Tuzla Açıkları", "İstanbul", "Tuzla",
                OffsetDateTime.now(), 40.8, 29.3, 7.4);
    }

    private EarthquakeAlert simAlert() {
        return new EarthquakeAlert(UUID.randomUUID(), EarthquakeSourceType.SIMULATION,
                6.0, "Kadıköy", null, "Kadıköy",
                OffsetDateTime.now(), null, null, null);
    }

    private AssemblyArea area(String name, String address, String mapsUrl, BigDecimal lat, BigDecimal lon) {
        return AssemblyArea.builder()
                .id(UUID.randomUUID())
                .name(name)
                .address(address)
                .googleMapsUrl(mapsUrl)
                .latitude(lat)
                .longitude(lon)
                .build();
    }

    @Test
    @DisplayName("Gerçek deprem mailinde toplanma alanları ad+adres+navigasyon linkiyle listelenir")
    void realEmail_listsAssemblyAreas() {
        AssemblyArea a = area("Moda Parkı", "Moda Cad. No:1", "https://maps.example/abc", null, null);
        String html = builder.buildHtml(realAlert(), "Ali Veli", List.of(a));

        assertThat(html).contains("Deprem Bildirimi");
        assertThat(html).contains("Moda Parkı");
        assertThat(html).contains("Moda Cad. No:1");
        assertThat(html).contains("https://maps.example/abc");
        assertThat(html).contains("AFAD ve yetkili kurumların duyurularını takip edin.");
        assertThat(html).contains("Bu bildirim afet koordinasyon platformu tarafından gönderilmiştir.");
    }

    @Test
    @DisplayName("googleMapsUrl yoksa navigasyon linki latitude/longitude'dan üretilir")
    void navUrl_fromLatLonWhenNoGoogleMapsUrl() {
        AssemblyArea a = area("Fenerbahçe Parkı", "Sahil Yolu", null,
                new BigDecimal("40.9700000"), new BigDecimal("29.0400000"));
        String html = builder.buildHtml(realAlert(), "Ali Veli", List.of(a));

        assertThat(html).contains("https://www.google.com/maps/dir/?api=1&destination=40.9700000,29.0400000");
    }

    @Test
    @DisplayName("Toplanma alanı yoksa mailde uygun boş durum metni yazar")
    void emptyAreas_showsEmptyStateMessage() {
        String html = builder.buildHtml(realAlert(), "Ali Veli", List.of());
        assertThat(html).contains(EarthquakeAlertEmailBuilder.NO_AREAS_MESSAGE);
    }

    @Test
    @DisplayName("Simülasyon mailinde [SİMÜLASYON] etiketi/başlığı bulunur")
    void simulation_hasSimulationLabel() {
        String subject = builder.buildSubject(simAlert());
        String html = builder.buildHtml(simAlert(), "Ali Veli", List.of());

        assertThat(subject).contains("[SİMÜLASYON]");
        assertThat(html).contains("Deprem Simülasyonu Bildirimi");
        assertThat(html).contains("SİMÜLASYON");
    }

    @Test
    @DisplayName("Gerçek deprem konusu [SİMÜLASYON] etiketi içermez")
    void real_subjectHasNoSimulationLabel() {
        assertThat(builder.buildSubject(realAlert())).doesNotContain("SİMÜLASYON");
    }

    @Test
    @DisplayName("Derinlik varsa mailde gösterilir, yoksa satır eklenmez")
    void depthRow_conditional() {
        assertThat(builder.buildHtml(realAlert(), "X Y", List.of())).contains("Derinlik");
        assertThat(builder.buildHtml(simAlert(), "X Y", List.of())).doesNotContain("Derinlik");
    }
}
