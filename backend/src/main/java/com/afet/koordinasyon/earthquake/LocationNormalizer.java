package com.afet.koordinasyon.earthquake;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * İl/ilçe adlarını eşleştirme amaçlı normalize eder (item 14). Türkçe karakter/case
 * farkları (İstanbul / ISTANBUL / istanbul) filtrelemeyi bozmasın diye kullanılır —
 * DISPLAY değeri asla normalize edilmiş haliyle kullanıcıya gösterilmez, yalnızca DB'de
 * ayrı bir "*_normalized" kolonunda arama/eşleştirme anahtarı olarak tutulur.
 * <p>
 * String.toUpperCase() KASITLI OLARAK kullanılmaz — Türkçe locale'de 'i' harfinin büyük
 * hali 'İ' olur (Locale-bağımlı "dotless I" hatası), bu yüzden Türkçe özel karakterler
 * önce elle ASCII eşdeğerine çevrilir, sonra Locale.ROOT ile büyütülür.
 */
@Component
public final class LocationNormalizer {

    private final Map<String, String> canonicalByNormalizedKey = new LinkedHashMap<>();

    public LocationNormalizer() {
        for (String province : TurkishProvinces.ALL) {
            canonicalByNormalizedKey.put(normalize(province), province);
        }
    }

    /** Eşleştirme/arama anahtarı üretir — display değeri DEĞİLDİR. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String folded = trimmed
                .replace('İ', 'I').replace('ı', 'I').replace('i', 'I')
                .replace('Ş', 'S').replace('ş', 'S')
                .replace('Ç', 'C').replace('ç', 'C')
                .replace('Ğ', 'G').replace('ğ', 'G')
                .replace('Ö', 'O').replace('ö', 'O')
                .replace('Ü', 'U').replace('ü', 'U');
        return folded.toUpperCase(Locale.ROOT);
    }

    /**
     * Ham il adını (AFAD'ın verdiği temiz Türkçe ad veya Kandilli'nin ASCII/büyük harf
     * metni) 81 illik kanonik listeyle eşleştirir. Eşleşmezse null döner — yanlış il
     * uydurulmaz (item 5/14 gereksinimi).
     */
    public String canonicalizeProvince(String rawProvince) {
        String key = normalize(rawProvince);
        if (key == null) {
            return null;
        }
        return canonicalByNormalizedKey.get(key);
    }
}
