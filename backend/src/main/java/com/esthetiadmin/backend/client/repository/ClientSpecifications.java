package com.esthetiadmin.backend.client.repository;

import com.esthetiadmin.backend.client.entity.Client;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;

public final class ClientSpecifications {

    private static final char ESCAPE = '!';

    private ClientSpecifications() {
    }

    /**
     * Every word of the search must appear in the first or the last name. Case and accents are ignored
     * because the column collation already ignores them.
     */
    public static Specification<Client> matching(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }
        String[] terms = search.trim().split("\\s+");
        return (root, query, builder) -> builder.and(Arrays.stream(terms)
                .map(term -> {
                    String pattern = "%" + escape(term) + "%";
                    return builder.or(
                            builder.like(root.get("firstName"), pattern, ESCAPE),
                            builder.like(root.get("lastName"), pattern, ESCAPE));
                })
                .toArray(Predicate[]::new));
    }

    private static String escape(String term) {
        return term.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
