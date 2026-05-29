package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EarthquakeEventRepository extends JpaRepository<EarthquakeEvent, UUID> {

    boolean existsByExternalId(String externalId);

    // ── Sync yardımcıları ─────────────────────────────────────────────────────

    // Sync başında tüm indeksleri sıfırla; eski eventler NULL kalır
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EarthquakeEvent e SET e.afadOrderIndex = NULL")
    void resetAllAfadOrderIndices();

    // Mevcut event'e yeni AFAD pozisyonu yaz
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EarthquakeEvent e SET e.afadOrderIndex = :orderIndex WHERE e.externalId = :externalId")
    void updateAfadOrderIndex(@Param("externalId") String externalId,
                              @Param("orderIndex") int orderIndex);

    // Debug / sync log — eventTime'a göre en yeni tek kayıt
    Optional<EarthquakeEvent> findTopByOrderByEventTimeDescCreatedAtAsc();

    // ── Listeleme ─────────────────────────────────────────────────────────────

    // /latest → AFAD raw order: afad_order_index ASC, sadece son batch eventleri
    @Query("SELECT e FROM EarthquakeEvent e " +
           "WHERE e.afadOrderIndex IS NOT NULL " +
           "ORDER BY e.afadOrderIndex ASC " +
           "LIMIT 10")
    List<EarthquakeEvent> findLatestByAfadOrder();

    // Sayfalı liste → AFAD sıralı eventler önce, sonra eski eventler eventTime DESC
    @Query(
        value = "SELECT e FROM EarthquakeEvent e " +
                "ORDER BY (CASE WHEN e.afadOrderIndex IS NULL THEN 1 ELSE 0 END) ASC, " +
                "e.afadOrderIndex ASC, " +
                "e.eventTime DESC",
        countQuery = "SELECT COUNT(e) FROM EarthquakeEvent e"
    )
    Page<EarthquakeEvent> findAllOrdered(Pageable pageable);
}
