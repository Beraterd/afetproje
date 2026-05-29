package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EmergencyStatusMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EmergencyStatusMessageLogRepository extends JpaRepository<EmergencyStatusMessageLog, UUID> {

    List<EmergencyStatusMessageLog> findByMessageId(UUID messageId);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM EmergencyStatusMessageLog l WHERE l.recipient.id IN :userIds")
    int deleteByRecipientIdIn(@Param("userIds") List<UUID> userIds);
}
