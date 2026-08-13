package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.BuildingSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Gerçek bina footprint kaydı (kaynak: OpenStreetMap, ilçe bazlı import — bkz.
 * Building3dProperties.enabledDistricts hangi ilçelerin aktif olduğunun tek doğruluk kaynağıdır).
 * Geometri, District/Neighborhood.geojsonPolygon ile aynı desende jsonb GeoJSON olarak
 * saklanır — bu deploymentta PostGIS yok.
 * bbox* alanları, bbox sorgularını PostGIS olmadan indeksli aralık taraması yapabilmek için
 * import sırasında önceden hesaplanır.
 */
@Entity
@Table(name = "buildings", uniqueConstraints = @UniqueConstraint(columnNames = { "source", "external_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private BuildingSource source;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id", nullable = false)
    private Neighborhood neighborhood;

    @Column(name = "street_name", length = 255)
    private String streetName;

    @Column(name = "building_number", length = 50)
    private String buildingNumber;

    @Column(name = "display_address", columnDefinition = "TEXT")
    private String displayAddress;

    /** Türkçe-katlanmış, sokak/cadde/mahalle kısaltmalarından arındırılmış arama metni — yalnızca
     *  BuildingImportService tarafından yazılır, kullanıcıya asla gösterilmez (bkz. TurkishTextNormalizer). */
    @Column(name = "search_text", columnDefinition = "TEXT")
    private String searchText;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    /** Ham GeoJSON geometry (Polygon/MultiPolygon) — District/Neighborhood.geojsonPolygon ile aynı depolama deseni. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geometry", columnDefinition = "jsonb", nullable = false)
    private String geometry;

    /** OSM `height` etiketinden gerçek yükseklik (metre) — yoksa null, ASLA uydurulmaz. */
    @Column(name = "height_m", precision = 6, scale = 2)
    private BigDecimal heightM;

    /** OSM `building:levels` etiketinden kat sayısı — yoksa null. */
    @Column(name = "levels")
    private Integer levels;

    /** 3B render'da kullanılan yükseklik: gerçek / kat sayısından hesap / sabit fallback. */
    @Column(name = "estimated_height_m", nullable = false, precision = 6, scale = 2)
    private BigDecimal estimatedHeightM;

    /** "osm_height" | "levels_x3" | "fallback" — frontend BuildingSummaryResponse.heightSource ile birebir. */
    @Column(name = "height_source", nullable = false, length = 20)
    private String heightSource;

    @Column(name = "building_type", length = 100)
    private String buildingType;

    @Column(length = 255)
    private String name;

    @Column(name = "bbox_min_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal bboxMinLat;

    @Column(name = "bbox_max_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal bboxMaxLat;

    @Column(name = "bbox_min_lon", nullable = false, precision = 10, scale = 7)
    private BigDecimal bboxMinLon;

    @Column(name = "bbox_max_lon", nullable = false, precision = 10, scale = 7)
    private BigDecimal bboxMaxLon;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
