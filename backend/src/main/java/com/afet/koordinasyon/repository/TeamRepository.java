package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.Team;
import com.afet.koordinasyon.domain.enums.TeamName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    Optional<Team> findByName(TeamName name);
}
