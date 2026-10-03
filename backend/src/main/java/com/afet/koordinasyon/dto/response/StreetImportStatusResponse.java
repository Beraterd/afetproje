package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class StreetImportStatusResponse {
    private UUID districtId;
    private String districtName;
    private long streetCount;
    private long neighborhoodCount;
    private OffsetDateTime lastImportAt;
    private String status;
}
