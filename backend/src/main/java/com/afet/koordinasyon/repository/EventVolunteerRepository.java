package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EventVolunteer;
import com.afet.koordinasyon.domain.enums.EventStatus;
import com.afet.koordinasyon.domain.enums.EventVolunteerStatus;
import com.afet.koordinasyon.domain.enums.TeamName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventVolunteerRepository extends JpaRepository<EventVolunteer, UUID> {
    Optional<EventVolunteer> findByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, EventVolunteerStatus status);

    Page<EventVolunteer> findByEventId(UUID eventId, Pageable pageable);

    List<EventVolunteer> findByEventIdAndStatus(UUID eventId, EventVolunteerStatus status);

    int countByEventIdAndStatus(UUID eventId, EventVolunteerStatus status);

    @Query("SELECT COUNT(ev) FROM EventVolunteer ev WHERE ev.user.id = :userId AND ev.status = :status")
    long countByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") EventVolunteerStatus status);

    /** Rapor: kapsamda göreve atanmış benzersiz gönüllü sayısı. */
    @Query("""
            SELECT COUNT(DISTINCT ev.user.id) FROM EventVolunteer ev
            WHERE (:districtId IS NULL OR ev.event.neighborhood.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR ev.event.neighborhood.id = :neighborhoodId)
            """)
    long reportAssignedVolunteers(@Param("districtId") UUID districtId,
                                  @Param("neighborhoodId") UUID neighborhoodId);

    @Query("SELECT ev FROM EventVolunteer ev WHERE ev.user.id = :userId")
    Page<EventVolunteer> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT ev FROM EventVolunteer ev JOIN FETCH ev.event e LEFT JOIN FETCH e.team LEFT JOIN FETCH e.neighborhood n LEFT JOIN FETCH n.district WHERE ev.user.id = :userId")
    List<EventVolunteer> findByUserIdAll(@Param("userId") UUID userId);

    @Query("SELECT ev FROM EventVolunteer ev WHERE ev.user.id = :userId AND ev.status = :status")
    Page<EventVolunteer> findByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") EventVolunteerStatus status,
            Pageable pageable);

    /**
     * Finds volunteers currently ASSIGNED to an active HASAR_TESPIT_EKIBI event
     * in the given district, excluding users already actively assigned to the
     * specified damage assessment. Deduplication by user is done in the service.
     */
    @Query("""
        SELECT ev FROM EventVolunteer ev
        JOIN FETCH ev.event e
        JOIN e.team t
        WHERE t.name = :teamName
          AND e.status IN :statuses
          AND e.neighborhood.district.id = :districtId
          AND ev.status = :volunteerStatus
          AND ev.user.id NOT IN (
            SELECT a.user.id FROM DamageAssessmentAssignment a
            WHERE a.damageAssessment.id = :damageAssessmentId
              AND a.active = true
          )
        """)
    List<EventVolunteer> findEligibleAssignees(
            @Param("teamName") TeamName teamName,
            @Param("statuses") List<EventStatus> statuses,
            @Param("districtId") UUID districtId,
            @Param("volunteerStatus") EventVolunteerStatus volunteerStatus,
            @Param("damageAssessmentId") UUID damageAssessmentId);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM EventVolunteer ev WHERE ev.user.id IN :userIds")
    int deleteByUserIdIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM EventVolunteer ev")
    int deleteAllVolunteers();
}
