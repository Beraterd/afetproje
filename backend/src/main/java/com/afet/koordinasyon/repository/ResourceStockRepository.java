package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.ResourceStock;
import com.afet.koordinasyon.domain.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceStockRepository extends JpaRepository<ResourceStock, UUID> {

    /**
     * Aktif stokları opsiyonel filtrelerle döner. status/criticalOnly filtreleri
     * hesaplanmış olduğundan servis katmanında uygulanır.
     * <p>
     * NOT: arama için CONCAT/literal yerine her zaman NON-NULL {@code pattern} + boolean
     * {@code hasSearch} kullanılır. Hibernate 6, null String parametresini bir string
     * fonksiyonu (LOWER/CONCAT) içinde {@code bytea} olarak bind ediyor ve PostgreSQL
     * "function lower(bytea) does not exist" hatası veriyordu. pattern her zaman varchar
     * olarak bind edildiğinden bu sorun ortadan kalkar.
     */
    @Query("""
            SELECT s FROM ResourceStock s
            LEFT JOIN FETCH s.district d
            LEFT JOIN FETCH s.neighborhood n
            LEFT JOIN FETCH s.updatedBy u
            WHERE s.active = true
              AND (:districtId IS NULL OR d.id = :districtId)
              AND (:neighborhoodId IS NULL OR n.id = :neighborhoodId)
              AND (:category IS NULL OR s.category = :category)
              AND (:hasSearch = false
                   OR LOWER(s.name) LIKE :pattern
                   OR (s.warehouseName IS NOT NULL AND LOWER(s.warehouseName) LIKE :pattern))
            ORDER BY s.name ASC
            """)
    List<ResourceStock> search(@Param("districtId") UUID districtId,
                               @Param("neighborhoodId") UUID neighborhoodId,
                               @Param("category") ResourceType category,
                               @Param("hasSearch") boolean hasSearch,
                               @Param("pattern") String pattern);

    /** Talep uyarısı için: belirli bölge + kategorideki aktif stoklar. */
    @Query("""
            SELECT s FROM ResourceStock s
            WHERE s.active = true
              AND s.category = :category
              AND s.district.id = :districtId
              AND (:neighborhoodId IS NULL OR s.neighborhood.id = :neighborhoodId)
            """)
    List<ResourceStock> findForLookup(@Param("districtId") UUID districtId,
                                      @Param("neighborhoodId") UUID neighborhoodId,
                                      @Param("category") ResourceType category);
}
