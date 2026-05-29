package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class DamagePointResponse {
    private UUID id;
    private double latitude;
    private double longitude;
    private String damageLevel;
    private String damageLevelLabel;
    private String address;
    private String verificationStatus;
    private String verificationStatusLabel;
    private String note;
    private UUID districtId;
    private String districtName;
    private UUID neighborhoodId;
    private String neighborhoodName;
    private boolean gasLeakRisk;
    private boolean collapseRisk;
    private boolean emergencyEvacuationNeeded;
    private boolean casualtiesSuspected;
    private OffsetDateTime createdAt;
}
