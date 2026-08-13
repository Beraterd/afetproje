package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BuildingSearchResultResponse {
    private UUID id;
    private String displayAddress;
    private String streetName;
    private String buildingNumber;
    private String neighborhoodName;
    private double latitude;
    private double longitude;
}
