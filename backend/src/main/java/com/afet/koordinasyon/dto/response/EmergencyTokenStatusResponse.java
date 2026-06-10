package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Value
@Builder
public class EmergencyTokenStatusResponse {
    boolean valid;
    String errorCode;
    String senderDisplayName;
    BigDecimal lastKnownLatitude;
    BigDecimal lastKnownLongitude;
    BigDecimal lastKnownLocationAccuracy;
    OffsetDateTime lastKnownLocationUpdatedAt;
}
