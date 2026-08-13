package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.enums.EarthquakeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EarthquakeEventRepository extends JpaRepository<EarthquakeEvent, UUID>,
        JpaSpecificationExecutor<EarthquakeEvent> {

    /** Provider içi dedup — aynı sağlayıcının aynı kaydı tekrar tekrar DB'ye yazılmaz. */
    boolean existsBySourceAndExternalId(EarthquakeSource source, String externalId);

    // ── Sync yardımcıları ─────────────────────────────────────────────────────

    // Sync başında AFAD sırasını sıfırla; eski AFAD eventleri NULL kalır. Kandilli
    // kayıtları bu alanı hiç kullanmadığından kapsam dışı bırakılır.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EarthquakeEvent e SET e.afadOrderIndex = NULL WHERE e.source = com.afet.koordinasyon.domain.enums.EarthquakeSource.AFAD")
    void resetAllAfadOrderIndices();

    // Mevcut event'e yeni AFAD pozisyonu yaz
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EarthquakeEvent e SET e.afadOrderIndex = :orderIndex WHERE e.source = com.afet.koordinasyon.domain.enums.EarthquakeSource.AFAD AND e.externalId = :externalId")
    void updateAfadOrderIndex(@Param("externalId") String externalId,
                              @Param("orderIndex") int orderIndex);

    // Debug / sync log — eventTime'a göre en yeni tek kayıt
    Optional<EarthquakeEvent> findTopByOrderByEventTimeDescCreatedAtAsc();

    /** Cross-source bildirim dedup adayları — item 17. Küçük bir zaman penceresiyle sınırlıdır. */
    List<EarthquakeEvent> findBySourceNotAndNotificationSentTrueAndEventTimeBetween(
            EarthquakeSource excludedSource, OffsetDateTime from, OffsetDateTime to);

    // ── Listeleme ─────────────────────────────────────────────────────────────

    // /latest → AFAD raw order: afad_order_index ASC, sadece son batch eventleri
    @Query("SELECT e FROM EarthquakeEvent e " +
           "WHERE e.afadOrderIndex IS NOT NULL " +
           "ORDER BY e.afadOrderIndex ASC " +
           "LIMIT 10")
    List<EarthquakeEvent> findLatestByAfadOrder();

    // NOT: Eskiden burada "filtresiz varsayılan" liste için AFAD-order-index'i eventTime'dan
    // önce önceliklendiren bir findAllOrdered() sorgusu vardı. Kandilli eklendikten sonra bu,
    // "Tüm Kaynaklar" görünümünde (source=null) Kandilli kayıtlarının afadOrderIndex'i hep NULL
    // olduğundan gerçek eventTime'dan bağımsız şekilde TÜM AFAD kayıtlarının arkasına
    // düşmesine yol açan bir bug'dı — kullanıcı en yeni Kandilli depremini listede göremiyordu.
    // "Tüm Kaynaklar" dahil TÜM listeleme artık EarthquakeEventSpecification (boş filtrelerde
    // bile) + global eventTime DESC sıralamasıyla yapılıyor (bkz. EarthquakeEventService.listEarthquakes).
    // AFAD'ın kendi ham batch sırası yalnızca /latest ve /debug/status uçlarında
    // (findLatestByAfadOrder) — kasıtlı olarak AFAD'a özel, ayrı bir debug amaçlı görünüm — korunur.

    // Filtreli arama (item 8/9) artık EarthquakeEventSpecification (Criteria API) ile
    // JpaSpecificationExecutor.findAll(spec, pageable) üzerinden yapılıyor — bkz.
    // EarthquakeEventService.listEarthquakes. Eskiden burada "(:param IS NULL OR ...)"
    // JPQL deseni kullanan bir @Query vardı; startDate/endDate NULL geçildiğinde PostgreSQL
    // parametre tipini çıkaramadığı için 500 hatası veriyordu ("could not determine data
    // type of parameter $9") — Criteria API null filtreler için hiç parametre üretmediğinden
    // bu sınıfta artık böyle bir sorgu yok.

    /** İl seçildiğinde ilçe dropdown'unu doldurmak için — yalnızca GERÇEKTEN kayıtlı ilçeler döner, uydurulmaz. */
    @Query("SELECT DISTINCT e.district FROM EarthquakeEvent e " +
           "WHERE e.provinceNormalized = :provinceNormalized AND e.district IS NOT NULL " +
           "ORDER BY e.district")
    List<String> findDistinctDistrictsByProvinceNormalized(@Param("provinceNormalized") String provinceNormalized);
}
