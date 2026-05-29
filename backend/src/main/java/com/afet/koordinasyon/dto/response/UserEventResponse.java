package com.afet.koordinasyon.dto.response;

import com.afet.koordinasyon.domain.enums.EventStatus;
import com.afet.koordinasyon.domain.enums.EventVolunteerStatus;
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
public class UserEventResponse {
    private UUID id;
    private String title;
    private EventStatus status;
    private TeamSummaryResponse team;
    private NeighborhoodSummaryResponse neighborhood;
    private OffsetDateTime joinedAt;
    private EventVolunteerStatus volunteerStatus;
}
