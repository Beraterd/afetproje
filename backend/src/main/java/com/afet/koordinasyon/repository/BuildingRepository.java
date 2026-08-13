package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.Building;
import com.afet.koordinasyon.domain.enums.BuildingSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building, UUID> {

    Optional<Building> findBySourceAndExternalId(BuildingSource source, String externalId);

    long countByDistrictId(UUID districtId);

    long countByNeighborhoodId(UUID neighborhoodId);

    /** Admin import-status raporu (bkz. AdminBuildingController) — ilçe başına gerçek DB
     *  envanteri, yeni bir "import log" tablosu gerektirmeden created_at/updated_at'ten türetilir. */
    interface DistrictBuildingStats {
        UUID getDistrictId();
        String getDistrictName();
        BuildingSource getSource();
        long getBuildingCount();
        OffsetDateTime getLastImportAt();
    }

    @Query("""
            SELECT b.district.id AS districtId, b.district.name AS districtName, b.source AS source,
                   COUNT(b) AS buildingCount, MAX(b.updatedAt) AS lastImportAt
            FROM Building b
            GROUP BY b.district.id, b.district.name, b.source
            """)
    List<DistrictBuildingStats> aggregateByDistrict();

    /** Mahalle bazlı kapsama raporu (bkz. item 7) — yalnızca bina bulunan mahalleleri döner,
     *  0 bina çıkan mahalleler çağıran tarafta (NeighborhoodRepository.findByDistrictId farkı ile) tespit edilir. */
    interface NeighborhoodBuildingCount {
        UUID getNeighborhoodId();
        long getBuildingCount();
    }

    @Query("""
            SELECT b.neighborhood.id AS neighborhoodId, COUNT(b) AS buildingCount
            FROM Building b
            WHERE b.district.id = :districtId
            GROUP BY b.neighborhood.id
            """)
    List<NeighborhoodBuildingCount> countByNeighborhoodForDistrict(@Param("districtId") UUID districtId);

    /**
     * Rectangle-overlap bbox query — no PostGIS in this deployment, so this is a plain
     * indexed range predicate on the precomputed bbox_* columns (see idx_buildings_bbox).
     * neighborhoodId is optional (null = whole district).
     */
    @Query("""
            SELECT b FROM Building b
            WHERE b.district.id = :districtId
              AND (:neighborhoodId IS NULL OR b.neighborhood.id = :neighborhoodId)
              AND b.bboxMinLon <= :maxLon AND b.bboxMaxLon >= :minLon
              AND b.bboxMinLat <= :maxLat AND b.bboxMaxLat >= :minLat
            """)
    Page<Building> findByDistrictAndBboxOverlap(
            @Param("districtId") UUID districtId,
            @Param("neighborhoodId") UUID neighborhoodId,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon,
            Pageable pageable);

    /**
     * `q`, çağıran tarafından (bkz. BuildingService.search) zaten {@code TurkishTextNormalizer}
     * ile normalize edilmiş olmalıdır — burada tekrar normalize edilmez, `searchText` sütunu
     * import sırasında aynı normalizasyonla önceden hesaplanıp saklanır (idx_buildings_search_text_trgm).
     */
    @Query("""
            SELECT b FROM Building b
            WHERE b.district.id = :districtId
              AND b.searchText LIKE CONCAT('%', :q, '%')
            ORDER BY b.displayAddress ASC
            """)
    Page<Building> searchByDistrict(@Param("districtId") UUID districtId, @Param("q") String q, Pageable pageable);
}
