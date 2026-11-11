package com.esthetiadmin.backend.treatment.service;

import com.esthetiadmin.backend.catalog.dto.OfferingInfo;
import com.esthetiadmin.backend.catalog.service.CatalogLookup;
import com.esthetiadmin.backend.client.service.ClientLookup;
import com.esthetiadmin.backend.common.dto.PageResponse;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import com.esthetiadmin.backend.treatment.dto.TreatmentRequest;
import com.esthetiadmin.backend.treatment.dto.TreatmentResponse;
import com.esthetiadmin.backend.treatment.dto.TreatmentUpdateRequest;
import com.esthetiadmin.backend.treatment.entity.Treatment;
import com.esthetiadmin.backend.treatment.repository.TreatmentRepository;
import com.esthetiadmin.backend.treatment.repository.TreatmentSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TreatmentService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("performedOn"), Sort.Order.desc("id"));
    private static final String UNKNOWN = "(desconocido)";

    private final TreatmentRepository treatmentRepository;
    private final CatalogLookup catalogLookup;
    private final ClientLookup clientLookup;

    @Transactional
    public TreatmentResponse create(TreatmentRequest request) {
        if (!clientLookup.exists(request.clientId())) {
            throw new NotFoundException("El cliente no existe");
        }
        ensureOfferable(request.serviceId());
        Treatment saved = treatmentRepository.save(new Treatment(request.clientId(), request.serviceId(),
                request.performedOn(), request.amount(), cleanNotes(request.notes())));
        return toResponses(List.of(saved)).get(0);
    }

    @Transactional
    public TreatmentResponse update(Long id, TreatmentUpdateRequest request) {
        Treatment treatment = getTreatment(id);
        // Keeping its own service is always fine, even if that service was turned off after this treatment.
        if (!treatment.getServiceId().equals(request.serviceId())) {
            ensureOfferable(request.serviceId());
        }
        treatment.update(request.serviceId(), request.performedOn(), request.amount(), cleanNotes(request.notes()));
        return toResponses(List.of(treatment)).get(0);
    }

    @Transactional
    public void delete(Long id) {
        treatmentRepository.delete(getTreatment(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TreatmentResponse> search(Long clientId, Long serviceId, Long categoryId,
                                                  LocalDate from, LocalDate to, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\"");
        }
        Collection<Long> serviceIds = serviceFilter(serviceId, categoryId);
        if (serviceIds != null && serviceIds.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        Page<Treatment> result = treatmentRepository.findAll(
                TreatmentSpecifications.matching(clientId, serviceIds, from, to),
                PageRequest.of(page, size, NEWEST_FIRST));
        return new PageResponse<>(toResponses(result.getContent()), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** {@code null} means no restriction; an empty collection means nothing can match. */
    private Collection<Long> serviceFilter(Long serviceId, Long categoryId) {
        if (categoryId == null) {
            return serviceId == null ? null : List.of(serviceId);
        }
        List<Long> inCategory = catalogLookup.offeringIdsInCategory(categoryId);
        if (serviceId == null) {
            return inCategory;
        }
        return inCategory.contains(serviceId) ? List.of(serviceId) : List.of();
    }

    private void ensureOfferable(Long serviceId) {
        OfferingInfo offering = catalogLookup.findOffering(serviceId)
                .orElseThrow(() -> new NotFoundException("El servicio no existe"));
        if (!offering.offerable()) {
            throw new ConflictException(
                    "El servicio no está disponible: está desactivado o su categoría está inactiva");
        }
    }

    private Treatment getTreatment(Long id) {
        return treatmentRepository.findById(id).orElseThrow(() -> new NotFoundException("El tratamiento no existe"));
    }

    private List<TreatmentResponse> toResponses(List<Treatment> treatments) {
        Map<Long, OfferingInfo> offerings = catalogLookup.findOfferings(
                treatments.stream().map(Treatment::getServiceId).distinct().toList());
        Map<Long, String> clientNames = clientLookup.namesByIds(
                treatments.stream().map(Treatment::getClientId).distinct().toList());
        return treatments.stream().map(treatment -> {
            OfferingInfo offering = offerings.get(treatment.getServiceId());
            return new TreatmentResponse(
                    treatment.getId(),
                    treatment.getClientId(),
                    clientNames.getOrDefault(treatment.getClientId(), UNKNOWN),
                    treatment.getServiceId(),
                    offering != null ? offering.name() : UNKNOWN,
                    offering != null ? offering.categoryId() : null,
                    offering != null ? offering.categoryName() : UNKNOWN,
                    treatment.getPerformedOn(),
                    treatment.getAmount(),
                    treatment.getNotes());
        }).toList();
    }

    private String cleanNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }
}
