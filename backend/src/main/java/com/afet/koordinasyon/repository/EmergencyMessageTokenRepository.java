package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EmergencyMessageToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmergencyMessageTokenRepository extends JpaRepository<EmergencyMessageToken, UUID> {

    Optional<EmergencyMessageToken> findByTokenHash(String tokenHash);
}
