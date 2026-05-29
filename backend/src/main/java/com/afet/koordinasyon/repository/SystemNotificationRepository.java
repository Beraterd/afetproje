package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.SystemNotification;
import com.afet.koordinasyon.domain.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, UUID> {

    /**
     * Kullanıcının görebileceği tüm bildirimleri döner.
     * Görünürlük kuralı:
     *   1) Doğrudan bu kullanıcıya gönderilmiş (recipient_user_id)
     *   2) Rolüne broadcast edilmiş (target_role)
     *   3) Kendi ilçesine ait + DISTRICT_COORDINATOR rolü
     *   4) Kendi mahallesine ait + NEIGHBORHOOD_COORDINATOR rolü
     */
    @Query("""
        SELECT n FROM SystemNotification n WHERE (
            n.recipientUser.id = :userId
            OR n.targetRole = :role
            OR (:districtId IS NOT NULL
                AND n.district IS NOT NULL
                AND n.district.id = :districtId
                AND :role = 'DISTRICT_COORDINATOR')
            OR (:neighborhoodId IS NOT NULL
                AND n.neighborhood IS NOT NULL
                AND n.neighborhood.id = :neighborhoodId
                AND :role = 'NEIGHBORHOOD_COORDINATOR')
        )
        AND (:type IS NULL OR n.type = :type)
        AND (:isRead IS NULL OR n.isRead = :isRead)
        ORDER BY n.createdAt DESC
        """)
    Page<SystemNotification> findVisible(
            @Param("userId")         UUID             userId,
            @Param("role")           String           role,
            @Param("districtId")     UUID             districtId,
            @Param("neighborhoodId") UUID             neighborhoodId,
            @Param("type")           NotificationType type,
            @Param("isRead")         Boolean          isRead,
            Pageable                                  pageable);

    /** Okunmamış bildirim sayısı. */
    @Query("""
        SELECT COUNT(n) FROM SystemNotification n WHERE n.isRead = false AND (
            n.recipientUser.id = :userId
            OR n.targetRole = :role
            OR (:districtId IS NOT NULL
                AND n.district IS NOT NULL
                AND n.district.id = :districtId
                AND :role = 'DISTRICT_COORDINATOR')
            OR (:neighborhoodId IS NOT NULL
                AND n.neighborhood IS NOT NULL
                AND n.neighborhood.id = :neighborhoodId
                AND :role = 'NEIGHBORHOOD_COORDINATOR')
        )
        """)
    long countUnread(
            @Param("userId")         UUID userId,
            @Param("role")           String role,
            @Param("districtId")     UUID districtId,
            @Param("neighborhoodId") UUID neighborhoodId);

    /** Kullanıcının görebildiği tüm okunmamışları işaretle. */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE SystemNotification n SET n.isRead = true, n.readAt = :now WHERE n.isRead = false AND (
            n.recipientUser.id = :userId
            OR n.targetRole = :role
            OR (:districtId IS NOT NULL
                AND n.district IS NOT NULL
                AND n.district.id = :districtId
                AND :role = 'DISTRICT_COORDINATOR')
            OR (:neighborhoodId IS NOT NULL
                AND n.neighborhood IS NOT NULL
                AND n.neighborhood.id = :neighborhoodId
                AND :role = 'NEIGHBORHOOD_COORDINATOR')
        )
        """)
    int markAllRead(
            @Param("userId")         UUID             userId,
            @Param("role")           String           role,
            @Param("districtId")     UUID             districtId,
            @Param("neighborhoodId") UUID             neighborhoodId,
            @Param("now")            OffsetDateTime   now);
}
