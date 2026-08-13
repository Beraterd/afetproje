package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.EarthquakeRiskLevel;
import com.afet.koordinasyon.domain.enums.EarthquakeSource;
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

    /** Sağlayıcı içinde benzersiz kimlik — gerçek benzersizlik source+externalId birlikte sağlanır. */
    @Column(name = "external_id", nullable = false)
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

    /** Büyüklük türü (ML/Mw/MD vb.) — sağlayıcı vermiyorsa null, uydurulmaz. */
    @Column(name = "magnitude_type", length = 10)
    private String magnitudeType;

    @Column(columnDefinition = "TEXT")
    private String location;

    /** Görüntü değeri — sağlayıcıdan geldiği/parse edildiği şekliyle (kanonik yazımla normalize edilmiş). */
    @Column
    private String province;

    /** Yalnızca eşleştirme/filtreleme için — Türkçe karakter/case farklarından bağımsız. */
    @Column(name = "province_normalized")
    private String provinceNormalized;

    @Column
    private String district;

    @Column(name = "district_normalized")
    private String districtNormalized;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EarthquakeSource source = EarthquakeSource.AFAD;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private EarthquakeRiskLevel riskLevel;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    // AFAD API raw response pozisyonu (0-based). Her sync'te güncellenir; NULL = eski kayıt.
    // Yalnızca AFAD kayıtlarında kullanılır; Kandilli kayıtlarında her zaman NULL kalır.
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
