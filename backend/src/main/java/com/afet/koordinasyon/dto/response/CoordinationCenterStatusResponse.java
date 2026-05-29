package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CoordinationCenterStatusResponse {

    private String userRole;

    /** Set when user is DISTRICT_COORDINATOR */
    private UUID assignedDistrictId;
    private String assignedDistrictName;

    /** Set when user is NEIGHBORHOOD_COORDINATOR */
    private UUID assignedNeighborhoodId;
    private String assignedNeighborhoodName;

    /**
     * The district that contains the assigned neighborhood (NC only).
     * Allows the frontend to display the district name and load the polygon.
     */
    private UUID assignedNeighborhoodDistrictId;
    private String assignedNeighborhoodDistrictName;

    private boolean districtCenterDefined;
    private boolean neighborhoodCenterDefined;

    /** True if the user has an assignment but no center is defined yet */
    private boolean mustSetCenter;
}
