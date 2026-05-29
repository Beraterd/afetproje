package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.Neighborhood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NeighborhoodRepository extends JpaRepository<Neighborhood, UUID> {
    List<Neighborhood> findByDistrictId(UUID districtId);

    Optional<Neighborhood> findByCoordinatorId(UUID coordinatorId);

    /** Excel import için district'i JOIN FETCH ile tek sorguda yükler */
    @Query("SELECT n FROM Neighborhood n JOIN FETCH n.district")
    List<Neighborhood> findAllWithDistrict();

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Neighborhood n SET n.coordinator = NULL WHERE n.coordinator IS NOT NULL AND n.coordinator.id IN :userIds")
    int clearCoordinatorIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Neighborhood n SET n.coordinatorLocationUpdatedBy = NULL WHERE n.coordinatorLocationUpdatedBy IS NOT NULL AND n.coordinatorLocationUpdatedBy.id IN :userIds")
    int clearCoordinatorLocationUpdatedByIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Neighborhood n SET n.riskScore = 0, n.riskScoreUpdatedAt = CURRENT_TIMESTAMP")
    int resetAllRiskScores();
}
