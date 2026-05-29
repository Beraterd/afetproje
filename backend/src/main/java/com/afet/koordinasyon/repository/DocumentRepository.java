package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.Document;
import com.afet.koordinasyon.domain.enums.DocumentStatus;
import com.afet.koordinasyon.domain.enums.DocumentType;
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
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserId(UUID userId);

    Optional<Document> findByDownloadToken(UUID downloadToken);

    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.status = :status AND d.user.district.id = :districtId")
    Page<Document> findByStatusAndUserDistrictId(@Param("status") DocumentStatus status,
            @Param("districtId") UUID districtId,
            Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.status = :status AND d.user.neighborhood.id = :neighborhoodId")
    Page<Document> findByStatusAndUserNeighborhoodId(@Param("status") DocumentStatus status,
            @Param("neighborhoodId") UUID neighborhoodId,
            Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.user.id = :userId " +
            "AND d.documentType = :documentType AND d.status = 'APPROVED' " +
            "ORDER BY d.reviewedAt DESC")
    List<Document> findApprovedByUserAndType(@Param("userId") UUID userId,
            @Param("documentType") DocumentType documentType);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Document d SET d.reviewedBy = NULL WHERE d.reviewedBy IS NOT NULL AND d.reviewedBy.id IN :userIds")
    int clearReviewedByIn(@Param("userIds") List<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Document d WHERE d.user.id IN :userIds")
    int deleteByUserIdIn(@Param("userIds") List<UUID> userIds);
}
