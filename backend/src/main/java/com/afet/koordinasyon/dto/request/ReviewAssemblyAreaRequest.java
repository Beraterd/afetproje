package com.afet.koordinasyon.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewAssemblyAreaRequest {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String googleMapsUrl;
    private String address;
    private String name;
    private Boolean needsReview;
    private Boolean active;
}
