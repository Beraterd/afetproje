package com.afet.koordinasyon.earthquake;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * İl/ilçe bilgisini üretmenin TEK merkezi yeri (AFAD ve Kandilli ortak kullanır — item
 * "province/district normalization merkezi helper olsun").
 * <p>
 * İki farklı güven seviyesi vardır:
 * <ul>
 *   <li>{@link #fromStructuredFields}: sağlayıcı zaten yapılandırılmış province/district
 *       alanı veriyorsa (AFAD) — güvenilir, serbest metin parse gerekmez.</li>
 *   <li>{@link #fromFreeText}: sağlayıcı yalnızca serbest metin "Yer" bilgisi veriyorsa
 *       (Kandilli) — kontrollü, konservatif parse. Belirsiz durumda province/district
 *       null bırakılır, YANLIŞ İLÇE UYDURULMAZ.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EarthquakeLocationResolver {

    private final LocationNormalizer locationNormalizer;

    /** "PLACE-DISTRICT (PROVINCE)" veya "PLACE (PROVINCE)" — parantez yoksa hiç eşleşmez. */
    private static final Pattern LOCATION_WITH_PROVINCE = Pattern.compile("^(.*?)-?\\s*\\(([^)]+)\\)\\s*$");

    public record Resolution(String province, String provinceNormalized, String district, String districtNormalized) {
        static final Resolution EMPTY = new Resolution(null, null, null, null);
    }

    /** AFAD gibi yapılandırılmış alan veren sağlayıcılar için — whitelist'e karşı yalnızca kanonik yazıma çevirir. */
    public Resolution fromStructuredFields(String rawProvince, String rawDistrict) {
        String canonicalProvince = locationNormalizer.canonicalizeProvince(rawProvince);
        if (canonicalProvince == null) {
            if (rawProvince != null && !rawProvince.isBlank()) {
                log.debug("Sağlayıcı province değeri 81 il listesiyle eşleşmedi, null bırakıldı: '{}'", rawProvince);
            }
            return Resolution.EMPTY;
        }
        String district = (rawDistrict != null && !rawDistrict.isBlank()) ? rawDistrict.trim() : null;
        return new Resolution(
                canonicalProvince,
                LocationNormalizer.normalize(canonicalProvince),
                district,
                LocationNormalizer.normalize(district));
    }

    /**
     * Kandilli gibi yalnızca serbest metin veren sağlayıcılar için. "Yer" metni yalnızca
     * gerçekten 81 il listesiyle eşleşen bir parantez içeriği taşıyorsa il set edilir;
     * aksi halde (yurt dışı, deniz adı, parantez yok) province=district=null kalır.
     * <p>
     * İlçe adı Kandilli'nin kendi ASCII/büyük harf metninden best-effort türetilir —
     * Türkçe aksan işaretleri (ş/ç/ğ/ö/ü/ı) İCAT EDİLMEZ, yalnızca basit title-case
     * uygulanır (örn. "KUSADASI" -> "Kusadasi", gerçek yazımı "Kuşadası" olsa da).
     */
    public Resolution fromFreeText(String rawLocation) {
        if (rawLocation == null || rawLocation.isBlank()) {
            return Resolution.EMPTY;
        }
        Matcher m = LOCATION_WITH_PROVINCE.matcher(rawLocation.trim());
        if (!m.matches()) {
            return Resolution.EMPTY;
        }

        String canonicalProvince = locationNormalizer.canonicalizeProvince(m.group(2).trim());
        if (canonicalProvince == null) {
            return Resolution.EMPTY;
        }

        String district = extractDistrict(m.group(1) == null ? "" : m.group(1).trim());

        return new Resolution(
                canonicalProvince,
                LocationNormalizer.normalize(canonicalProvince),
                district,
                district != null ? LocationNormalizer.normalize(district) : null);
    }

    private String extractDistrict(String beforeParens) {
        if (beforeParens.isEmpty()) {
            return null;
        }
        int dashIdx = beforeParens.lastIndexOf('-');
        if (dashIdx < 0 || dashIdx == beforeParens.length() - 1) {
            // Ya hiç "-" yok (tek kelimelik yer adı, ilçe belirtilmemiş) ya da "-(İL)" gibi
            // boş segment — ilçe belirsiz, uydurulmaz.
            return null;
        }
        String candidate = beforeParens.substring(dashIdx + 1).trim();
        return candidate.isEmpty() ? null : titleCaseAscii(candidate);
    }

    private String titleCaseAscii(String word) {
        String[] parts = word.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }
}
