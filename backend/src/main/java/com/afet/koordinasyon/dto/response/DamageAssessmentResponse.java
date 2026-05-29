package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class DamageAssessmentResponse {
    private UUID id;
    private UUID districtId;
    private String districtName;
    private UUID neighborhoodId;
    private String neighborhoodName;
    private String streetName;
    private String buildingNo;
    private String address;
    private Double latitude;
    private Double longitude;
    private String locationSource;
    private boolean locationVerified;
    private String buildingType;
    private Integer floorCount;
    private String occupancyType;
    private String damageLevel;
    private String damageLevelLabel;
    private boolean collapseRisk;
    private boolean emergencyEvacuationNeeded;
    private boolean casualtiesSuspected;
    private boolean blockedRoad;
    private boolean gasLeakRisk;
    private String note;
    private String verificationStatus;
    private String verificationStatusLabel;
    private String reportedBy;
    private String verifiedBy;
    private OffsetDateTime verifiedAt;
    private String approvedBy;
    private OffsetDateTime approvedAt;
    private java.util.List<String> photoUrls;
    private java.util.List<String> reporterPhotoUrls;
    private java.util.List<String> fieldPhotoUrls;
    private java.util.List<DamageAssessmentAssignmentResponse> assignments;
    private String aiComment;
    private String aiConfidence;
    private String aiConfidenceLabel;
    private String aiModel;
    private OffsetDateTime aiAnalyzedAt;
    private String aiAnalysisStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
