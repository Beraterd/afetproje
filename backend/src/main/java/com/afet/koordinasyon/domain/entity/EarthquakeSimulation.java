package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.SimulationEmailStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "earthquake_simulations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EarthquakeSimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal magnitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "triggered_at", nullable = false)
    private OffsetDateTime triggeredAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "email_status", nullable = false, columnDefinition = "simulation_email_status")
    @Builder.Default
    private SimulationEmailStatus emailStatus = SimulationEmailStatus.QUEUED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        triggeredAt = OffsetDateTime.now();
    }
}
