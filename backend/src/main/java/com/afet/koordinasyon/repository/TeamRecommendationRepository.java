package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.TeamRecommendation;
import com.afet.koordinasyon.domain.enums.RecommendationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRecommendationRepository extends JpaRepository<TeamRecommendation, UUID> {

    Page<TeamRecommendation> findByStatusOrderByCreatedAtDesc(RecommendationStatus status, Pageable pageable);

    Page<TeamRecommendation> findByRequestedByIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<TeamRecommendation> findByDistrictIdAndStatusOrderByCreatedAtDesc(UUID districtId, RecommendationStatus status, Pageable pageable);

    List<TeamRecommendation> findByStatus(RecommendationStatus status);

    Optional<TeamRecommendation> findTopByEventIdOrderByCreatedAtDesc(UUID eventId);

    List<TeamRecommendation> findByEventIdOrderByCreatedAtDesc(UUID eventId);

    // ── Devir teslim özeti ────────────────────────────────────────────────────

    @Query("""
            SELECT COUNT(t) FROM TeamRecommendation t
            WHERE t.status = 'APPROVED' AND t.approvedAt >= :from AND t.approvedAt < :to
              AND (:districtId IS NULL OR t.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR t.neighborhood.id = :neighborhoodId)
            """)
    long countApprovedBetween(@Param("from") OffsetDateTime from,
                              @Param("to") OffsetDateTime to,
                              @Param("districtId") UUID districtId,
                              @Param("neighborhoodId") UUID neighborhoodId);
}
