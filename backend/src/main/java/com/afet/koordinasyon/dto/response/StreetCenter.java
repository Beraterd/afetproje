package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StreetCenter {
    private double lon;
    private double lat;
}
