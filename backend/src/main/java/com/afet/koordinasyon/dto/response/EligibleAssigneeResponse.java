package com.afet.koordinasyon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Returned by GET /api/damage-assessments/{id}/eligible-assignees.
 * Represents a volunteer currently active in a HASAR_TESPIT_EKIBI event
 * within the same district as the damage assessment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleAssigneeResponse {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UUID eventId;
    private String eventTitle;
}
