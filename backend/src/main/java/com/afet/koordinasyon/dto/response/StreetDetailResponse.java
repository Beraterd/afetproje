package com.afet.koordinasyon.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** GET /api/streets/{id} — seçim sonrası harita highlight'ı için tam geometri (LineString/
 *  MultiLineString) döner, autocomplete listesinde YER ALMAZ (bkz. StreetSearchResultResponse). */
@Data
@Builder
public class StreetDetailResponse {
    private UUID id;
    private String name;
    private UUID districtId;
    private String districtName;
    private UUID neighborhoodId;
    private String neighborhoodName;
    private String roadType;
    private JsonNode geometry;
    private StreetBounds bounds;
    private StreetCenter center;
}
