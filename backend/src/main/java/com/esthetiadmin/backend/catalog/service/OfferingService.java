package com.esthetiadmin.backend.catalog.service;

import com.esthetiadmin.backend.catalog.dto.OfferingRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingResponse;
import com.esthetiadmin.backend.catalog.entity.Offering;
import com.esthetiadmin.backend.catalog.repository.CategoryRepository;
import com.esthetiadmin.backend.catalog.repository.OfferingRepository;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfferingService {

    private static final String DUPLICATE_NAME = "Ya existe un servicio con ese nombre en esta categoría";

    private final OfferingRepository offeringRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public OfferingResponse create(OfferingRequest request) {
        ensureCategoryExists(request.categoryId());
        String name = request.name().trim();
        if (offeringRepository.existsByCategoryIdAndName(request.categoryId(), name)) {
            throw new ConflictException(DUPLICATE_NAME);
        }
        Offering saved = offeringRepository.save(
                new Offering(request.categoryId(), name, cleanDescription(request.description()), request.price(),
                        request.bookable() == null || request.bookable()));
        return OfferingResponse.from(saved);
    }

    @Transactional
    public OfferingResponse update(Long id, OfferingRequest request) {
        Offering offering = getOffering(id);
        ensureCategoryExists(request.categoryId());
        String name = request.name().trim();
        if (offeringRepository.existsByCategoryIdAndNameAndIdNot(request.categoryId(), name, id)) {
            throw new ConflictException(DUPLICATE_NAME);
        }
        boolean bookable = request.bookable() == null ? offering.isBookable() : request.bookable();
        offering.update(request.categoryId(), name, cleanDescription(request.description()), request.price(), bookable);
        return OfferingResponse.from(offering);
    }

    @Transactional
    public OfferingResponse setActive(Long id, boolean active) {
        Offering offering = getOffering(id);
        offering.changeActive(active);
        return OfferingResponse.from(offering);
    }

    @Transactional
    public void delete(Long id) {
        Offering offering = getOffering(id);
        if (offeringRepository.countTreatments(id) > 0) {
            throw new ConflictException(
                    "El servicio tiene tratamientos registrados. Desactivalo para que deje de ofrecerse");
        }
        if (offeringRepository.countAppointments(id) > 0) {
            throw new ConflictException(
                    "El servicio tiene turnos registrados. Desactivalo para que deje de ofrecerse");
        }
        offeringRepository.delete(offering);
    }

    private Offering getOffering(Long id) {
        return offeringRepository.findById(id).orElseThrow(() -> new NotFoundException("El servicio no existe"));
    }

    private void ensureCategoryExists(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("La categoría no existe");
        }
    }

    /** An empty description is stored as no description. */
    private String cleanDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
