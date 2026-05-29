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
public class EventSummaryResponse {
    private UUID id;
    private String title;
    private EventStatus status;
    private int requiredPeople;
    private BigDecimal riskScore;
    private NeighborhoodSummaryResponse neighborhood;
    private TeamSummaryResponse team;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private OffsetDateTime startsAt;
    private OffsetDateTime createdAt;
}
