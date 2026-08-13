package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.enums.EarthquakeSource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Hotfix — {@code GET /api/earthquakes} 500 hatası: eski JPQL "(:param IS NULL OR ...)"
 * deseni PostgreSQL'de timestamptz parametreleri null geçildiğinde tip çıkaramıyordu
 * ("could not determine data type of parameter $9"). Bu test, Criteria API tabanlı yeni
 * çözümün NULL filtreler için hiç predicate/parametre üretmediğini (bu yüzden sorunun kökten
 * ortadan kalktığını) doğrudan doğrular — gerçek DB gerekmez.
 */
@SuppressWarnings("unchecked")
class EarthquakeEventSpecificationTest {

    private Root<EarthquakeEvent> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;
    private Predicate conjunction;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        conjunction = mock(Predicate.class);

        Path<Object> anyPath = mock(Path.class);
        when(root.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(anyPath);
        when(cb.and(any(Predicate[].class))).thenReturn(conjunction);
        when(cb.desc(any())).thenReturn(mock(jakarta.persistence.criteria.Order.class));
    }

    @Test
    void allFiltersNull_buildsNoPredicates_noDateComparisonAttempted() {
        Specification<EarthquakeEvent> spec = EarthquakeEventSpecification.withFilters(
                null, null, null, null, null, null);

        Predicate result = spec.toPredicate(root, query, cb);

        // Kritik doğrulama: startDate/endDate null iken cb.greaterThanOrEqualTo/lessThanOrEqualTo
        // HİÇ çağrılmıyor — eski koddaki "? IS NULL" parametre bind'i tamamen ortadan kalktı.
        verify(cb, never()).greaterThanOrEqualTo(any(), any(Comparable.class));
        verify(cb, never()).lessThanOrEqualTo(any(), any(Comparable.class));
        verify(cb, never()).equal(any(), any());
        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void onlySourceFilter_producesExactlyOneEqualPredicate() {
        Specification<EarthquakeEvent> spec = EarthquakeEventSpecification.withFilters(
                EarthquakeSource.KANDILLI, null, null, null, null, null);

        spec.toPredicate(root, query, cb);

        verify(cb, org.mockito.Mockito.times(1)).equal(any(), eq(EarthquakeSource.KANDILLI));
        verify(cb, never()).greaterThanOrEqualTo(any(), any(Comparable.class));
        verify(cb, never()).lessThanOrEqualTo(any(), any(Comparable.class));
    }

    @Test
    void onlyStartDate_producesExactlyOneGreaterThanOrEqualPredicate_noNullCheckParam() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-08T00:00:00Z");
        Specification<EarthquakeEvent> spec = EarthquakeEventSpecification.withFilters(
                null, null, null, null, start, null);

        spec.toPredicate(root, query, cb);

        verify(cb, org.mockito.Mockito.times(1)).greaterThanOrEqualTo(any(), eq(start));
        verify(cb, never()).lessThanOrEqualTo(any(), any(Comparable.class));
        verify(cb, never()).equal(any(), any());
    }

    @Test
    void startAndEndDateBothPresent_producesBothPredicates() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-08-08T23:59:59Z");
        Specification<EarthquakeEvent> spec = EarthquakeEventSpecification.withFilters(
                null, null, null, null, start, end);

        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(any(), eq(start));
        verify(cb).lessThanOrEqualTo(any(), eq(end));
    }

    @Test
    void allFiltersPresent_producesAllSixPredicates() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-08-08T23:59:59Z");
        Specification<EarthquakeEvent> spec = EarthquakeEventSpecification.withFilters(
                EarthquakeSource.AFAD, "ISTANBUL", "KARTAL", 3.0, start, end);

        spec.toPredicate(root, query, cb);

        // NOT: any() ile equal(Expression,Object) çağrısını doğrulamak CriteriaBuilder'ın
        // equal(Expression,Expression) overload'ıyla belirsizliğe (Mockito yanlış overload'ı
        // doğrular, "wanted but not invoked" verir) yol açtığından somut eq(...) değerleri kullanılır.
        verify(cb).equal(any(), eq(EarthquakeSource.AFAD));
        verify(cb).equal(any(), eq("ISTANBUL"));
        verify(cb).equal(any(), eq("KARTAL"));
        verify(cb).greaterThanOrEqualTo(any(), eq(3.0));
        verify(cb).greaterThanOrEqualTo(any(), eq(start));
        verify(cb).lessThanOrEqualTo(any(), eq(end));
    }
}
