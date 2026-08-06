package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.BloodType;
import com.afet.koordinasyon.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "blood_type", columnDefinition = "blood_type")
    private BloodType bloodType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id", nullable = false)
    private Neighborhood neighborhood;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(length = 255)
    private String profession;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "user_role")
    @Builder.Default
    private UserRole role = UserRole.VOLUNTEER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Salt okunur "Admin Demo Modu" hesabı mı? bkz. DemoAdminSeeder / DemoModeWriteGuardFilter. */
    @Column(name = "is_demo", nullable = false)
    @Builder.Default
    private boolean demo = false;

    // ── Email bildirim tercihleri ─────────────────────────────────────────────
    @Column(name = "email_task_notifications_enabled", nullable = false)
    @Builder.Default private boolean emailTaskNotificationsEnabled = true;

    @Column(name = "email_damage_notifications_enabled", nullable = false)
    @Builder.Default private boolean emailDamageNotificationsEnabled = true;

    @Column(name = "email_earthquake_notifications_enabled", nullable = false)
    @Builder.Default private boolean emailEarthquakeNotificationsEnabled = true;

    @Column(name = "email_team_notifications_enabled", nullable = false)
    @Builder.Default private boolean emailTeamNotificationsEnabled = true;

    @Column(name = "email_aid_notifications_enabled", nullable = false)
    @Builder.Default private boolean emailAidNotificationsEnabled = true;

    @Column(name = "email_system_notifications_enabled", nullable = false)
    @Builder.Default private boolean emailSystemNotificationsEnabled = true;

    // ── Konum bilgisi ─────────────────────────────────────────────────────────
    @Column(name = "last_known_latitude", precision = 10, scale = 8)
    private BigDecimal lastKnownLatitude;

    @Column(name = "last_known_longitude", precision = 11, scale = 8)
    private BigDecimal lastKnownLongitude;

    @Column(name = "last_known_location_accuracy", precision = 8, scale = 2)
    private BigDecimal lastKnownLocationAccuracy;

    /** GRANTED, DENIED veya SKIPPED */
    @Column(name = "location_permission_status", length = 10)
    private String locationPermissionStatus;

    @Column(name = "last_known_location_updated_at")
    private OffsetDateTime lastKnownLocationUpdatedAt;

    /** GPS, Network veya Cell — frontend tarafından accuracy'den türetilir */
    @Column(name = "location_source", length = 30)
    private String locationSource;

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
