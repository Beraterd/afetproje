package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ResourceSummaryResponse {
    private UUID neighborhoodId;
    private UUID districtId;
    private int openCount;
}
