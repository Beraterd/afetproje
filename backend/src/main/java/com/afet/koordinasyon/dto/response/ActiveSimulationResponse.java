package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ActiveSimulationResponse {
    private UUID id;
    private String districtName;
    private BigDecimal magnitude;
    private OffsetDateTime triggeredAt;
}
