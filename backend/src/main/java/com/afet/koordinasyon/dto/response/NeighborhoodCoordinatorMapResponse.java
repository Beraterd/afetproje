package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class NeighborhoodCoordinatorMapResponse {
    private UUID neighborhoodId;
    private String neighborhoodName;
    private UUID districtId;
    private String districtName;
    private Double latitude;
    private Double longitude;
    private String address;
    private Boolean locationVerified;
    private long openEventCount;
    private long openResourceRequestCount;
    private long totalDamageCount;
    private OffsetDateTime centerUpdatedAt;
}
