package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.DamageAssessment;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.DamageLevel;
import com.afet.koordinasyon.domain.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DamageAssessmentRepository
        extends JpaRepository<DamageAssessment, UUID>, JpaSpecificationExecutor<DamageAssessment> {

    Optional<DamageAssessment> findByClientGeneratedId(String clientGeneratedId);

    Page<DamageAssessment> findByReportedByIdOrderByCreatedAtDesc(UUID reportedById, Pageable pageable);

    Page<DamageAssessment> findByDistrictId(UUID districtId, Pageable pageable);

    Page<DamageAssessment> findByNeighborhoodId(UUID neighborhoodId, Pageable pageable);

    @Query("""
        SELECT d.neighborhood.id,
               COUNT(d),
               SUM(CASE WHEN d.damageLevel = 'LIGHT'     THEN 1 ELSE 0 END),
               SUM(CASE WHEN d.damageLevel = 'MODERATE'  THEN 1 ELSE 0 END),
               SUM(CASE WHEN d.damageLevel = 'HEAVY'     THEN 1 ELSE 0 END),
               SUM(CASE WHEN d.damageLevel = 'COLLAPSED' THEN 1 ELSE 0 END)
        FROM DamageAssessment d
        GROUP BY d.neighborhood.id
        """)
    List<Object[]> findNeighborhoodDamageSummary();

    List<DamageAssessment> findByLatitudeIsNotNullAndLongitudeIsNotNull();

    @Query("SELECT d FROM DamageAssessment d WHERE d.district.id = :districtId AND d.latitude IS NOT NULL AND d.longitude IS NOT NULL")
    List<DamageAssessment> findByDistrictIdWithCoordinates(@Param("districtId") UUID districtId);

    @Query("SELECT d FROM DamageAssessment d WHERE d.neighborhood.id = :neighborhoodId AND d.latitude IS NOT NULL AND d.longitude IS NOT NULL")
    List<DamageAssessment> findByNeighborhoodIdWithCoordinates(@Param("neighborhoodId") UUID neighborhoodId);

    List<DamageAssessment> findByNeighborhoodIdAndVerificationStatus(UUID neighborhoodId, VerificationStatus status);

    List<DamageAssessment> findByDistrictIdAndVerificationStatus(UUID districtId, VerificationStatus status);

    long countByNeighborhoodIdAndDamageLevel(UUID neighborhoodId, DamageLevel level);

    long countByDistrictId(UUID districtId);

    long countByNeighborhoodId(UUID neighborhoodId);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DamageAssessment d SET d.reportedBy = :admin WHERE d.reportedBy.id IN :userIds")
    int reassignReportedBy(@Param("admin") User admin, @Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DamageAssessment d SET d.verifiedBy = NULL WHERE d.verifiedBy IS NOT NULL AND d.verifiedBy.id IN :userIds")
    int clearVerifiedByIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DamageAssessment d SET d.approvedBy = NULL WHERE d.approvedBy IS NOT NULL AND d.approvedBy.id IN :userIds")
    int clearApprovedByIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DamageAssessment d")
    int deleteAllAssessments();
}
