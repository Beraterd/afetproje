package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {

    List<EmergencyContact> findByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    long countByOwnerId(UUID ownerId);

    Optional<EmergencyContact> findByOwnerIdAndContactId(UUID ownerId, UUID contactId);

    boolean existsByOwnerIdAndContactId(UUID ownerId, UUID contactId);

    void deleteByOwnerIdAndContactId(UUID ownerId, UUID contactId);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM EmergencyContact c WHERE c.owner.id IN :userIds OR c.contact.id IN :userIds")
    int deleteByOwnerOrContactIn(@Param("userIds") List<UUID> userIds);
}
