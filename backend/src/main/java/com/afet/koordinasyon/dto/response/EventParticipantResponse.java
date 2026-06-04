package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class EventParticipantResponse {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String source;   // DIRECT_JOIN | AI_INVITATION
    private String status;   // JOINED | INVITED | ACCEPTED | DECLINED
    private OffsetDateTime joinedAt;
}
