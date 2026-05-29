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
public class SimulationCreatedResponse {
    private UUID simulationId;
    private UUID districtId;
    private String districtName;
    private BigDecimal magnitude;
    private String emailStatus;
    private int totalUsersToNotify;
    private OffsetDateTime triggeredAt;
}
