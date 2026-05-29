package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.SmsLoginCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface SmsLoginCodeRepository extends JpaRepository<SmsLoginCode, UUID> {

    @Query("SELECT COUNT(c) FROM SmsLoginCode c WHERE c.user.id = :userId " +
           "AND c.createdAt > :since AND c.usedAt IS NULL")
    long countActiveCodesSince(@Param("userId") UUID userId, @Param("since") OffsetDateTime since);
}
