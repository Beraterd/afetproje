package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.TeamRecommendationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRecommendationMemberRepository extends JpaRepository<TeamRecommendationMember, UUID> {
    List<TeamRecommendationMember> findByRecommendationIdOrderByOrderIdAsc(UUID recommendationId);
}
