package com.afet.koordinasyon.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Geographic utility for point-in-polygon validation using ray casting algorithm.
 * GeoJSON coordinates are stored as [longitude, latitude] pairs.
 */
@Slf4j
public final class GeoUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GeoUtils() {}

    /**
     * Returns true if the point (lat, lng) is inside the given GeoJSON polygon string.
     * If geojsonPolygon is null/blank, returns true (no restriction).
     */
    public static boolean isPointInPolygon(double lat, double lng, String geojsonPolygon) {
        if (geojsonPolygon == null || geojsonPolygon.isBlank()) {
            return true; // no polygon = no restriction
        }
        try {
            JsonNode node = MAPPER.readTree(geojsonPolygon);
            String type = node.path("type").asText();
            JsonNode coordinates = node.path("coordinates");

            return switch (type) {
                case "Polygon" -> isPointInRings(lat, lng, coordinates);
                case "MultiPolygon" -> {
                    for (JsonNode poly : coordinates) {
                        if (isPointInRings(lat, lng, poly)) yield true;
                    }
                    yield false;
                }
                default -> {
                    log.warn("Unknown GeoJSON type for polygon validation: {}", type);
                    yield true;
                }
            };
        } catch (Exception e) {
            log.warn("Failed to parse GeoJSON for polygon validation: {}", e.getMessage());
            return true; // allow on parse error
        }
    }

    /**
     * Check if point is in any of the polygon rings (uses first/outer ring only).
     * coordinates is [[ring], [hole], ...] for a Polygon.
     */
    private static boolean isPointInRings(double lat, double lng, JsonNode rings) {
        if (rings == null || !rings.isArray() || rings.isEmpty()) return false;
        JsonNode outerRing = rings.get(0);
        return isPointInRing(lat, lng, outerRing);
    }

    /**
     * Ray casting algorithm.
     * ring is [[lng,lat], [lng,lat], ...]
     */
    private static boolean isPointInRing(double lat, double lng, JsonNode ring) {
        if (ring == null || !ring.isArray()) return false;
        boolean inside = false;
        int n = ring.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            // GeoJSON: [longitude, latitude]
            double xi = ring.get(i).get(0).asDouble(); // lng of vertex i
            double yi = ring.get(i).get(1).asDouble(); // lat of vertex i
            double xj = ring.get(j).get(0).asDouble(); // lng of vertex j
            double yj = ring.get(j).get(1).asDouble(); // lat of vertex j

            // Ray from (lng, lat) going right; check edge crossing
            if (((yi > lat) != (yj > lat)) &&
                    (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }
}
