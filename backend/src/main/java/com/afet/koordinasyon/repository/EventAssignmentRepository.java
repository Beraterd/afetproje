package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EventAssignment;
import com.afet.koordinasyon.domain.enums.EventInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventAssignmentRepository extends JpaRepository<EventAssignment, UUID> {

    Optional<EventAssignment> findByInvitationToken(String token);

    List<EventAssignment> findByEventIdOrderByInvitedAtDesc(UUID eventId);

    List<EventAssignment> findByEventIdAndStatus(UUID eventId, EventInvitationStatus status);

    int countByEventIdAndStatus(UUID eventId, EventInvitationStatus status);

    Optional<EventAssignment> findByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, EventInvitationStatus status);

    @Query("SELECT ea FROM EventAssignment ea JOIN FETCH ea.user WHERE ea.event.id = :eventId ORDER BY ea.invitedAt DESC")
    List<EventAssignment> findByEventIdWithUser(@Param("eventId") UUID eventId);

    /** Kullanıcının kabul ettiği tüm davetleri event detaylarıyla yükle (tamamlanan görev tespiti için). */
    @Query("SELECT ea FROM EventAssignment ea JOIN FETCH ea.event e LEFT JOIN FETCH e.team LEFT JOIN FETCH e.neighborhood n LEFT JOIN FETCH n.district WHERE ea.user.id = :userId AND ea.status = :status")
    List<EventAssignment> findByUserIdAndStatusWithEvent(@Param("userId") UUID userId,
                                                         @Param("status") EventInvitationStatus status);
}
