package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.EventInvitationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id")
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "event_invitation_status")
    @Builder.Default
    private EventInvitationStatus status = EventInvitationStatus.INVITED;

    @Column(name = "invitation_token", unique = true, nullable = false, length = 128)
    private String invitationToken;

    @Column(name = "token_expires_at", nullable = false)
    private OffsetDateTime tokenExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    private TeamRecommendation recommendation;

    @Column(name = "invited_at", nullable = false)
    private OffsetDateTime invitedAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "declined_at")
    private OffsetDateTime declinedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (invitedAt == null) invitedAt = OffsetDateTime.now();
    }
}
