package com.afet.koordinasyon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictResponse {
    private UUID id;
    private String name;
    private BigDecimal riskScore;
    private String riskColor;
    private boolean active;
    private OffsetDateTime riskScoreUpdatedAt;
}
