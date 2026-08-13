package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Admin/maintenance seviyesinde ilçe başına bina import durumu (bkz. item 5) — yeni bir
 *  "import log" tablosu gerektirmez, gerçek DB envanterinden (buildings.updated_at) türetilir. */
@Data
@Builder
public class BuildingImportStatusResponse {
    private UUID districtId;
    private String districtName;
    private String source;
    private long buildingCount;
    private OffsetDateTime lastImportAt;
    /** HAS_DATA | NO_DATA */
    private String status;
}
