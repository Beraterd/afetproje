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
public class SimulationDetailResponse {
    private UUID id;
    private DistrictSummaryResponse district;
    private BigDecimal magnitude;
    private String notes;
    private String emailStatus;
    private int totalQueued;
    private int totalSent;
    private int totalFailed;
    private OffsetDateTime triggeredAt;
    private UserSummaryResponse createdBy;
}
