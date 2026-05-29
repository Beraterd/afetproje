package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.DistrictCoordinationCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DistrictCoordinationCenterRepository extends JpaRepository<DistrictCoordinationCenter, UUID> {
    Optional<DistrictCoordinationCenter> findByDistrictId(UUID districtId);

    boolean existsByDistrictId(UUID districtId);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DistrictCoordinationCenter c SET c.updatedBy = NULL WHERE c.updatedBy IS NOT NULL AND c.updatedBy.id IN :userIds")
    int clearUpdatedByIn(@Param("userIds") List<UUID> userIds);
}
