package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.Event;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    List<Event> findByNeighborhoodId(UUID neighborhoodId);

    @Query("SELECT e FROM Event e WHERE e.neighborhood.district.id = :districtId")
    List<Event> findAllByDistrictId(@Param("districtId") UUID districtId);

    @Query("SELECT e FROM Event e WHERE e.neighborhood.id = :neighborhoodId AND e.status IN :statuses")
    List<Event> findActiveByNeighborhoodId(@Param("neighborhoodId") UUID neighborhoodId,
            @Param("statuses") java.util.List<EventStatus> statuses);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.neighborhood.id = :neighborhoodId AND e.status = :status")
    long countByNeighborhoodIdAndStatus(@Param("neighborhoodId") UUID neighborhoodId,
            @Param("status") EventStatus status);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.neighborhood.district.id = :districtId AND e.status = :status")
    long countByDistrictIdAndStatus(@Param("districtId") UUID districtId,
            @Param("status") EventStatus status);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.neighborhood.id = :neighborhoodId AND e.status IN :statuses")
    long countByNeighborhoodIdAndStatusIn(@Param("neighborhoodId") UUID neighborhoodId,
            @Param("statuses") List<EventStatus> statuses);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.neighborhood.district.id = :districtId AND e.status IN :statuses")
    long countByDistrictIdAndStatusIn(@Param("districtId") UUID districtId,
            @Param("statuses") List<EventStatus> statuses);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Event e SET e.createdBy = :admin WHERE e.createdBy.id IN :userIds")
    int reassignCreatedBy(@Param("admin") User admin, @Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Event e")
    int deleteAllEvents();
}
