package com.afet.koordinasyon.dto.response;

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
public class EventJoinResponse {
    private UUID eventVolunteerId;
    private UUID eventId;
    private EventVolunteerStatus status;
    private OffsetDateTime joinedAt;
}
