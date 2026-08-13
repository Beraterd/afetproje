package com.afet.koordinasyon.client.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Kandilli Rasathanesi'nin (koeri.boun.edu.tr/scripts/lst0.asp) sabit genişlikli metin
 * satırından parse edilen tek bir deprem kaydı. JSON'a bağımlı değildir — parser
 * ({@link com.afet.koordinasyon.client.KandilliEarthquakeClient}) satırı elle ayrıştırıp
 * bu DTO'yu üretir.
 *
 * @param magnitudeMD  Süre büyüklüğü (Duration magnitude) — yoksa null, uydurulmaz.
 * @param magnitudeML  Yerel büyüklük (Local magnitude) — yoksa null.
 * @param magnitudeMw  Moment büyüklüğü — yoksa null.
 * @param rawLocation  "Yer" kolonunun ham metni (örn. "SOGUCAK-AFSIN (KAHRAMANMARAS)").
 */
public record KandilliEventDto(
        LocalDate date,
        LocalTime time,
        Double latitude,
        Double longitude,
        Double depth,
        Double magnitudeMD,
        Double magnitudeML,
        Double magnitudeMw,
        String rawLocation
) {

    /**
     * Kandilli üç farklı büyüklük türü yayınlar; hangisinin "başlık" değeri olarak
     * gösterileceğine dair resmi tek bir kural yok. Türkiye'de haber/uygulamalarda en
     * yaygın kabul gören sıralama: ML (yerel büyüklük, küçük/orta depremler için en sık
     * raporlanan) → Mw (büyük depremler için hesaplanır) → MD (en az tercih edilen, veri
     * yetersizse hesaplanan kaba tahmin). Üçü de yoksa null döner — kayıt bu durumda atlanır.
     */
    public Double resolvedMagnitude() {
        if (magnitudeML != null) return magnitudeML;
        if (magnitudeMw != null) return magnitudeMw;
        return magnitudeMD;
    }

    public String resolvedMagnitudeType() {
        if (magnitudeML != null) return "ML";
        if (magnitudeMw != null) return "Mw";
        if (magnitudeMD != null) return "MD";
        return null;
    }
}
