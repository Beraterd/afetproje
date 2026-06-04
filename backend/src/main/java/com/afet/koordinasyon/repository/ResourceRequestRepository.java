package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.ResourceRequest;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.ResourceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResourceRequestRepository
        extends JpaRepository<ResourceRequest, UUID>, JpaSpecificationExecutor<ResourceRequest> {

    Page<ResourceRequest> findByDistrictId(UUID districtId, Pageable pageable);

    Page<ResourceRequest> findByNeighborhoodId(UUID neighborhoodId, Pageable pageable);

    List<ResourceRequest> findByNeighborhoodIdAndStatus(UUID neighborhoodId, ResourceRequestStatus status);

    List<ResourceRequest> findByDistrictIdAndStatus(UUID districtId, ResourceRequestStatus status);

    List<ResourceRequest> findByNeighborhoodIdAndStatusIn(UUID neighborhoodId, List<ResourceRequestStatus> statuses);

    List<ResourceRequest> findByDistrictIdAndStatusIn(UUID districtId, List<ResourceRequestStatus> statuses);

    @Query("""
        SELECT r.neighborhood.id, COUNT(r), r.neighborhood.district.id
        FROM ResourceRequest r
        WHERE r.status = 'OPEN' AND r.neighborhood IS NOT NULL
        GROUP BY r.neighborhood.id, r.neighborhood.district.id
        """)
    List<Object[]> findOpenRequestCountByNeighborhood();

    @Query("""
        SELECT r.district.id, COUNT(r)
        FROM ResourceRequest r
        WHERE r.status = 'OPEN'
        GROUP BY r.district.id
        """)
    List<Object[]> findOpenRequestCountByDistrict();

    // ── Operations AI context queries ────────────────────────────────────────

    @Query("""
        SELECT r.resourceType, COUNT(r)
        FROM ResourceRequest r
        WHERE r.status IN :statuses
        GROUP BY r.resourceType
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> findResourceTypeSummary(@Param("statuses") List<ResourceRequestStatus> statuses);

    @Query("""
        SELECT r.resourceType, COUNT(r)
        FROM ResourceRequest r
        WHERE r.district.id = :districtId AND r.status IN :statuses
        GROUP BY r.resourceType
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> findResourceTypeSummaryByDistrict(
            @Param("districtId") UUID districtId,
            @Param("statuses") List<ResourceRequestStatus> statuses);

    @Query("""
        SELECT r.resourceType, COUNT(r)
        FROM ResourceRequest r
        WHERE r.neighborhood.id = :neighborhoodId AND r.status IN :statuses
        GROUP BY r.resourceType
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> findResourceTypeSummaryByNeighborhood(
            @Param("neighborhoodId") UUID neighborhoodId,
            @Param("statuses") List<ResourceRequestStatus> statuses);

    // ── Rapor: trend / süre / mahalle karşılaştırma ──────────────────────────

    /** Kapsamda belirli tarih aralığında oluşturulan talep sayısı (trend analizi). */
    @Query("""
            SELECT COUNT(r) FROM ResourceRequest r
            WHERE r.createdAt >= :from AND r.createdAt < :to
              AND (:districtId IS NULL OR r.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR r.neighborhood.id = :neighborhoodId)
            """)
    long reportCountCreatedBetween(@Param("districtId") UUID districtId,
                                   @Param("neighborhoodId") UUID neighborhoodId,
                                   @Param("from") java.time.OffsetDateTime from,
                                   @Param("to") java.time.OffsetDateTime to);

    /** Ortalama talep çözüm süresi (saniye) — resolvedAt set edilmiş talepler üzerinden. */
    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (r.resolved_at - r.created_at)))
              FROM resource_requests r
             WHERE r.resolved_at IS NOT NULL
               AND (:districtId IS NULL OR r.district_id = :districtId)
               AND (:neighborhoodId IS NULL OR r.neighborhood_id = :neighborhoodId)
            """, nativeQuery = true)
    Double reportAvgResolutionSeconds(@Param("districtId") UUID districtId,
                                      @Param("neighborhoodId") UUID neighborhoodId);

    /** Mahalle bazında toplam talep sayısı (ilçe kapsamında karşılaştırma). [neighborhoodId, count] */
    @Query("""
            SELECT r.neighborhood.id, COUNT(r) FROM ResourceRequest r
            WHERE r.neighborhood IS NOT NULL
              AND (:districtId IS NULL OR r.district.id = :districtId)
            GROUP BY r.neighborhood.id
            """)
    List<Object[]> reportRequestCountByNeighborhood(@Param("districtId") UUID districtId);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ResourceRequest r SET r.createdBy = :admin WHERE r.createdBy.id IN :userIds")
    int reassignCreatedBy(@Param("admin") User admin, @Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ResourceRequest r SET r.assignedTo = NULL WHERE r.assignedTo IS NOT NULL AND r.assignedTo.id IN :userIds")
    int clearAssignedToIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ResourceRequest r SET r.closedBy = NULL WHERE r.closedBy IS NOT NULL AND r.closedBy.id IN :userIds")
    int clearClosedByIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ResourceRequest r")
    int deleteAllRequests();
}
