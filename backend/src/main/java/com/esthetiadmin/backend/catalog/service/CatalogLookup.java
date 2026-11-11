package com.esthetiadmin.backend.catalog.service;

import com.esthetiadmin.backend.catalog.dto.BookableCategory;
import com.esthetiadmin.backend.catalog.dto.BookableService;
import com.esthetiadmin.backend.catalog.dto.OfferingInfo;
import com.esthetiadmin.backend.catalog.entity.Category;
import com.esthetiadmin.backend.catalog.entity.Offering;
import com.esthetiadmin.backend.catalog.repository.CategoryRepository;
import com.esthetiadmin.backend.catalog.repository.OfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The read-only door of the catalog for the rest of the modules, which must not reach its repositories. */
@Service
@RequiredArgsConstructor
public class CatalogLookup {

    private final OfferingRepository offeringRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Optional<OfferingInfo> findOffering(Long id) {
        return Optional.ofNullable(findOfferings(List.of(id)).get(id));
    }

    @Transactional(readOnly = true)
    public Map<Long, OfferingInfo> findOfferings(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Offering> offerings = offeringRepository.findAllById(ids);
        Map<Long, Category> categories = categoryRepository
                .findAllById(offerings.stream().map(Offering::getCategoryId).distinct().toList()).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        return offerings.stream().collect(Collectors.toMap(Offering::getId, offering -> {
            Category category = categories.get(offering.getCategoryId());
            return new OfferingInfo(offering.getId(), offering.getName(), category.getId(), category.getName(),
                    offering.isActive() && category.isActive(), offering.isBookable(), category.getAppointmentMinutes());
        }));
    }

    /** What the public page offers: active, bookable services of active categories, by category, without prices. */
    @Transactional(readOnly = true)
    public List<BookableCategory> bookableCatalog() {
        Map<Long, List<BookableService>> servicesByCategory = offeringRepository.findAllByOrderByNameAsc().stream()
                .filter(offering -> offering.isActive() && offering.isBookable())
                .collect(Collectors.groupingBy(Offering::getCategoryId, Collectors.mapping(
                        offering -> new BookableService(offering.getId(), offering.getName(), offering.getDescription()),
                        Collectors.toList())));
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .filter(category -> category.isActive() && servicesByCategory.containsKey(category.getId()))
                .map(category -> new BookableCategory(category.getId(), category.getName(),
                        category.getAppointmentMinutes(), servicesByCategory.get(category.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> offeringIdsInCategory(Long categoryId) {
        return offeringRepository.findAllByCategoryId(categoryId).stream().map(Offering::getId).toList();
    }
}
