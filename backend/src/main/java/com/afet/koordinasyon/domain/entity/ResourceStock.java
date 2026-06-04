package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bölgesel (ilçe/mahalle) kaynak/ekipman stok kaydı.
 * status alanı kalıcı tutulmaz; servis katmanında hesaplanır.
 */
@Entity
@Table(name = "resource_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    /** Kaynak/ekipman kategorisi — talep türleriyle eşleşir (warning eşleştirmesi için). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ResourceType category;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 0;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String unit = "adet";

    /** Günlük tahmini tüketim. null ise yeterlilik gün hesabı yapılamaz. */
    @Column(name = "daily_usage_estimate")
    private Double dailyUsageEstimate;

    @Column(name = "critical_threshold", nullable = false)
    @Builder.Default
    private int criticalThreshold = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id")
    private Neighborhood neighborhood;

    @Column(name = "warehouse_name", length = 200)
    private String warehouseName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;

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
