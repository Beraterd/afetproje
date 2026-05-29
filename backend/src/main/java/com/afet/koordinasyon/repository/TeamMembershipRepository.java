package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.TeamMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {
    Optional<TeamMembership> findByUserIdAndTeamId(UUID userId, UUID teamId);

    boolean existsByUserIdAndTeamIdAndActiveTrue(UUID userId, UUID teamId);

    Page<TeamMembership> findByTeamIdAndActiveTrue(UUID teamId, Pageable pageable);

    int countByTeamIdAndActiveTrue(UUID teamId);

    boolean existsByUserIdAndActiveTrue(UUID userId);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TeamMembership m WHERE m.user.id IN :userIds")
    int deleteByUserIdIn(@Param("userIds") List<UUID> userIds);
}
