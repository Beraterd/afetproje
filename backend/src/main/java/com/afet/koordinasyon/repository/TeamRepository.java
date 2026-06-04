package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.Team;
import com.afet.koordinasyon.domain.enums.TeamName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByName(TeamName name);

    Optional<Team> findByTeamCode(String teamCode);

    boolean existsByTeamCode(String teamCode);

    List<Team> findByDistrictId(UUID districtId);

    int countByDistrictId(UUID districtId);

    @Query("SELECT t FROM Team t WHERE t.district.id = :districtId ORDER BY t.teamCode ASC")
    List<Team> findByDistrictIdOrderByCode(@Param("districtId") UUID districtId);

    /**
     * Returns max sequential number for a combined prefix (e.g. "PEN-TAH").
     * Format: [DIST_PREFIX]-[TYPE_CODE]-[SEQ]
     * Example: "PEN-TAH-3" → 3 for prefix "PEN-TAH"
     */
    @Query(value = """
        SELECT COALESCE(MAX(CAST(SPLIT_PART(team_code, '-', 3) AS INTEGER)), 0)
        FROM teams
        WHERE team_code LIKE CONCAT(:prefix, '-%')
          AND team_code ~ ('^' || :prefix || '-[0-9]+$')
        """, nativeQuery = true)
    int findMaxTeamNumberByCombinedPrefix(@Param("prefix") String prefix);

    /** Legacy method — kept for backfill compatibility only. */
    @Query(value = """
        SELECT COALESCE(MAX(CAST(SPLIT_PART(team_code, '-', 2) AS INTEGER)), 0)
        FROM teams
        WHERE team_code LIKE CONCAT(:prefix, '-%')
          AND team_code ~ ('^' || :prefix || '-[0-9]+$')
        """, nativeQuery = true)
    int findMaxTeamNumberByPrefix(@Param("prefix") String prefix);
}
