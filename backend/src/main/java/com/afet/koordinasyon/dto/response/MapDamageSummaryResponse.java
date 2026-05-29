package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MapDamageSummaryResponse {
    private UUID neighborhoodId;
    private int totalCount;
    private int lightCount;
    private int moderateCount;
    private int heavyCount;
    private int collapsedCount;
}
