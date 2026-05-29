package com.afet.koordinasyon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationLogResponse {
    private UUID id;
    private UUID userId;
    private String emailAddress;
    private String status;
    private short retryCount;
    private String lastError;
    private OffsetDateTime sentAt;
}
