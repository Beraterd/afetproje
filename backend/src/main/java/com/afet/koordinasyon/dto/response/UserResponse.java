package com.afet.koordinasyon.dto.response;

import com.afet.koordinasyon.domain.enums.BloodType;
import com.afet.koordinasyon.domain.enums.UserRole;
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
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private BloodType bloodType;
    private UserRole role;
    private boolean emailVerified;
    private boolean active;
    private DistrictSummaryResponse district;
    private NeighborhoodSummaryResponse neighborhood;
    private UUID districtId;
    private UUID neighborhoodId;
    private String address;
    private String profession;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
