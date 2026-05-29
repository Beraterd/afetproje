package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EarthquakeSimulation;
import com.afet.koordinasyon.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EarthquakeSimulationRepository extends JpaRepository<EarthquakeSimulation, UUID> {
    Page<EarthquakeSimulation> findByDistrictId(UUID districtId, Pageable pageable);

    Optional<EarthquakeSimulation> findTop1ByOrderByCreatedAtDesc();

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE EarthquakeSimulation s SET s.createdBy = :admin WHERE s.createdBy.id IN :userIds")
    int reassignCreatedBy(@Param("admin") User admin, @Param("userIds") List<UUID> userIds);
}
