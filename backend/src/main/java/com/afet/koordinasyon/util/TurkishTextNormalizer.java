package com.afet.koordinasyon.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Bina/adres arama eşleştirmesi için tek yönlü, kayıp-kabul eden normalizasyon — YALNIZCA arama
 * indeksleme/karşılaştırma amaçlıdır, hiçbir zaman kullanıcıya gösterilen metne (displayAddress,
 * streetName vb.) uygulanmaz.
 *
 * Yapılanlar:
 *   1) Türkçe harf katlama: İ/I/ı/i → i, Ş/ş → s, Ç/ç → c, Ğ/ğ → g, Ö/ö → o, Ü/ü → u
 *      (locale-duyarlı String.toLowerCase() KULLANILMAZ — Türkçe "I".toLowerCase(tr) → "ı" olur,
 *      bu da "I" ve "i" varyantlarının aynı arama token'ına düşmesini bilerek engeller).
 *   1b) Eski yazım/şapka varyantları: â/Â → a, î/Î → i, û/Û → u (bkz. Kâğıthane/Kağıthane OSM
 *      isim uyuşmazlığı — aynı sınıf problem sokak isimlerinde de görülebilir, ör. "Sâbit Sokak"
 *      vs "Sabit Sokak"). Bu katlama olmadan â/î/û NON_ALNUM tarafından kelime sınırı sayılıp
 *      kelimeyi ikiye bölerdi (ör. "sâbit" → "s" + "bit").
 *   2) Harf/rakam olmayan her karakter boşluğa çevrilir (noktalama, "No:12" gibi ayraçlar).
 *   3) Yaygın sokak/cadde/mahalle kısaltmaları ayrı kelime olarak tamamen kaldırılır — böylece
 *      "Selvi Boylum Sokak", "Selvi Boylum Sk" ve "Selvi Boylum" aynı normalize metne düşer.
 */
public final class TurkishTextNormalizer {

    private static final Set<String> ADDRESS_STOPWORDS = Set.of(
            "sokak", "sok", "sk",
            "cadde", "cad", "cd",
            "bulvari", "bulvar", "blv",
            "mahallesi", "mahalle", "mah", "mh",
            "no"
    );

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private TurkishTextNormalizer() {}

    public static String normalize(String input) {
        if (input == null) return "";

        StringBuilder folded = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case 'İ', 'I', 'ı', 'i' -> folded.append('i');
                case 'Ş', 'ş' -> folded.append('s');
                case 'Ç', 'ç' -> folded.append('c');
                case 'Ğ', 'ğ' -> folded.append('g');
                case 'Ö', 'ö' -> folded.append('o');
                case 'Ü', 'ü' -> folded.append('u');
                case 'Â', 'â' -> folded.append('a');
                case 'Î', 'î' -> folded.append('i');
                case 'Û', 'û' -> folded.append('u');
                default -> folded.append(c);
            }
        }

        String lower = folded.toString().toLowerCase(Locale.ROOT);
        String spaced = NON_ALNUM.matcher(lower).replaceAll(" ");

        StringBuilder result = new StringBuilder(spaced.length());
        for (String token : spaced.split(" ")) {
            if (token.isEmpty() || ADDRESS_STOPWORDS.contains(token)) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(token);
        }

        return result.toString();
    }
}
