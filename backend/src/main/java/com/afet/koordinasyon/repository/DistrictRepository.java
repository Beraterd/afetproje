package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DistrictRepository extends JpaRepository<District, UUID> {
    List<District> findByActiveTrue();

    Optional<District> findByName(String name);

    Optional<District> findByCoordinatorId(UUID coordinatorId);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE District d SET d.coordinator = NULL WHERE d.coordinator IS NOT NULL AND d.coordinator.id IN :userIds")
    int clearCoordinatorIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE District d SET d.coordinatorLocationUpdatedBy = NULL WHERE d.coordinatorLocationUpdatedBy IS NOT NULL AND d.coordinatorLocationUpdatedBy.id IN :userIds")
    int clearCoordinatorLocationUpdatedByIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE District d SET d.riskScore = 0, d.riskScoreUpdatedAt = CURRENT_TIMESTAMP")
    int resetAllRiskScores();
}
