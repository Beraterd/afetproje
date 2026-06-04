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

    // ── Rapor sorguları ────────────────────────────────────────────────────────

    /** Kapsamdaki toplam olay sayısı. */
    @Query("""
            SELECT COUNT(e) FROM Event e
            WHERE (:districtId IS NULL OR e.neighborhood.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR e.neighborhood.id = :neighborhoodId)
            """)
    long reportCountAllEvents(@Param("districtId") UUID districtId,
                              @Param("neighborhoodId") UUID neighborhoodId);

    /** Kapsamdaki belirli durumdaki olay sayısı (status zorunlu). */
    @Query("""
            SELECT COUNT(e) FROM Event e
            WHERE e.status = :status
              AND (:districtId IS NULL OR e.neighborhood.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR e.neighborhood.id = :neighborhoodId)
            """)
    long reportCountEventsByStatus(@Param("status") EventStatus status,
                                   @Param("districtId") UUID districtId,
                                   @Param("neighborhoodId") UUID neighborhoodId);

    /** Kapsamda belirtilen durumda OLMAYAN olaylarda görevli benzersiz ekip sayısı (aktif/sahadaki ekip). */
    @Query("""
            SELECT COUNT(DISTINCT e.team.id) FROM Event e
            WHERE e.status <> :excludeStatus
              AND (:districtId IS NULL OR e.neighborhood.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR e.neighborhood.id = :neighborhoodId)
            """)
    long reportActiveTeams(@Param("excludeStatus") EventStatus excludeStatus,
                           @Param("districtId") UUID districtId,
                           @Param("neighborhoodId") UUID neighborhoodId);

    /** Kapsamda belirli tarih aralığında oluşturulan olay sayısı (trend analizi). */
    @Query("""
            SELECT COUNT(e) FROM Event e
            WHERE e.createdAt >= :from AND e.createdAt < :to
              AND (:districtId IS NULL OR e.neighborhood.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR e.neighborhood.id = :neighborhoodId)
            """)
    long reportCountCreatedBetween(@Param("districtId") UUID districtId,
                                   @Param("neighborhoodId") UUID neighborhoodId,
                                   @Param("from") java.time.OffsetDateTime from,
                                   @Param("to") java.time.OffsetDateTime to);

    /** Ortalama müdahale süresi (saniye) — tamamlanan (closedAt set) olaylar üzerinden. */
    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (e.closed_at - e.created_at)))
              FROM events e
              JOIN neighborhoods n ON n.id = e.neighborhood_id
             WHERE e.closed_at IS NOT NULL
               AND (:districtId IS NULL OR n.district_id = :districtId)
               AND (:neighborhoodId IS NULL OR e.neighborhood_id = :neighborhoodId)
            """, nativeQuery = true)
    Double reportAvgResolutionSeconds(@Param("districtId") UUID districtId,
                                      @Param("neighborhoodId") UUID neighborhoodId);

    /** Mahalle bazında toplam olay sayısı (ilçe kapsamında karşılaştırma). [neighborhoodId, count] */
    @Query("""
            SELECT e.neighborhood.id, COUNT(e) FROM Event e
            WHERE (:districtId IS NULL OR e.neighborhood.district.id = :districtId)
            GROUP BY e.neighborhood.id
            """)
    List<Object[]> reportEventCountByNeighborhood(@Param("districtId") UUID districtId);

    /** Rapor: kapsamda ekip (team) tipine göre olay sayısı. [teamName, count] */
    @Query("""
            SELECT e.team.name, COUNT(e) FROM Event e
            WHERE (:districtId IS NULL OR e.neighborhood.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR e.neighborhood.id = :neighborhoodId)
            GROUP BY e.team.name
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> reportEventCountByTeam(@Param("districtId") UUID districtId,
                                          @Param("neighborhoodId") UUID neighborhoodId);

    /** Rapor: kapsamda belirtilen durumlardaki olayların toplam gereken personel sayısı. */
    @Query("""
            SELECT COALESCE(SUM(e.requiredPeople), 0) FROM Event e
            WHERE e.status IN :statuses
              AND (:districtId IS NULL OR e.neighborhood.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR e.neighborhood.id = :neighborhoodId)
            """)
    long reportRequiredPeopleSum(@Param("statuses") List<EventStatus> statuses,
                                 @Param("districtId") UUID districtId,
                                 @Param("neighborhoodId") UUID neighborhoodId);

    // ── Operations AI context queries ────────────────────────────────────────

    long countByStatusIn(List<EventStatus> statuses);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Event e SET e.createdBy = :admin WHERE e.createdBy.id IN :userIds")
    int reassignCreatedBy(@Param("admin") User admin, @Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Event e")
    int deleteAllEvents();
}
