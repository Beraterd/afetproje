package com.afet.koordinasyon.dto.response;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinatorDistrictResponse {
    private UUID districtId;
    private String districtName;
    private UUID coordinatorId;
    private String coordinatorFirstName;
    private String coordinatorLastName;
    private String coordinatorEmail;
}
