package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.Event;
import com.afet.koordinasyon.domain.enums.EventStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventSpecification {

    private EventSpecification() {}

    public static Specification<Event> withFilters(EventStatus status, UUID districtId, UUID neighborhoodId, UUID teamId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (districtId != null) {
                predicates.add(cb.equal(root.get("neighborhood").get("district").get("id"), districtId));
            }
            if (neighborhoodId != null) {
                predicates.add(cb.equal(root.get("neighborhood").get("id"), neighborhoodId));
            }
            if (teamId != null) {
                predicates.add(cb.equal(root.get("team").get("id"), teamId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
