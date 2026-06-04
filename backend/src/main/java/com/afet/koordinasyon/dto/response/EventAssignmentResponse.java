package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class EventAssignmentResponse {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private OffsetDateTime invitedAt;
    private OffsetDateTime acceptedAt;
    private OffsetDateTime declinedAt;
    private boolean mailSent;
}
