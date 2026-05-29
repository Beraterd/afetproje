package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.AssemblyArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssemblyAreaRepository extends JpaRepository<AssemblyArea, UUID> {

    /** Tüm kayıtlar — dahili kullanım için */
    List<AssemblyArea> findByNeighborhoodId(UUID neighborhoodId);

    /**
     * AFAD deprem mail bildirimi için: googleMapsUrl zorunlu tutulmadan aktif/onaylı kayıtlar.
     * URL üretimi servis katmanında yapılır (googleMapsUrl → lat/lon → name+address sıralamasıyla).
     */
    @Query("""
            SELECT a FROM AssemblyArea a
            WHERE a.neighborhood.id = :neighborhoodId
              AND a.isActive = true
              AND a.needsReview = false
            """)
    List<AssemblyArea> findActiveApprovedByNeighborhoodId(@Param("neighborhoodId") UUID neighborhoodId);

    /**
     * Simülasyon mail gönderiminde kullanılan filtreli sorgu.
     * Koşullar: is_active=TRUE, needs_review=FALSE, google_maps_url NOT NULL.
     * Kaynak adından bağımsız tüm doğrulanmış kayıtları döner.
     */
    @Query("""
            SELECT a FROM AssemblyArea a
            WHERE a.neighborhood.id = :neighborhoodId
              AND a.isActive = true
              AND a.needsReview = false
              AND a.googleMapsUrl IS NOT NULL
            """)
    List<AssemblyArea> findVerifiedByNeighborhoodId(@Param("neighborhoodId") UUID neighborhoodId);

    /**
     * Admin review sayfası için filtrelenmiş sayfalı sorgu.
     * Tüm parametreler opsiyoneldir; null geçilirse filtre uygulanmaz.
     * search: name veya address üzerinde ILIKE arama yapar.
     */
    @Query(value = """
            SELECT a FROM AssemblyArea a
            LEFT JOIN FETCH a.district d
            LEFT JOIN FETCH a.neighborhood n
            WHERE (:districtId IS NULL OR d.id = :districtId)
              AND (:neighborhoodId IS NULL OR n.id = :neighborhoodId)
              AND (:sourceName IS NULL OR a.sourceName = :sourceName)
              AND (:needsReview IS NULL OR a.needsReview = :needsReview)
              AND (:active IS NULL OR a.isActive = :active)
              AND (:search IS NULL
                   OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(a.address, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(a) FROM AssemblyArea a
            LEFT JOIN a.district d
            LEFT JOIN a.neighborhood n
            WHERE (:districtId IS NULL OR d.id = :districtId)
              AND (:neighborhoodId IS NULL OR n.id = :neighborhoodId)
              AND (:sourceName IS NULL OR a.sourceName = :sourceName)
              AND (:needsReview IS NULL OR a.needsReview = :needsReview)
              AND (:active IS NULL OR a.isActive = :active)
              AND (:search IS NULL
                   OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(a.address, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<AssemblyArea> findWithFilters(
            @Param("districtId") UUID districtId,
            @Param("neighborhoodId") UUID neighborhoodId,
            @Param("sourceName") String sourceName,
            @Param("needsReview") Boolean needsReview,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable);

    int countByNeedsReviewTrue();

    @Query("SELECT COUNT(a) FROM AssemblyArea a")
    int countTotal();

    /** Coverage raporu: doğrulanmış toplanma alanı olan mahalle UUID'lerini döner. */
    @Query(value = """
            SELECT DISTINCT neighborhood_id
              FROM assembly_areas
             WHERE is_active = TRUE
               AND needs_review = FALSE
               AND google_maps_url IS NOT NULL
            """, nativeQuery = true)
    List<UUID> findCoveredNeighborhoodIds();
}
