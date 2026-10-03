package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StreetBounds {
    private double minLon;
    private double minLat;
    private double maxLon;
    private double maxLat;
}
