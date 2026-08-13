package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.enums.EarthquakeSource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Item 45 hotfix — {@code GET /api/earthquakes} filtreleri artık JPQL'deki
 * {@code (:param IS NULL OR field >= :param)} deseni yerine Criteria API ile dinamik
 * olarak kuruluyor. Yalnızca GERÇEKTEN verilen filtreler için predicate eklenir; boş
 * filtre için hiçbir parametre sorguya bağlanmaz.
 * <p>
 * Kök neden: PostgreSQL, {@code ? IS NULL} biçimindeki bir parametreyi (özellikle
 * {@code timestamptz} gibi doğal bir "unknown" varsayılanı olmayan tipler için) statement
 * parse aşamasında tip çıkarımı yapamadığından reddediyordu
 * ("could not determine data type of parameter $9") — startDate/endDate NULL geçildiğinde
 * (örn. yalnızca {@code source} filtresiyle çağrıldığında) 500 ile patlıyordu. Criteria API
 * bu deseni hiç üretmediği için sorun kökten ortadan kalkıyor.
 */
public final class EarthquakeEventSpecification {

    private EarthquakeEventSpecification() {
    }

    public static Specification<EarthquakeEvent> withFilters(
            EarthquakeSource source,
            String provinceNormalized,
            String districtNormalized,
            Double minMagnitude,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (provinceNormalized != null) {
                predicates.add(cb.equal(root.get("provinceNormalized"), provinceNormalized));
            }
            if (districtNormalized != null) {
                predicates.add(cb.equal(root.get("districtNormalized"), districtNormalized));
            }
            if (minMagnitude != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("magnitude"), minMagnitude));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventTime"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventTime"), endDate));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("eventTime")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
