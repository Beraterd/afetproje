package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Belirli bir kullanıcıya özel bildirim. Null ise rol/bölge bazlı. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private User recipientUser;

    /** Belirli bir role (ADMIN, DISTRICT_COORDINATOR, NEIGHBORHOOD_COORDINATOR) broadcast. */
    @Column(name = "target_role", length = 50)
    private String targetRole;

    /** İlçe bazlı bildirim — sadece o ilçenin koordinatörü görür. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    /** Mahalle bazlı bildirim — sadece o mahallenin koordinatörü görür. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id")
    private Neighborhood neighborhood;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** İlgili entity tipi (EarthquakeEvent, Document, vb.). */
    @Column(name = "related_entity_type", length = 100)
    private String relatedEntityType;

    /** İlgili entity'nin ID'si (UUID veya externalId string). */
    @Column(name = "related_entity_id", length = 100)
    private String relatedEntityId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
