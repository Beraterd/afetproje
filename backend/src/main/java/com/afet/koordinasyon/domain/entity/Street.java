package com.afet.koordinasyon.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bir mahalle içindeki mantıksal sokak/cadde/bulvar segmenti (kaynak: OpenStreetMap named
 * highway=* way'leri, bkz. scripts/fetch_district_streets.py). Aynı fiziksel yol birden fazla
 * mahalleden geçiyorsa her mahallede AYRI bir satır olarak bulunur — geometry o mahalleye
 * clip edilmiştir (bkz. V91 migration doc). Bu sayede mahalle-scoped arama
 * (StreetRepository.search) ekstra spatial sorgu gerektirmeden districtId+neighborhoodId
 * filtresiyle çalışır.
 */
@Entity
@Table(name = "streets", uniqueConstraints = @UniqueConstraint(columnNames = { "district_id", "neighborhood_id", "name" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Street {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id", nullable = false)
    private Neighborhood neighborhood;

    /** Kullanıcıya gösterilen canonical isim — asla normalize edilmez. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** TurkishTextNormalizer.normalize(name) — yalnızca StreetImportService tarafından yazılır. */
    @Column(name = "normalized_name", columnDefinition = "TEXT", nullable = false)
    private String normalizedName;

    /** OSM `highway` etiketi (residential, primary, pedestrian, vb.) — yoksa null. */
    @Column(name = "road_type", length = 30)
    private String roadType;

    /** GeoJSON LineString/MultiLineString, ilgili mahalleye clip edilmiş. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geometry", columnDefinition = "jsonb", nullable = false)
    private String geometry;

    @Column(name = "bbox_min_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal bboxMinLat;

    @Column(name = "bbox_max_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal bboxMaxLat;

    @Column(name = "bbox_min_lon", nullable = false, precision = 10, scale = 7)
    private BigDecimal bboxMinLon;

    @Column(name = "bbox_max_lon", nullable = false, precision = 10, scale = 7)
    private BigDecimal bboxMaxLon;

    @Column(name = "center_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal centerLat;

    @Column(name = "center_lon", nullable = false, precision = 10, scale = 7)
    private BigDecimal centerLon;

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
