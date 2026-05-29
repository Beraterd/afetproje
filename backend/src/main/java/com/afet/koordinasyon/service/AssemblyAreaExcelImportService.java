package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.AssemblyArea;
import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.repository.AssemblyAreaRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Excel dosyasından toplanma alanı verisi okur ve assembly_areas tablosuna import eder.
 *
 * Beklenen Excel kolon sırası:
 *   0: İlçe          (örn. "Kadıköy", "Pendik")
 *   1: Mahalle        (örn. "Bostancı", "Kurtköy Mh.")
 *   2: Toplanma Alanı (ad, örn. "Bostancı Sahil Parkı")
 *   3: Alan_m2        (sayısal, isteğe bağlı — capacity alanına yazılır)
 *   4: Google Maps URL (örn. "https://maps.google.com/?q=40.96,29.08")
 *
 * Kullanım:
 *   POST /api/admin/assembly-areas/import-excel
 *   Content-Type: multipart/form-data
 *   file: &lt;xlsx dosyası&gt;
 *
 * Import her çalıştırmada güvenlidir (idempotent): aynı mahallede aynı isimli alan varsa atlanır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssemblyAreaExcelImportService {

    private static final String SOURCE_NAME = "İstanbul Toplanma Alanları Master (Excel)";

    // Google Maps URL'inden lat/lng çıkarmak için iki yaygın format:
    // https://maps.google.com/?q=lat,lng
    // https://www.google.com/maps/place/.../@lat,lng,zoom...
    private static final Pattern QUERY_PATTERN   = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern AT_PATTERN      = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");

    private final DistrictRepository      districtRepository;
    private final NeighborhoodRepository  neighborhoodRepository;
    private final AssemblyAreaRepository  assemblyAreaRepository;

    public record ImportResult(
            int imported,
            int skippedDuplicate,
            int skippedNoNeighborhood,
            List<String> unmatchedNeighborhoods
    ) {}

    /**
     * Verilen InputStream'den Excel'i okur ve assembly_areas tablosuna import eder.
     * InputStream kapatma sorumluluğu çağırana aittir (controller'da try-with-resources yapılır).
     */
    @Transactional
    public ImportResult importFromExcel(InputStream inputStream) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            // İlçe adlarını normalize(name) → District olarak yükle
            Map<String, District> districtMap = districtRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            d -> normalize(d.getName()),
                            d -> d,
                            (a, b) -> a
                    ));

            // District-bazlı mahalle adı → Neighborhood lookup (smartMatch için).
            // Her mahalle hem normalize hem normalizeRaw key ile eklenir:
            //   "Yenimahalle" → normalize = "yeni", normalizeRaw = "yenimahalle"
            // Excel "Yenimahalle Mh." → normalizeRaw = "yenimahalle" → eşleşir.
            Map<String, Map<String, Neighborhood>> nhByDistrict = new java.util.HashMap<>();
            for (Neighborhood n : neighborhoodRepository.findAllWithDistrict()) {
                String dNorm = normalize(n.getDistrict().getName());
                nhByDistrict.computeIfAbsent(dNorm, k -> new java.util.HashMap<>())
                        .putIfAbsent(normalize(n.getName()), n);
                nhByDistrict.get(dNorm).putIfAbsent(normalizeRaw(n.getName()), n);
            }

            int imported             = 0;
            int skippedDuplicate     = 0;
            int skippedNoNeighborhood = 0;
            Set<String> unmatchedSet = new LinkedHashSet<>();

            // Başlık satırını atla (satır 0)
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                String districtName     = cellStr(row, 0);
                String neighborhoodName = cellStr(row, 1);
                String areaName         = cellStr(row, 2);
                String areaM2Str        = cellStr(row, 3);
                String googleMapsUrl    = cellStr(row, 4);

                if (districtName.isBlank() || neighborhoodName.isBlank() || areaName.isBlank()) continue;

                String districtKey = normalize(districtName);

                District district = districtMap.get(districtKey);
                if (district == null) {
                    log.warn("Row {}: İlçe bulunamadı: '{}'", rowIdx + 1, districtName);
                    unmatchedSet.add("İlçe: " + districtName);
                    skippedNoNeighborhood++;
                    continue;
                }

                // Smart matching: önce standart normalize, sonra raw normalize dener
                Map<String, Neighborhood> nhMap = nhByDistrict.getOrDefault(districtKey, Map.of());
                Neighborhood neighborhood = smartMatch(neighborhoodName, nhMap);
                if (neighborhood == null) {
                    log.warn("Row {}: Mahalle bulunamadı: '{}/{}'", rowIdx + 1, districtName, neighborhoodName);
                    unmatchedSet.add(districtName + " / " + neighborhoodName);
                    skippedNoNeighborhood++;
                    continue;
                }

                // Duplicate kontrol: aynı mahallede aynı ad zaten varsa atla
                List<AssemblyArea> existing = assemblyAreaRepository.findByNeighborhoodId(neighborhood.getId());
                boolean duplicate = existing.stream()
                        .anyMatch(a -> normalize(a.getName()).equals(normalize(areaName)));
                if (duplicate) {
                    skippedDuplicate++;
                    continue;
                }

                // lat/lng çıkar
                BigDecimal latitude  = null;
                BigDecimal longitude = null;
                if (googleMapsUrl != null && !googleMapsUrl.isBlank()) {
                    double[] latLng = extractLatLng(googleMapsUrl);
                    if (latLng != null) {
                        latitude  = BigDecimal.valueOf(latLng[0]);
                        longitude = BigDecimal.valueOf(latLng[1]);
                    }
                }

                Integer areaM2 = null;
                try {
                    if (!areaM2Str.isBlank()) areaM2 = (int) Double.parseDouble(areaM2Str);
                } catch (NumberFormatException ignored) {}

                AssemblyArea area = AssemblyArea.builder()
                        .district(district)
                        .neighborhood(neighborhood)
                        .name(areaName.trim())
                        .latitude(latitude)
                        .longitude(longitude)
                        .googleMapsUrl(googleMapsUrl.isBlank() ? null : googleMapsUrl.trim())
                        .capacity(areaM2)           // Alan_m2 → capacity alanına
                        .sourceName(SOURCE_NAME)
                        .needsReview(googleMapsUrl == null || googleMapsUrl.isBlank()) // URL yoksa inceleme gerekli
                        .isActive(true)
                        .build();

                assemblyAreaRepository.save(area);
                imported++;

                if (imported % 100 == 0) {
                    log.info("Excel import: {} kayıt eklendi...", imported);
                }
            }

            log.info("Excel import tamamlandı — eklendi: {}, duplicate atlandı: {}, mahalle bulunamadı: {}",
                    imported, skippedDuplicate, skippedNoNeighborhood);

            return new ImportResult(imported, skippedDuplicate, skippedNoNeighborhood,
                    new ArrayList<>(unmatchedSet));
        }
    }

    // ── Yardımcı metodlar ──────────────────────────────────────────────────────

    private String cellStr(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    /**
     * İsmi küçük harfe çevirir, Türkçe karakter normalizes eder,
     * mahalle suffix'lerini (Mh., Mah., Mahallesi) temizler.
     *
     * Excel → DB eşleşme stratejisi (duplicate kaçınmak için):
     *   1. normalize(excel) == normalize(db)  — standart yol
     *   2. normalizeRaw(excel) == normalizeRaw(db)  — suffix stripping olmadan
     *      "Yenimahalle" DB'de suffix stripping'ten etkilenir ("yeni" olur), ama
     *      raw versiyonunda "yenimahalle" olur ve Excel "Yenimahalle Mh." raw'ı da
     *      "yenimahalle" olduğundan eşleşir.
     */
    private String normalize(String s) {
        String raw = normalizeRaw(s);
        for (String suffix : new String[]{"mahallesi", "mahalle", "mah", "mh"}) {
            if (raw.endsWith(suffix) && raw.length() > suffix.length()) {
                return raw.substring(0, raw.length() - suffix.length());
            }
        }
        return raw;
    }

    /** Suffix stripping olmadan normalize — bileşik sözcük eşleşmesi için. */
    private String normalizeRaw(String s) {
        if (s == null) return "";
        return s.toLowerCase(new Locale("tr"))
                .replace("ı", "i")
                .replace("İ", "i")
                .replace("ğ", "g")
                .replace("Ğ", "g")
                .replace("ü", "u")
                .replace("Ü", "u")
                .replace("ş", "s")
                .replace("Ş", "s")
                .replace("ö", "o")
                .replace("Ö", "o")
                .replace("ç", "c")
                .replace("Ç", "c")
                .replaceAll("[^a-z0-9]", "");
    }

    /**
     * DB mahalle haritasında eşleşme arar.
     * Önce suffix-stripped, sonra raw normalize ile dener.
     * Bu, "Yenimahalle Mh." (Excel) ↔ "Yenimahalle" (DB) gibi durumları çözer.
     */
    private Neighborhood smartMatch(String excelNh, Map<String, Neighborhood> dbNhMap) {
        Neighborhood found = dbNhMap.get(normalize(excelNh));
        if (found != null) return found;
        found = dbNhMap.get(normalizeRaw(excelNh));
        if (found != null) return found;
        // DB adını raw ile karşılaştır (compound word düzeltme)
        String rawKey = normalizeRaw(excelNh);
        for (Neighborhood nh : dbNhMap.values()) {
            if (rawKey.equals(normalizeRaw(nh.getName()))) {
                return nh;
            }
        }
        return null;
    }

    /** Google Maps URL'inden [lat, lng] çıkarır. Null döner eğer çıkaramazsa. */
    private double[] extractLatLng(String url) {
        Matcher m = QUERY_PATTERN.matcher(url);
        if (m.find()) {
            try {
                return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))};
            } catch (NumberFormatException ignored) {}
        }
        m = AT_PATTERN.matcher(url);
        if (m.find()) {
            try {
                return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))};
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
