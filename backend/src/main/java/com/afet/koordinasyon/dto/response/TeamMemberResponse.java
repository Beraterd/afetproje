package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class TeamMemberResponse {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private OffsetDateTime joinedAt;
}
