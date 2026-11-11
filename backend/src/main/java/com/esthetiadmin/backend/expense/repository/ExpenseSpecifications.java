package com.esthetiadmin.backend.expense.repository;

import com.esthetiadmin.backend.expense.entity.Expense;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<Expense> matching(LocalDate from, LocalDate to, Long expenseTypeId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("spentOn"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("spentOn"), to));
            }
            if (expenseTypeId != null) {
                predicates.add(builder.equal(root.get("expenseTypeId"), expenseTypeId));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
