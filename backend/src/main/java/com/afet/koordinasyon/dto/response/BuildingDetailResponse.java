package com.afet.koordinasyon.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BuildingDetailResponse {
    private UUID id;
    private String source;
    private UUID districtId;
    private String districtName;
    private UUID neighborhoodId;
    private String neighborhoodName;
    private String streetName;
    private String buildingNumber;
    private String displayAddress;
    private double latitude;
    private double longitude;
    private JsonNode geometry;
    private Double heightM;
    private Integer levels;
    private double estimatedHeightM;
    /** bkz. BuildingSummaryResponse'daki aynı not — Lombok/Jackson "is" önek çakışması. */
    @JsonProperty("isEstimatedHeight")
    private boolean isEstimatedHeight;
    private String heightSource;
    private String buildingType;
    private String name;

    private String damageStatus;
    private String damageStatusLabel;
    private int damageAssessmentCount;
    private int verifiedAssessmentCount;
    private BuildingLatestAssessmentSummary latestAssessment;
}
