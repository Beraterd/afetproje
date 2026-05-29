package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.DamageAssessmentPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface DamageAssessmentPhotoRepository extends JpaRepository<DamageAssessmentPhoto, UUID> {
    Optional<DamageAssessmentPhoto> findByDownloadToken(UUID downloadToken);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DamageAssessmentPhoto p")
    int deleteAllPhotos();
}
