package com.afet.koordinasyon.client;

import com.afet.koordinasyon.client.dto.KandilliEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Boğaziçi Üniversitesi Kandilli Rasathanesi ve Deprem Araştırma Enstitüsü'nün resmi
 * "Son Depremler" sayfasını (koeri.boun.edu.tr/scripts/lst0.asp) parse eder.
 * <p>
 * Bu sayfa resmi bir JSON/XML API DEĞİLDİR — sabit genişlikli, windows-1254 kodlamalı,
 * &lt;pre&gt; bloğu içinde düz metin bir tablo döner (son 500 deprem). Doğrulama: sayfa
 * gerçekten erişilebilir ve bu formatı döndürüyor (2026-08 itibarıyla manuel olarak
 * curl ile teyit edildi) — URL/format tahmin edilmedi.
 * <p>
 * AFAD entegrasyonundan tamamen bağımsızdır: kendi RestTemplate'i, kendi timeout'u,
 * kendi hata izolasyonu vardır. Bu client hiçbir zaman exception fırlatmaz — hata
 * durumunda boş liste döner (AfadEarthquakeClient ile aynı sözleşme).
 */
@Component
@Slf4j
public class KandilliEarthquakeClient {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final Charset KANDILLI_CHARSET = Charset.forName("windows-1254");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Pattern PRE_BLOCK = Pattern.compile("<pre>(.*?)</pre>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // Tarih  Saat  Enlem  Boylam  Derinlik  MD  ML  Mw  Yer(+opsiyonel kalite sütunu)
    private static final Pattern ROW = Pattern.compile(
            "^(\\d{4}\\.\\d{2}\\.\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})\\s+" +
            "(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s+" +
            "(-\\.-|\\d+\\.\\d+)\\s+(-\\.-|\\d+\\.\\d+)\\s+(-\\.-|\\d+\\.\\d+)\\s+" +
            "(\\S.+)$");

    /** En az iki boşluk = kolon ayırıcı; "Yer" metninin kendi iç boşluklarını korumak için kullanılır. */
    private static final Pattern COLUMN_GAP = Pattern.compile("\\s{2,}");

    /** Görüntülenecek konum metninin makul uzunlukta ve kontrol karakterlerinden arınmış olmasını sağlar. */
    private static final int MAX_LOCATION_LENGTH = 300;

    private final RestTemplate restTemplate;
    private final String url;

    public KandilliEarthquakeClient(
            RestTemplateBuilder builder,
            @Value("${app.kandilli.base-url:http://www.koeri.boun.edu.tr/scripts/lst0.asp}") String url) {
        this.restTemplate = builder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(20))
                .build();
        this.url = url;
    }

    public List<KandilliEventDto> fetchRecentEvents(int hoursBefore) {
        String raw = fetchRawPage();
        if (raw == null) {
            return Collections.emptyList();
        }

        List<KandilliEventDto> all = parse(raw);
        if (all.isEmpty()) {
            return all;
        }

        ZonedDateTime cutoff = ZonedDateTime.now(ISTANBUL).minusHours(hoursBefore);
        List<KandilliEventDto> filtered = all.stream()
                .filter(e -> e.date() != null && e.time() != null
                        && LocalDateTime.of(e.date(), e.time()).atZone(ISTANBUL).isAfter(cutoff))
                .toList();

        log.info("Kandilli fetch tamamlandı: parse edilen={}, {} saat penceresinde={}",
                all.size(), hoursBefore, filtered.size());
        return filtered;
    }

    private String fetchRawPage() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (AfetKoordinasyon earthquake sync)");
            // Sayfa HTTP üzerinde yayınlanıyor (resmi kaynak) — SSL doğrulaması devre dışı
            // BIRAKILMAZ, bu client hiçbir yerde SSL doğrulamasını bypass etmez.
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                log.warn("Kandilli: boş yanıt");
                return null;
            }
            return new String(body, KANDILLI_CHARSET);
        } catch (Exception e) {
            // Response body'sini production'da loglamıyoruz — yalnızca hata mesajı.
            log.error("Kandilli fetch hatası: {}", e.getMessage());
            return null;
        }
    }

    /** Package-private: fixture-based parser testleri gerçek ağa bağlanmadan bunu doğrudan çağırır. */
    List<KandilliEventDto> parse(String rawHtml) {
        Matcher preMatcher = PRE_BLOCK.matcher(rawHtml);
        if (!preMatcher.find()) {
            log.error("Kandilli: <pre> bloğu bulunamadı, sayfa formatı değişmiş olabilir");
            return Collections.emptyList();
        }

        String[] lines = preMatcher.group(1).split("\\r?\\n");
        List<KandilliEventDto> events = new ArrayList<>();
        int skipped = 0;

        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            KandilliEventDto dto = parseRow(trimmed);
            if (dto == null) {
                skipped++;
                continue;
            }
            events.add(dto);
        }

        log.info("Kandilli parse: {} satır -> {} geçerli kayıt, {} atlandı (başlık/ayraç/bozuk)",
                lines.length, events.size(), skipped);
        return events;
    }

    /** Tek bir satırı parse eder. Format uymuyorsa (başlık/ayraç/bozuk kayıt) null döner — sync'i düşürmez. */
    private KandilliEventDto parseRow(String line) {
        Matcher m = ROW.matcher(line);
        if (!m.matches()) {
            return null; // başlık satırı, "----" ayraç satırı veya beklenmeyen format — sessizce atla
        }

        try {
            LocalDate date = LocalDate.parse(m.group(1), DATE_FMT);
            LocalTime time = LocalTime.parse(m.group(2), TIME_FMT);
            double lat = Double.parseDouble(m.group(3));
            double lon = Double.parseDouble(m.group(4));
            double depth = Double.parseDouble(m.group(5));
            Double md = parseMagnitudeOrNull(m.group(6));
            Double ml = parseMagnitudeOrNull(m.group(7));
            Double mw = parseMagnitudeOrNull(m.group(8));

            if (md == null && ml == null && mw == null) {
                log.warn("Kandilli satırı atlandı (büyüklük bilgisi yok): {}", truncate(line));
                return null;
            }

            String rest = m.group(9);
            String location = sanitizeLocation(firstColumn(rest));

            return new KandilliEventDto(date, time, lat, lon, depth, md, ml, mw, location);
        } catch (DateTimeParseException | NumberFormatException e) {
            log.warn("Kandilli satırı parse edilemedi ({}): {}", e.getMessage(), truncate(line));
            return null;
        }
    }

    private Double parseMagnitudeOrNull(String raw) {
        if (raw == null || "-.-".equals(raw)) {
            return null;
        }
        return Double.parseDouble(raw);
    }

    /** "Yer" ve opsiyonel "Çözüm Niteliği" kolonu 2+ boşlukla ayrılır — ilk kolon "Yer"dir. */
    private String firstColumn(String rest) {
        String[] parts = COLUMN_GAP.split(rest.strip(), 2);
        return parts.length > 0 ? parts[0].strip() : rest.strip();
    }

    /** Güvenilmeyen dış kaynak metnini görüntüye vermeden önce temizler (kontrol karakteri/uzunluk). */
    private String sanitizeLocation(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[\\r\\n\\p{Cntrl}]", "").strip();
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > MAX_LOCATION_LENGTH ? cleaned.substring(0, MAX_LOCATION_LENGTH) : cleaned;
    }

    private String truncate(String s) {
        return s.length() > 150 ? s.substring(0, 150) + "..." : s;
    }
}
