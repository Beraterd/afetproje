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

import java.time.OffsetDateTime;
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

    // ── Harita pinleri: SADECE sahada doğrulanıp koordinatör onaylanan + koordinatlı kayıtlar ──
    // KOORDINATOR_ONAYLADI, doğrulama akışının terminal durumudur (SAHADA_DOGRULANDI sonrası gelir),
    // dolayısıyla hem saha doğrulaması hem koordinatör onayı sağlanmış demektir.

    @Query("""
            SELECT d FROM DamageAssessment d
            WHERE d.verificationStatus = :status
              AND d.latitude IS NOT NULL AND d.longitude IS NOT NULL
            """)
    List<DamageAssessment> findApprovedWithCoordinates(@Param("status") VerificationStatus status);

    @Query("""
            SELECT d FROM DamageAssessment d
            WHERE d.district.id = :districtId AND d.verificationStatus = :status
              AND d.latitude IS NOT NULL AND d.longitude IS NOT NULL
            """)
    List<DamageAssessment> findApprovedByDistrictIdWithCoordinates(@Param("districtId") UUID districtId,
                                                                   @Param("status") VerificationStatus status);

    @Query("""
            SELECT d FROM DamageAssessment d
            WHERE d.neighborhood.id = :neighborhoodId AND d.verificationStatus = :status
              AND d.latitude IS NOT NULL AND d.longitude IS NOT NULL
            """)
    List<DamageAssessment> findApprovedByNeighborhoodIdWithCoordinates(@Param("neighborhoodId") UUID neighborhoodId,
                                                                       @Param("status") VerificationStatus status);

    List<DamageAssessment> findByNeighborhoodIdAndVerificationStatus(UUID neighborhoodId, VerificationStatus status);

    List<DamageAssessment> findByDistrictIdAndVerificationStatus(UUID districtId, VerificationStatus status);

    long countByNeighborhoodIdAndDamageLevel(UUID neighborhoodId, DamageLevel level);

    long countByDistrictId(UUID districtId);

    long countByNeighborhoodId(UUID neighborhoodId);

    // ── Operations AI context queries ────────────────────────────────────────

    @Query("""
        SELECT d.district.name, d.damageLevel, COUNT(d)
        FROM DamageAssessment d
        WHERE d.createdAt >= :since
        GROUP BY d.district.name, d.damageLevel
        ORDER BY COUNT(d) DESC
        """)
    List<Object[]> findDistrictDamageSummary(@Param("since") OffsetDateTime since);

    @Query("""
        SELECT d.neighborhood.name, d.damageLevel, COUNT(d)
        FROM DamageAssessment d
        WHERE d.district.id = :districtId AND d.createdAt >= :since
        GROUP BY d.neighborhood.name, d.damageLevel
        ORDER BY COUNT(d) DESC
        """)
    List<Object[]> findNeighborhoodDamageSummaryByDistrict(
            @Param("districtId") UUID districtId,
            @Param("since") OffsetDateTime since);

    @Query("""
        SELECT d.damageLevel, COUNT(d)
        FROM DamageAssessment d
        WHERE d.neighborhood.id = :neighborhoodId AND d.createdAt >= :since
        GROUP BY d.damageLevel
        ORDER BY COUNT(d) DESC
        """)
    List<Object[]> findDamageLevelSummaryByNeighborhood(
            @Param("neighborhoodId") UUID neighborhoodId,
            @Param("since") OffsetDateTime since);

    @Query("""
        SELECT
            SUM(CASE WHEN d.collapseRisk = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN d.gasLeakRisk = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN d.emergencyEvacuationNeeded = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN d.casualtiesSuspected = true THEN 1 ELSE 0 END)
        FROM DamageAssessment d
        WHERE d.createdAt >= :since
        """)
    Object[] findRiskFlagsSummary(@Param("since") OffsetDateTime since);

    @Query("""
        SELECT
            SUM(CASE WHEN d.collapseRisk = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN d.gasLeakRisk = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN d.emergencyEvacuationNeeded = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN d.casualtiesSuspected = true THEN 1 ELSE 0 END)
        FROM DamageAssessment d
        WHERE d.district.id = :districtId AND d.createdAt >= :since
        """)
    Object[] findRiskFlagsSummaryByDistrict(
            @Param("districtId") UUID districtId,
            @Param("since") OffsetDateTime since);

    @Query("""
        SELECT
            SUM(CASE WHEN d.collapseRisk = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN d.gasLeakRisk = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN d.emergencyEvacuationNeeded = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN d.casualtiesSuspected = true THEN 1 ELSE 0 END)
        FROM DamageAssessment d
        WHERE d.neighborhood.id = :neighborhoodId AND d.createdAt >= :since
        """)
    Object[] findRiskFlagsSummaryByNeighborhood(
            @Param("neighborhoodId") UUID neighborhoodId,
            @Param("since") OffsetDateTime since);

    // ── Rapor: trend / mahalle karşılaştırma ─────────────────────────────────

    /** Kapsamda belirli tarih aralığında bildirilen hasar sayısı (trend analizi). */
    @Query("""
            SELECT COUNT(d) FROM DamageAssessment d
            WHERE d.createdAt >= :from AND d.createdAt < :to
              AND (:districtId IS NULL OR d.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR d.neighborhood.id = :neighborhoodId)
            """)
    long reportCountCreatedBetween(@Param("districtId") UUID districtId,
                                   @Param("neighborhoodId") UUID neighborhoodId,
                                   @Param("from") OffsetDateTime from,
                                   @Param("to") OffsetDateTime to);

    /** Mahalle bazında toplam hasar sayısı (ilçe kapsamında karşılaştırma). [neighborhoodId, count] */
    @Query("""
            SELECT d.neighborhood.id, COUNT(d) FROM DamageAssessment d
            WHERE (:districtId IS NULL OR d.district.id = :districtId)
            GROUP BY d.neighborhood.id
            """)
    List<Object[]> reportDamageCountByNeighborhood(@Param("districtId") UUID districtId);

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
