package com.esthetiadmin.backend.catalog.service;

import com.esthetiadmin.backend.catalog.dto.CategoryRequest;
import com.esthetiadmin.backend.catalog.dto.CategoryResponse;
import com.esthetiadmin.backend.catalog.dto.OfferingResponse;
import com.esthetiadmin.backend.catalog.entity.Category;
import com.esthetiadmin.backend.catalog.entity.Offering;
import com.esthetiadmin.backend.catalog.repository.CategoryRepository;
import com.esthetiadmin.backend.catalog.repository.OfferingRepository;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final OfferingRepository offeringRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        Map<Long, List<OfferingResponse>> offeringsByCategory = offeringRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.groupingBy(Offering::getCategoryId,
                        Collectors.mapping(OfferingResponse::from, Collectors.toList())));
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> CategoryResponse.from(category,
                        offeringsByCategory.getOrDefault(category.getId(), List.of())))
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new ConflictException("Ya existe una categoría con ese nombre");
        }
        int minutes = appointmentMinutes(request.appointmentMinutes(), Category.DEFAULT_APPOINTMENT_MINUTES);
        Category saved = categoryRepository.save(new Category(name, minutes));
        return CategoryResponse.from(saved, List.of());
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("Ya existe una categoría con ese nombre");
        }
        category.update(name, appointmentMinutes(request.appointmentMinutes(), category.getAppointmentMinutes()));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse setActive(Long id, boolean active) {
        Category category = getCategory(id);
        category.changeActive(active);
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        if (offeringRepository.existsByCategoryId(id)) {
            throw new ConflictException(
                    "La categoría tiene servicios. Trasladalos a otra categoría o desactivala");
        }
        categoryRepository.delete(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("La categoría no existe"));
    }

    /** A missing duration keeps {@code fallback}: 60 for a new category, the current one when modifying. */
    private int appointmentMinutes(Integer requested, int fallback) {
        if (requested == null) {
            return fallback;
        }
        if (!Category.APPOINTMENT_DURATIONS.contains(requested)) {
            throw new BadRequestException("La duración del turno tiene que ser de 30 o 60 minutos");
        }
        return requested;
    }

    private CategoryResponse toResponse(Category category) {
        List<OfferingResponse> offerings = offeringRepository.findAllByOrderByNameAsc().stream()
                .filter(offering -> offering.getCategoryId().equals(category.getId()))
                .map(OfferingResponse::from)
                .toList();
        return CategoryResponse.from(category, offerings);
    }
}
