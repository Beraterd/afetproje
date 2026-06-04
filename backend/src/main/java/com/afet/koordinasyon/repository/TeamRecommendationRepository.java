package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.TeamRecommendation;
import com.afet.koordinasyon.domain.enums.RecommendationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
