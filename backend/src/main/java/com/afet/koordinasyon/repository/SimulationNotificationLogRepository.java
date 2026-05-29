package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.SimulationNotificationLog;
import com.afet.koordinasyon.domain.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SimulationNotificationLogRepository extends JpaRepository<SimulationNotificationLog, UUID> {
    Page<SimulationNotificationLog> findBySimulationId(UUID simulationId, Pageable pageable);

    Page<SimulationNotificationLog> findBySimulationIdAndStatus(UUID simulationId, NotificationStatus status,
            Pageable pageable);

    int countBySimulationIdAndStatus(UUID simulationId, NotificationStatus status);

    int countBySimulationId(UUID simulationId);

    /** Async e-posta gönderimi için sayfalama olmaksızın QUEUED kayıtları çeker. */
    @Query("SELECT l FROM SimulationNotificationLog l JOIN FETCH l.user u LEFT JOIN FETCH u.neighborhood WHERE l.simulation.id = :simulationId AND l.status = :status")
    List<SimulationNotificationLog> findBySimulationIdAndStatusUnpaged(
            @Param("simulationId") UUID simulationId,
            @Param("status") NotificationStatus status);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SimulationNotificationLog l WHERE l.user.id IN :userIds")
    int deleteByUserIdIn(@Param("userIds") List<UUID> userIds);
}
