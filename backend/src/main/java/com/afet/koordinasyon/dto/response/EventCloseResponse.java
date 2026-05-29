package com.afet.koordinasyon.dto.response;

import com.afet.koordinasyon.domain.enums.EventStatus;
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
public class EventCloseResponse {
    private UUID id;
    private EventStatus status;
    private OffsetDateTime closedAt;
    private BigDecimal riskScore;
}
