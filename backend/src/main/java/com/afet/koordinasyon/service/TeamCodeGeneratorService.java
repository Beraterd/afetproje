package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Event;
import com.afet.koordinasyon.domain.enums.TeamName;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ekip kodu üretici.
 *
 * Format: [DIST_PREFIX]-[TYPE_CODE]-[SEQ]
 *
 * Örnekler:
 *   Pendik  / COMMUNICATION → PEN-ILE-1
 *   Beşiktaş / EVACUATION   → BES-TAH-1
 *   Beyoğlu / EVACUATION    → BEYO-TAH-1  (Beylikdüzü ile çakışma)
 *   Global  / SEARCH_RESCUE → IST-AKU-1
 *
 * District prefix: min 3 harf, çakışma varsa 4 harf (39 İstanbul ilçesi üzerinden)
 * Type code      : sabit ASCII mapping (3 harf)
 * Seq            : [DIST-TYPE] kombinasyonuna göre max+1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamCodeGeneratorService {

    private static final String GLOBAL_PREFIX = "IST";

    private final DistrictRepository districtRepository;
    private final TeamRepository teamRepository;

    private volatile Map<String, String> prefixCache;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates the next available team code for a district + team type.
     * Thread-safe: synchronized on this instance.
     */
    public synchronized String generateTeamCode(District district, TeamName teamType) {
        String districtPrefix = district != null
                ? resolveDistrictPrefix(district.getName())
                : GLOBAL_PREFIX;
        String typeCode = teamTypeCode(teamType);
        String combined = districtPrefix + "-" + typeCode;
        int seq = teamRepository.findMaxTeamNumberByCombinedPrefix(combined) + 1;
        String code = combined + "-" + seq;
        log.debug("[TEAM_CODE] Generated {} for district={} type={}", code,
                district != null ? district.getName() : "global", teamType);
        return code;
    }

    /**
     * Generates team code using just district name (for backfill scenarios).
     * Requires the caller to supply the TeamName.
     */
    public synchronized String generateTeamCode(String districtName, TeamName teamType) {
        String districtPrefix = districtName != null ? resolveDistrictPrefix(districtName) : GLOBAL_PREFIX;
        String typeCode = teamTypeCode(teamType);
        String combined = districtPrefix + "-" + typeCode;
        int seq = teamRepository.findMaxTeamNumberByCombinedPrefix(combined) + 1;
        return combined + "-" + seq;
    }

    /**
     * Generates a display code for an event based on the event's district, not the team's district.
     * Uses the team's existing sequence number to avoid reassignment drift.
     * Falls back to IST only when the event has no neighborhood/district.
     */
    public String generateDisplayCodeForEvent(Event event) {
        if (event == null || event.getTeam() == null) return null;
        String districtName = null;
        if (event.getNeighborhood() != null && event.getNeighborhood().getDistrict() != null) {
            districtName = event.getNeighborhood().getDistrict().getName();
        }
        String districtPrefix = districtName != null ? resolveDistrictPrefix(districtName) : GLOBAL_PREFIX;
        String typeCode = teamTypeCode(event.getTeam().getName());
        int seq = parseSequenceFromTeamCode(event.getTeam().getTeamCode());
        if (seq < 1) seq = 1;
        return districtPrefix + "-" + typeCode + "-" + seq;
    }

    /**
     * Legacy compatibility — generates district-only code (deprecated, prefer two-arg version).
     */
    public synchronized String generateNextTeamCode(String districtName) {
        return resolveDistrictPrefix(districtName) + "-1";
    }

    // ── District prefix ───────────────────────────────────────────────────────

    public String generateDistrictPrefix(String districtName) {
        return resolveDistrictPrefix(districtName);
    }

    private String resolveDistrictPrefix(String districtName) {
        if (prefixCache == null) rebuildPrefixCache();
        String key = normalizeKey(districtName);
        return prefixCache.getOrDefault(key, fallbackPrefix(districtName));
    }

    private synchronized void rebuildPrefixCache() {
        if (prefixCache != null) return;
        List<String> allNames = districtRepository.findAll().stream()
                .map(District::getName)
                .toList();
        Map<String, String> cache = new ConcurrentHashMap<>();
        for (String name : allNames) {
            cache.put(normalizeKey(name), resolveWithCollisionCheck(name, allNames));
        }
        prefixCache = cache;
        log.info("[TEAM_CODE] District prefix cache built for {} districts", cache.size());
        cache.forEach((k, v) -> log.debug("[TEAM_CODE]   {} → {}", k, v));
    }

    /**
     * Tries 3-char prefix first; if collision with any other district, tries 4-char.
     */
    private String resolveWithCollisionCheck(String districtName, List<String> allNames) {
        String norm = normalizeAscii(districtName);
        if (norm.length() < 3) return norm.isEmpty() ? GLOBAL_PREFIX : norm;

        String prefix3 = norm.substring(0, 3);
        boolean collision3 = allNames.stream()
                .filter(other -> !other.equalsIgnoreCase(districtName))
                .anyMatch(other -> normalizeAscii(other).startsWith(prefix3)
                        && normalizeAscii(other).length() >= 3
                        && normalizeAscii(other).substring(0, 3).equals(prefix3));
        if (!collision3) return prefix3;

        // Collision → try 4 chars
        if (norm.length() >= 4) return norm.substring(0, 4);
        return norm; // 3-char normalized name already
    }

    private String fallbackPrefix(String districtName) {
        String norm = normalizeAscii(districtName);
        return norm.isEmpty() ? GLOBAL_PREFIX : norm.substring(0, Math.min(3, norm.length()));
    }

    // ── Team type code ────────────────────────────────────────────────────────

    public String generateTeamTypeCode(TeamName teamType) {
        return teamTypeCode(teamType);
    }

    private String teamTypeCode(TeamName teamType) {
        if (teamType == null) return "DIG";
        return switch (teamType) {
            case SEARCH_RESCUE      -> "AKU";
            case FOOD_WATER         -> "YEM";
            case EVACUATION         -> "TAH";
            case COMMUNICATION      -> "ILE";
            case PSYCHOSOCIAL       -> "PSK";
            case HASAR_TESPIT_EKIBI -> "HAT";
            case LOGISTICS          -> "LOJ";
            case OTHER              -> "DIG";
        };
    }

    // ── Sequence parsing ──────────────────────────────────────────────────────

    /**
     * Parses the sequence number from a team code.
     * "PEN-TAH-3" → 3
     * Returns -1 if not parseable.
     */
    public int parseSequenceFromTeamCode(String teamCode) {
        if (teamCode == null || teamCode.isBlank()) return -1;
        int lastDash = teamCode.lastIndexOf('-');
        if (lastDash < 0 || lastDash == teamCode.length() - 1) return -1;
        try {
            return Integer.parseInt(teamCode.substring(lastDash + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ── Turkish normalization ─────────────────────────────────────────────────

    public String normalizeTurkish(String input) {
        return normalizeAscii(input);
    }

    private String normalizeAscii(String name) {
        if (name == null || name.isBlank()) return "";
        String upper = name.trim().toUpperCase();
        String replaced = upper
                .replace('Ç', 'C').replace('ç', 'C')
                .replace('Ğ', 'G').replace('ğ', 'G')
                .replace('İ', 'I').replace('i', 'I')
                .replace('I', 'I')
                .replace('Ö', 'O').replace('ö', 'O')
                .replace('Ş', 'S').replace('ş', 'S')
                .replace('Ü', 'U').replace('ü', 'U');
        return Normalizer.normalize(replaced, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^A-Z]", "");
    }

    private String normalizeKey(String name) {
        return name == null ? "" : name.trim().toUpperCase();
    }

    /** Invalidates the prefix cache (call when districts are added/renamed). */
    public synchronized void invalidatePrefixCache() {
        prefixCache = null;
    }
}
