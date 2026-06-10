package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.EmergencyMessageType;
import com.afet.koordinasyon.domain.enums.EmergencyMessageTokenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * §6/§11 — Deprem e-postasındaki "Yakınlarıma Mesaj Gönder" linkleri için güvenli token.
 *
 * <p>Ham token e-postada gider; veritabanında YALNIZCA SHA-256 hash'i tutulur (düz metin
 * loglanmaz/saklanmaz). Token süreli (24 saat) ve tek kullanımlıktır; kullanıcıya bağlıdır,
 * yani link yalnızca o kullanıcının yakınlarına mesaj göndermeye yetki verir.
 */
@Entity
@Table(name = "emergency_message_tokens",
        indexes = {
                @Index(name = "idx_emt_token_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_emt_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyMessageToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Ham token'ın SHA-256 hash'i (hex). Düz token asla saklanmaz. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Bağlı deprem/simülasyon kaydının id'si (izleme amaçlı; null olabilir). */
    @Column(name = "earthquake_notification_id")
    private UUID earthquakeNotificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EmergencyMessageTokenStatus status = EmergencyMessageTokenStatus.ACTIVE;

    /** Token tüketildiğinde seçilen mesaj tipi (audit/iz için). */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 40)
    private EmergencyMessageType messageType;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Konum bilgisi (token kullanıldığında kaydedilir)
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "location_accuracy", precision = 8, scale = 2)
    private BigDecimal locationAccuracy;

    /** CURRENT veya LAST_KNOWN */
    @Column(name = "location_source", length = 30)
    private String locationSource;

    @Column(name = "maps_url", columnDefinition = "TEXT")
    private String mapsUrl;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
