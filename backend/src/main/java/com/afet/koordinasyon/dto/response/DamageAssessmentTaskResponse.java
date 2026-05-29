package com.afet.koordinasyon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamageAssessmentTaskResponse {
    private UUID assignmentId;
    private UUID damageAssessmentId;
    private String address;
    private String districtName;
    private String neighborhoodName;
    private String damageLevel;
    private String damageLevelLabel;
    private Double latitude;
    private Double longitude;
    private String note;
    private boolean collapseRisk;
    private boolean emergencyEvacuationNeeded;
    private boolean casualtiesSuspected;
    private boolean gasLeakRisk;
    private String assignmentStatus;
    private String assignmentStatusLabel;
    private String assignedByName;
    private OffsetDateTime assignedAt;
    private boolean active;
    private List<String> reporterPhotoUrls;
    private List<String> fieldPhotoUrls;
    private String aiComment;
    private String aiConfidence;
    private String aiConfidenceLabel;
    private String aiAnalysisStatus;
}
