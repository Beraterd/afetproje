package com.afet.koordinasyon.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "neighborhoods", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "district_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Neighborhood {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_id")
    private User coordinator;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geojson_polygon", columnDefinition = "jsonb")
    private String geojsonPolygon;

    @Column(name = "risk_score", precision = 10, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal riskScore = BigDecimal.ZERO;

    @Column(name = "risk_score_updated_at")
    private OffsetDateTime riskScoreUpdatedAt;

    @Column(name = "coordinator_latitude", precision = 10, scale = 7)
    private BigDecimal coordinatorLatitude;

    @Column(name = "coordinator_longitude", precision = 10, scale = 7)
    private BigDecimal coordinatorLongitude;

    @Column(name = "coordinator_location_updated_at")
    private OffsetDateTime coordinatorLocationUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_location_updated_by")
    private User coordinatorLocationUpdatedBy;

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
