package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShortLinkRepository extends JpaRepository<ShortLink, UUID> {

    Optional<ShortLink> findByCode(String code);

    boolean existsByCode(String code);
}
