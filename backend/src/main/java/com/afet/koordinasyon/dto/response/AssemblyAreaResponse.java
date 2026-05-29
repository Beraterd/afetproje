package com.afet.koordinasyon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssemblyAreaResponse {
    private UUID id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String googleMapsUrl;
    private Integer capacity;
    private String description;
    private String sourceName;
    private String sourceReference;
    private boolean needsReview;
    private boolean active;
    private OffsetDateTime createdAt;
    private DistrictSummaryResponse district;
    private NeighborhoodSummaryResponse neighborhood;
}
