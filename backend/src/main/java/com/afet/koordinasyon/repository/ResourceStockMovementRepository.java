package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.ResourceStockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceStockMovementRepository extends JpaRepository<ResourceStockMovement, UUID> {

    @Query("""
            SELECT m FROM ResourceStockMovement m
            LEFT JOIN FETCH m.createdBy u
            WHERE m.stock.id = :stockId
            ORDER BY m.createdAt DESC
            """)
    List<ResourceStockMovement> findByStockIdOrderByCreatedAtDesc(@Param("stockId") UUID stockId);

    /** Rapor: belirli bölgede son dönemde tüketilen (negatif hareket) toplam miktar. */
    @Query("""
            SELECT COALESCE(SUM(-m.quantityChange), 0) FROM ResourceStockMovement m
            WHERE m.quantityChange < 0 AND m.createdAt >= :since
              AND (:districtId IS NULL OR m.stock.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR m.stock.neighborhood.id = :neighborhoodId)
            """)
    long sumConsumedSince(@Param("districtId") UUID districtId,
                          @Param("neighborhoodId") UUID neighborhoodId,
                          @Param("since") java.time.OffsetDateTime since);

    /** Rapor: belirli bölgede [from, to) aralığında tüketilen (negatif hareket) toplam miktar (trend). */
    @Query("""
            SELECT COALESCE(SUM(-m.quantityChange), 0) FROM ResourceStockMovement m
            WHERE m.quantityChange < 0 AND m.createdAt >= :from AND m.createdAt < :to
              AND (:districtId IS NULL OR m.stock.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR m.stock.neighborhood.id = :neighborhoodId)
            """)
    long sumConsumedBetween(@Param("districtId") UUID districtId,
                            @Param("neighborhoodId") UUID neighborhoodId,
                            @Param("from") java.time.OffsetDateTime from,
                            @Param("to") java.time.OffsetDateTime to);

    /** Rapor: belirli bölgede [from, to) aralığındaki net stok değişimi (giriş - çıkış) (trend). */
    @Query("""
            SELECT COALESCE(SUM(m.quantityChange), 0) FROM ResourceStockMovement m
            WHERE m.createdAt >= :from AND m.createdAt < :to
              AND (:districtId IS NULL OR m.stock.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR m.stock.neighborhood.id = :neighborhoodId)
            """)
    long sumNetChangeBetween(@Param("districtId") UUID districtId,
                             @Param("neighborhoodId") UUID neighborhoodId,
                             @Param("from") java.time.OffsetDateTime from,
                             @Param("to") java.time.OffsetDateTime to);

    /** Rapor: mahalle bazında toplam tüketim (negatif hareketler). [neighborhoodId, consumed] */
    @Query("""
            SELECT m.stock.neighborhood.id, COALESCE(SUM(-m.quantityChange), 0)
            FROM ResourceStockMovement m
            WHERE m.quantityChange < 0 AND m.stock.neighborhood IS NOT NULL
              AND (:districtId IS NULL OR m.stock.district.id = :districtId)
            GROUP BY m.stock.neighborhood.id
            """)
    List<Object[]> reportConsumedByNeighborhood(@Param("districtId") UUID districtId);
}
