package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    /** Birden fazla entity ID'si için TEK sorguda audit log çeker — N+1'i önlemek için (bkz. EventService.getTimeline). */
    List<AuditLog> findByEntityTypeAndEntityIdIn(String entityType, Collection<UUID> entityIds);

    Page<AuditLog> findByActorId(UUID actorId, Pageable pageable);

    /** Rapor: belirli aksiyonun [from,to) aralığındaki adedi (örn. koordinatör atamaları). */
    long countByActionAndCreatedAtBetween(String action, OffsetDateTime from, OffsetDateTime to);
}
