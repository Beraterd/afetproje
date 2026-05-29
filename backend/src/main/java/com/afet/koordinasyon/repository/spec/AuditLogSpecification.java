package com.afet.koordinasyon.repository.spec;

import com.afet.koordinasyon.domain.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AuditLogSpecification {

    private AuditLogSpecification() {}

    public static Specification<AuditLog> filter(
            String actionType,
            UUID actorId,
            String actorRole,
            String entityType,
            UUID entityId,
            Boolean isSystemAction,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actionType != null && !actionType.isBlank()) {
                predicates.add(cb.equal(root.get("action"), actionType.toUpperCase()));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (actorRole != null && !actorRole.isBlank()) {
                predicates.add(cb.equal(root.get("actorRole"), actorRole.toUpperCase()));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (isSystemAction != null) {
                predicates.add(cb.equal(root.get("isSystemAction"), isSystemAction));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorName")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("action")), pattern)
                ));
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
