package com.esthetiadmin.backend.treatment.repository;

import com.esthetiadmin.backend.treatment.entity.Treatment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TreatmentSpecifications {

    private TreatmentSpecifications() {
    }

    /**
     * Every filter is optional. {@code serviceIds} must not be an empty collection: callers answer that case
     * themselves, because "in ()" is not valid SQL. Both dates are inclusive.
     */
    public static Specification<Treatment> matching(Long clientId, Collection<Long> serviceIds,
                                                    LocalDate from, LocalDate to) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (clientId != null) {
                predicates.add(builder.equal(root.get("clientId"), clientId));
            }
            if (serviceIds != null) {
                predicates.add(root.get("serviceId").in(serviceIds));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("performedOn"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("performedOn"), to));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
