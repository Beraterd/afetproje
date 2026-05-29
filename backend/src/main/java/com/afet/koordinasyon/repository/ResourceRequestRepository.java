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
