package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EmergencyStatusMessage;
import com.afet.koordinasyon.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyStatusMessageRepository extends JpaRepository<EmergencyStatusMessage, UUID> {

    Optional<EmergencyStatusMessage> findByClientGeneratedId(String clientGeneratedId);

    List<EmergencyStatusMessage> findBySenderIdAndSimulationIdOrderByCreatedAtDesc(UUID senderId, UUID simulationId);

    List<EmergencyStatusMessage> findBySenderIdOrderByCreatedAtDesc(UUID senderId);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE EmergencyStatusMessage m SET m.sender = :admin WHERE m.sender.id IN :userIds")
    int reassignSenderIn(@Param("admin") User admin, @Param("userIds") List<UUID> userIds);
}
