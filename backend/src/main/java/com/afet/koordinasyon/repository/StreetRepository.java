package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.Street;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreetRepository extends JpaRepository<Street, UUID> {

    long countByDistrictId(UUID districtId);

    long countByNeighborhoodId(UUID neighborhoodId);

    /** Import-time upsert anahtarı — bkz. V91'in uq_streets_district_neighborhood_name'i. */
    Optional<Street> findByDistrictIdAndNeighborhoodIdAndName(UUID districtId, UUID neighborhoodId, String name);

    /**
     * Mahalle-scoped autocomplete arama. `q` çağıran tarafından (bkz. StreetService) zaten
     * {@code TurkishTextNormalizer} ile normalize edilmiş olmalıdır.
     *
     * Ranking (item 13): exact match (0) &lt; normalized_name tamamı q ile başlıyor (1) &lt;
     * herhangi bir kelime q ile başlıyor — "word-prefix" (2) &lt; q herhangi bir yerde geçiyor (3).
     * Aynı rank içinde kısa/alfabetik isim önce gelir — kullanıcıya en "temiz" eşleşme üstte
     * görünür (ör. "Selvi Sokak" "Selvi Boylum Sokak"'tan önce).
     */
    @Query(value = """
            SELECT s.* FROM streets s
            WHERE s.district_id = :districtId
              AND s.neighborhood_id = :neighborhoodId
              AND s.normalized_name LIKE CONCAT('%', :q, '%')
            ORDER BY
              CASE
                WHEN s.normalized_name = :q THEN 0
                WHEN s.normalized_name LIKE CONCAT(:q, '%') THEN 1
                WHEN s.normalized_name LIKE CONCAT('% ', :q, '%') THEN 2
                ELSE 3
              END,
              length(s.normalized_name) ASC,
              s.name ASC
            """, nativeQuery = true)
    List<Street> search(@Param("districtId") UUID districtId,
                         @Param("neighborhoodId") UUID neighborhoodId,
                         @Param("q") String q,
                         Pageable pageable);

    /** Admin coverage raporu (bkz. AdminStreetController) — ilçe başına gerçek DB envanteri. */
    interface DistrictStreetStats {
        UUID getDistrictId();
        String getDistrictName();
        long getStreetCount();
        long getNeighborhoodCount();
        OffsetDateTime getLastImportAt();
    }

    @Query("""
            SELECT s.district.id AS districtId, s.district.name AS districtName,
                   COUNT(s) AS streetCount, COUNT(DISTINCT s.neighborhood.id) AS neighborhoodCount,
                   MAX(s.updatedAt) AS lastImportAt
            FROM Street s
            GROUP BY s.district.id, s.district.name
            """)
    List<DistrictStreetStats> aggregateByDistrict();
}
