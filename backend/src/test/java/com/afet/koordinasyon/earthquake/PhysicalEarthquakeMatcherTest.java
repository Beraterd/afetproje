package com.afet.koordinasyon.earthquake;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.enums.EarthquakeRiskLevel;
import com.afet.koordinasyon.domain.enums.EarthquakeSource;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Item 4/17 — AFAD ve Kandilli'nin aynı fiziksel depremi bildirim spam'ine yol açmadan
 * tespit edip etmediğini, ama YANLIŞLIKLA farklı depremleri birleştirmediğini doğrular.
 */
class PhysicalEarthquakeMatcherTest {

    private final PhysicalEarthquakeMatcher matcher = new PhysicalEarthquakeMatcher();

    private EarthquakeEvent event(EarthquakeSource source, OffsetDateTime time, double lat, double lon, Double magnitude) {
        return EarthquakeEvent.builder()
                .source(source).eventTime(time).latitude(lat).longitude(lon)
                .magnitude(magnitude != null ? magnitude : 0.0)
                .riskLevel(EarthquakeRiskLevel.LOW)
                .externalId("x").build();
    }

    @Test
    void afadAndKandilli_sameEvent_slightlyDifferentMagnitudeAndTime_isRecognizedAsSame() {
        OffsetDateTime t = OffsetDateTime.parse("2026-08-08T14:00:00Z");
        EarthquakeEvent afad = event(EarthquakeSource.AFAD, t, 38.1440, 36.9340, 4.2);
        EarthquakeEvent kandilli = event(EarthquakeSource.KANDILLI, t.plusSeconds(40), 38.1520, 36.9280, 4.1);

        assertThat(matcher.isSamePhysicalEvent(afad, kandilli)).isTrue();
    }

    @Test
    void differentLocation_farApart_isNotSameEvent_evenIfTimeAndMagnitudeMatch() {
        OffsetDateTime t = OffsetDateTime.parse("2026-08-08T14:00:00Z");
        // İstanbul ve Ankara civarı — aynı anda, benzer büyüklükte ama coğrafi olarak çok uzak.
        EarthquakeEvent istanbul = event(EarthquakeSource.AFAD, t, 41.0082, 28.9784, 4.0);
        EarthquakeEvent ankara = event(EarthquakeSource.KANDILLI, t.plusSeconds(10), 39.9334, 32.8597, 4.0);

        assertThat(matcher.isSamePhysicalEvent(istanbul, ankara)).isFalse();
    }

    @Test
    void differentTime_farApart_isNotSameEvent_evenIfLocationAndMagnitudeMatch() {
        OffsetDateTime t = OffsetDateTime.parse("2026-08-08T14:00:00Z");
        EarthquakeEvent first = event(EarthquakeSource.AFAD, t, 38.1440, 36.9340, 4.2);
        EarthquakeEvent muchLater = event(EarthquakeSource.KANDILLI, t.plusHours(3), 38.1440, 36.9340, 4.2);

        assertThat(matcher.isSamePhysicalEvent(first, muchLater)).isFalse();
    }

    @Test
    void magnitudeTooDifferent_isNotSameEvent() {
        OffsetDateTime t = OffsetDateTime.parse("2026-08-08T14:00:00Z");
        EarthquakeEvent small = event(EarthquakeSource.AFAD, t, 38.1440, 36.9340, 2.0);
        EarthquakeEvent large = event(EarthquakeSource.KANDILLI, t.plusSeconds(20), 38.1440, 36.9340, 4.5);

        assertThat(matcher.isSamePhysicalEvent(small, large)).isFalse();
    }

    @Test
    void boundaryTimeWindow_exactlyAtTolerance_isStillConsideredSame() {
        OffsetDateTime t = OffsetDateTime.parse("2026-08-08T14:00:00Z");
        EarthquakeEvent a = event(EarthquakeSource.AFAD, t, 38.1440, 36.9340, 4.0);
        EarthquakeEvent b = event(EarthquakeSource.KANDILLI, t.plus(PhysicalEarthquakeMatcher.TIME_TOLERANCE), 38.1440, 36.9340, 4.0);

        assertThat(matcher.isSamePhysicalEvent(a, b)).isTrue();
    }

    @Test
    void haversineKm_knownDistance_istanbulToAnkara_isApproximatelyCorrect() {
        // İstanbul-Ankara kuş uçuşu mesafesi yaklaşık 350 km'dir.
        double km = PhysicalEarthquakeMatcher.haversineKm(41.0082, 28.9784, 39.9334, 32.8597);
        assertThat(km).isBetween(340.0, 360.0);
    }
}
