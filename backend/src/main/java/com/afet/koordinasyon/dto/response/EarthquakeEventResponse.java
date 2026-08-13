package com.afet.koordinasyon.dto.response;

import com.afet.koordinasyon.domain.enums.EarthquakeRiskLevel;
import com.afet.koordinasyon.domain.enums.EarthquakeSource;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class EarthquakeEventResponse {
    private UUID id;
    private String externalId;
    private OffsetDateTime eventTime;
    private Double latitude;
    private Double longitude;
    private Double depth;
    private Double magnitude;
    /** ML/Mw/MD vb. — sağlayıcı vermiyorsa null. */
    private String magnitudeType;
    private String location;
    private String province;
    private String district;
    private EarthquakeSource source;
    private EarthquakeRiskLevel riskLevel;
    private OffsetDateTime createdAt;
}
