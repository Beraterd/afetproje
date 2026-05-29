package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.AiAnalysisStatus;
import com.afet.koordinasyon.domain.enums.AiConfidence;
import com.afet.koordinasyon.domain.enums.DamageLevel;
import com.afet.koordinasyon.domain.enums.LocationSource;
import com.afet.koordinasyon.domain.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "damage_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamageAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id", nullable = false)
    private Neighborhood neighborhood;

    @Column(name = "street_name", length = 255)
    private String streetName;

    @Column(name = "building_no", length = 50)
    private String buildingNo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "building_type", length = 100)
    private String buildingType;

    @Column(name = "floor_count")
    private Integer floorCount;

    @Column(name = "occupancy_type", length = 100)
    private String occupancyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_level", nullable = false, length = 30)
    @Builder.Default
    private DamageLevel damageLevel = DamageLevel.UNASSESSED;

    @Column(name = "collapse_risk", nullable = false)
    @Builder.Default
    private boolean collapseRisk = false;

    @Column(name = "emergency_evacuation_needed", nullable = false)
    @Builder.Default
    private boolean emergencyEvacuationNeeded = false;

    @Column(name = "casualties_suspected", nullable = false)
    @Builder.Default
    private boolean casualtiesSuspected = false;

    @Column(name = "blocked_road", nullable = false)
    @Builder.Default
    private boolean blockedRoad = false;

    @Column(name = "gas_leak_risk", nullable = false)
    @Builder.Default
    private boolean gasLeakRisk = false;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_source", nullable = false, length = 50)
    @Builder.Default
    private LocationSource locationSource = LocationSource.NEIGHBORHOOD_CENTER;

    @Column(name = "location_verified", nullable = false)
    @Builder.Default
    private boolean locationVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.INCELEME_GEREKIYOR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @OneToMany(mappedBy = "damageAssessment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DamageAssessmentPhoto> photos = new ArrayList<>();

    @Column(name = "ai_comment", columnDefinition = "TEXT")
    private String aiComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_confidence", length = 20)
    private AiConfidence aiConfidence;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "ai_analyzed_at")
    private OffsetDateTime aiAnalyzedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_analysis_status", length = 30)
    private AiAnalysisStatus aiAnalysisStatus;

    @Column(name = "client_generated_id", length = 36, unique = true)
    private String clientGeneratedId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
