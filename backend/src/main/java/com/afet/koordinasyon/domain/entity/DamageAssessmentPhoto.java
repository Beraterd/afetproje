package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.PhotoType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "damage_assessment_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamageAssessmentPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_assessment_id", nullable = false)
    private DamageAssessment damageAssessment;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "download_token", nullable = false, unique = true)
    private UUID downloadToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "photo_type", nullable = false, length = 30)
    @Builder.Default
    private PhotoType photoType = PhotoType.REPORTER_PHOTO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = OffsetDateTime.now();
        if (downloadToken == null) {
            downloadToken = UUID.randomUUID();
        }
    }
}
