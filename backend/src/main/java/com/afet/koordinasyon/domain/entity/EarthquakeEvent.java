package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.EarthquakeRiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "earthquake_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EarthquakeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column
    private Double depth;

    @Column(nullable = false)
    private Double magnitude;

    @Column(columnDefinition = "TEXT")
    private String location;

    @Column
    private String province;

    @Column
    private String district;

    @Column(nullable = false)
    @Builder.Default
    private String source = "AFAD";

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private EarthquakeRiskLevel riskLevel;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    // AFAD API raw response pozisyonu (0-based). Her sync'te güncellenir; NULL = eski kayıt.
    @Column(name = "afad_order_index")
    private Integer afadOrderIndex;

    @Column(name = "notification_sent", nullable = false)
    @Builder.Default
    private boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private OffsetDateTime notificationSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
