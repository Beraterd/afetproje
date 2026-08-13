package com.afet.koordinasyon.earthquake;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AFAD ve Kandilli aynı gerçek/fiziksel depremi ayrı kayıtlar olarak yayınlayabilir.
 * Bu sınıf İKİ KAYDI TEK KAYDA BİRLEŞTİRMEZ (item 4'te açıkça izin verilen konservatif
 * yaklaşım — kayıtlar ayrı tutulur, UI'da kaynak badge'i ile gösterilir). Yalnızca
 * bildirim spam'ini önlemek için (item 17) "bu muhtemelen az önce başka bir kaynaktan
 * bildirilen depremle aynı fiziksel olay mı?" sorusuna konservatif bir cevap üretir.
 * <p>
 * Toleranslar deliberately dar tutulmuştur — YANLIŞLIKLA İKİ FARKLI DEPREMİ AYNI SAYMAK,
 * aynı depremi iki kez bildirmekten daha kötü bir kullanıcı deneyimidir:
 * <ul>
 *   <li>Zaman farkı ≤ 2 dakika — AFAD ve Kandilli otomatik çözümleri genellikle saniyeler
 *       içinde yayınlanır, aynı olay için birkaç dakikadan uzun sapma beklenmez.</li>
 *   <li>Mesafe ≤ 50 km — iki kurumun episantr hesaplama farkı tipik olarak birkaç-onlarca
 *       km'dir; 50 km, farklı algoritma/istasyon ağı farkını tolere eder ama komşu illerdeki
 *       gerçekten farklı depremleri birleştirmeyecek kadar dardır.</li>
 *   <li>Büyüklük farkı ≤ 1.0 — ML/Mw/MD gibi farklı ölçeklerin aynı olay için tipik sapması
 *       bu aralıktadır (AFAD 4.2 / Kandilli 4.1 gibi örnekler normaldir).</li>
 * </ul>
 * Üçü BİRDEN sağlanmalıdır (AND) — tek bir kriter yakınlığı yeterli değildir.
 */
@Component
public class PhysicalEarthquakeMatcher {

    public static final Duration TIME_TOLERANCE = Duration.ofMinutes(2);
    private static final double DISTANCE_TOLERANCE_KM = 50.0;
    private static final double MAGNITUDE_TOLERANCE = 1.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    public boolean isSamePhysicalEvent(EarthquakeEvent a, EarthquakeEvent b) {
        if (a == null || b == null || a.getEventTime() == null || b.getEventTime() == null) {
            return false;
        }
        Duration timeDiff = Duration.between(a.getEventTime(), b.getEventTime()).abs();
        if (timeDiff.compareTo(TIME_TOLERANCE) > 0) {
            return false;
        }
        if (a.getLatitude() == null || a.getLongitude() == null
                || b.getLatitude() == null || b.getLongitude() == null) {
            return false;
        }
        double distanceKm = haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        if (distanceKm > DISTANCE_TOLERANCE_KM) {
            return false;
        }
        if (a.getMagnitude() != null && b.getMagnitude() != null) {
            if (Math.abs(a.getMagnitude() - b.getMagnitude()) > MAGNITUDE_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
