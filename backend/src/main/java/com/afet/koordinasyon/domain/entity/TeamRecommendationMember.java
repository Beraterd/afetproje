package com.afet.koordinasyon.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "team_recommendation_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRecommendationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private TeamRecommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int score;

    @Column(name = "order_id", nullable = false)
    private int orderId;

    @Column(name = "completed_similar_tasks", nullable = false)
    private int completedSimilarTasks;

    @Column(columnDefinition = "TEXT")
    private String reasons;

    @Column(name = "proximity_level", length = 30)
    private String proximityLevel;

    @Column(name = "availability", length = 20)
    private String availability;

    @Column(name = "previous_similar_task", columnDefinition = "TEXT")
    private String previousSimilarTask;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
