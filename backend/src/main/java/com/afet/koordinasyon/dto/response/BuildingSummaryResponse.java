package com.afet.koordinasyon.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** GET /api/buildings FeatureCollection'ındaki her bina Feature'ının "properties" gövdesi. */
@Data
@Builder
public class BuildingSummaryResponse {
    private UUID id;
    private String source;
    private String buildingType;
    private String name;
    private String streetName;
    private String buildingNumber;
    private Double heightM;
    private Integer levels;
    private double estimatedHeightM;
    /** Lombok bu boolean alan için `isEstimatedHeight()` getter'ı üretir; Jackson varsayılan olarak
     *  "is" önekini JSON alan adından da düşürür ("estimatedHeight" üretir) — frontend'in beklediği
     *  "isEstimatedHeight" adını korumak için açıkça sabitlenir (canlı testte doğrulanan gerçek bug). */
    @JsonProperty("isEstimatedHeight")
    private boolean isEstimatedHeight;
    private String heightSource;
    /** Bu binaya bağlı hasar kaydı yoksa null (rapor yok ≠ UNASSESSED). */
    private String damageStatus;
    private String damageStatusLabel;
    private int damageAssessmentCount;
}
