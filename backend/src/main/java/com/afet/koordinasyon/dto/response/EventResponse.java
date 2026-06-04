package com.afet.koordinasyon.dto.response;

import com.afet.koordinasyon.domain.enums.EventStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class EventResponse {
    private UUID id;
    private String title;
    private String description;
    private EventStatus status;
    private int requiredPeople;
    private BigDecimal riskScore;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private NeighborhoodSummaryResponse neighborhood;
    private TeamSummaryResponse team;
    private UserSummaryResponse createdBy;
    private int assignedVolunteers;
    private int acceptedCount;
    @JsonProperty("isParticipating")
    private boolean isParticipating;
    // currentUserJoined = isParticipating (alias, "is" prefix olmadan serializasyon sorunsuz)
    private boolean currentUserJoined;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String eventTeamCode;
}
