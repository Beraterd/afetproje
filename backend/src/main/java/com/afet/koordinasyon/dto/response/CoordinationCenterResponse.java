package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CoordinationCenterResponse {

    private UUID id;

    /** For district centers */
    private UUID districtId;
    private String districtName;

    /** For neighborhood centers */
    private UUID neighborhoodId;
    private String neighborhoodName;

    private String address;
    private String streetName;
    private String buildingNo;

    private Double latitude;
    private Double longitude;

    private boolean locationVerified;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String updatedBy;
}
