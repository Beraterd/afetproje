package com.afet.koordinasyon.dto.request;

import com.afet.koordinasyon.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignRoleRequest {
    @NotNull
    private UserRole role;
    private UUID districtId; // required for DISTRICT_COORDINATOR
    private UUID neighborhoodId; // required for NEIGHBORHOOD_COORDINATOR
}
