package com.afet.koordinasyon.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapNeighborhoodResponse {
    private UUID id;
    private String name;
    private BigDecimal riskScore;
    private String riskColor;
    private String riskLevel;
    private long openEventCount;
    private long openResourceRequestCount;
    private long damageCount;
    private JsonNode polygon;
}
