package com.afet.koordinasyon.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Kısa link kaydı. WhatsApp/SMS gibi kanallarda uzun URL yerine
 * /s/{code} yönlendirmesi için kullanılır.
 * <p>
 * Public redirect endpoint yalnızca targetUrl'e yönlendirir; kişisel veri
 * göstermez. userId/earthquakeEventId sadece dahili izleme amaçlıdır.
 */
@Entity
@Table(name = "short_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 16, unique = true)
    private String code;

    @Column(name = "target_url", nullable = false, length = 1000)
    private String targetUrl;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "earthquake_event_id")
    private UUID earthquakeEventId;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
