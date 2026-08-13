package com.afet.koordinasyon.client;

import com.afet.koordinasyon.client.dto.KandilliEventDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Item 19/20 — Kandilli parser'ı gerçek koeri.boun.edu.tr/scripts/lst0.asp formatının
 * (2026-08'de manuel doğrulanmış) küçük, izin verilen bir fixture'ına karşı test eder.
 * GERÇEK AĞA BAĞLANMAZ.
 */
class KandilliEarthquakeClientTest {

    private final KandilliEarthquakeClient client =
            new KandilliEarthquakeClient(new RestTemplateBuilder(), "http://unused.invalid");

    private String loadFixture() throws IOException {
        ClassPathResource resource = new ClassPathResource("kandilli/sample_response.html");
        return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }

    private Optional<KandilliEventDto> findByLat(List<KandilliEventDto> events, double lat) {
        return events.stream().filter(e -> Math.abs(e.latitude() - lat) < 1e-6).findFirst();
    }

    @Test
    void validFixture_parsesExpectedNumberOfValidRows() throws IOException {
        List<KandilliEventDto> events = client.parse(loadFixture());

        // 7 geçerli satır: Sogucak-Afsin, Yunanistan, Milli Park-Kusadasi, Samli, Agaclipinar(MD),
        // Sisam Adasi(Ege Denizi), Yukarikaragoz-Pinarbasi. "Bayat-Soma" (büyüklük yok) ve
        // "BOZUK SATIR..." (format uymuyor) VE başlık/ayraç satırları elenir.
        assertThat(events).hasSize(7);
    }

    @Test
    void malformedRow_doesNotAbortParsing_othersStillProcessed() throws IOException {
        List<KandilliEventDto> events = client.parse(loadFixture());

        // "BOZUK SATIR BU FORMATA UYMUYOR VE ATLANMALI" satırından SONRA gelen geçerli
        // satırlar (Sisam Adası, Yukarikaragöz-Pınarbaşı) hâlâ işlenmiş olmalı.
        assertThat(findByLat(events, 37.8877).stream().count()).isGreaterThanOrEqualTo(1);
        assertThat(findByLat(events, 38.7448)).isPresent();
    }

    @Test
    void missingMagnitude_rowIsSkipped() throws IOException {
        List<KandilliEventDto> events = client.parse(loadFixture());

        // BAYAT-SOMA satırında MD=ML=Mw= "-.-" — büyüklüksüz deprem kaydı olamaz, atlanmalı.
        assertThat(findByLat(events, 39.1995)).isEmpty();
    }

    @Test
    void magnitudeSelection_prefersML_thenMw_thenMD() throws IOException {
        List<KandilliEventDto> events = client.parse(loadFixture());

        // Sogucak-Afsin: yalnızca ML=2.3 var.
        KandilliEventDto sogucak = findByLat(events, 38.1440).orElseThrow();
        assertThat(sogucak.resolvedMagnitude()).isEqualTo(2.3);
        assertThat(sogucak.resolvedMagnitudeType()).isEqualTo("ML");

        // Yunanistan: ML=3.0 VE Mw=3.1 var -> ML tercih edilir.
        KandilliEventDto yunanistan = findByLat(events, 40.0868).orElseThrow();
        assertThat(yunanistan.resolvedMagnitude()).isEqualTo(3.0);
        assertThat(yunanistan.resolvedMagnitudeType()).isEqualTo("ML");

        // Agaclipinar-Kurtalan: yalnızca MD=2.0 var (ML/Mw yok) -> MD kullanılır.
        KandilliEventDto agaclipinar = findByLat(events, 37.8877)
                .filter(e -> Math.abs(e.longitude() - 41.7123) < 1e-6 && e.magnitudeMD() != null)
                .orElseThrow();
        assertThat(agaclipinar.resolvedMagnitude()).isEqualTo(2.0);
        assertThat(agaclipinar.resolvedMagnitudeType()).isEqualTo("MD");
    }

    @Test
    void turkishLocation_rawLocationCapturedVerbatim() throws IOException {
        List<KandilliEventDto> events = client.parse(loadFixture());

        KandilliEventDto sogucak = findByLat(events, 38.1440).orElseThrow();
        assertThat(sogucak.rawLocation()).isEqualTo("SOGUCAK-AFSIN (KAHRAMANMARAS)");

        KandilliEventDto yunanistan = findByLat(events, 40.0868).orElseThrow();
        assertThat(yunanistan.rawLocation()).isEqualTo("YUNANISTAN");
    }

    @Test
    void emptyLine_isSkippedWithoutError() throws IOException {
        // Fixture'da boş bir satır var (Sisam Adası'ndan önce) — parse() exception fırlatmamalı.
        List<KandilliEventDto> events = client.parse(loadFixture());
        assertThat(events).isNotEmpty();
    }

    @Test
    void noPreBlock_returnsEmptyList_doesNotThrow() {
        List<KandilliEventDto> events = client.parse("<html><body>beklenmeyen format</body></html>");
        assertThat(events).isEmpty();
    }
}
