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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TreatmentServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 25);
    private static final OfferingInfo BOZO = new OfferingInfo(10L, "Bozo", 1L, "Depilación láser", true, true, 30);

    private TreatmentRepository repository;
    private CatalogLookup catalog;
    private ClientLookup clients;
    private TreatmentService service;

    @BeforeEach
    void setUp() {
        repository = mock(TreatmentRepository.class);
        catalog = mock(CatalogLookup.class);
        clients = mock(ClientLookup.class);
        service = new TreatmentService(repository, catalog, clients);

        when(clients.exists(7L)).thenReturn(true);
        when(clients.namesByIds(any())).thenReturn(Map.of(7L, "Diaz, Carina"));
        when(catalog.findOffering(10L)).thenReturn(Optional.of(BOZO));
        when(catalog.findOfferings(any())).thenReturn(Map.of(10L, BOZO));
        when(repository.save(any(Treatment.class))).thenAnswer(call -> {
            Treatment treatment = call.getArgument(0);
            ReflectionTestUtils.setField(treatment, "id", 31L);
            return treatment;
        });
    }

    private TreatmentRequest request(Long clientId, Long serviceId, String amount, String notes) {
        return new TreatmentRequest(clientId, serviceId, DAY, new BigDecimal(amount), notes);
    }

    private Treatment treatment(Long id, Long serviceId) {
        Treatment treatment = new Treatment(7L, serviceId, DAY, new BigDecimal("15000.00"), null);
        ReflectionTestUtils.setField(treatment, "id", id);
        return treatment;
    }

    @Test
    void createSavesTheTreatmentAndReturnsItWithNames() {
        TreatmentResponse response = service.create(request(7L, 10L, "15000.00", "  Sin reacciones "));

        assertThat(response.id()).isEqualTo(31L);
        assertThat(response.clientName()).isEqualTo("Diaz, Carina");
        assertThat(response.serviceName()).isEqualTo("Bozo");
        assertThat(response.categoryName()).isEqualTo("Depilación láser");
        assertThat(response.amount()).isEqualByComparingTo("15000.00");
        assertThat(response.notes()).isEqualTo("Sin reacciones");
    }

    @Test
    void createAcceptsAFreeTreatment() {
        TreatmentResponse response = service.create(request(7L, 10L, "0", null));

        assertThat(response.amount()).isEqualByComparingTo("0");
    }

    @Test
    void createStoresBlankNotesAsNothing() {
        assertThat(service.create(request(7L, 10L, "100", "   ")).notes()).isNull();
    }

    @Test
    void createFailsWhenTheClientDoesNotExist() {
        when(clients.exists(9L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request(9L, 10L, "100", null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("El cliente no existe");
        verify(repository, never()).save(any());
    }

    @Test
    void createFailsWhenTheServiceDoesNotExist() {
        when(catalog.findOffering(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(7L, 99L, "100", null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("El servicio no existe");
    }

    @Test
    void createRejectsAServiceThatIsNotOfferedAnymore() {
        when(catalog.findOffering(11L)).thenReturn(Optional.of(new OfferingInfo(11L, "Axilas", 1L, "Depilación láser", false, true, 30)));

        assertThatThrownBy(() -> service.create(request(7L, 11L, "100", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no está disponible");
        verify(repository, never()).save(any());
    }

    @Test
    void updateChangesTheDataButNeverTheClient() {
        Treatment existing = treatment(31L, 10L);
        when(repository.findById(31L)).thenReturn(Optional.of(existing));

        TreatmentResponse response = service.update(31L,
                new TreatmentUpdateRequest(10L, DAY.minusDays(1), new BigDecimal("12000.00"), "Con descuento"));

        assertThat(response.clientId()).isEqualTo(7L);
        assertThat(response.performedOn()).isEqualTo(DAY.minusDays(1));
        assertThat(response.amount()).isEqualByComparingTo("12000.00");
        assertThat(response.notes()).isEqualTo("Con descuento");
    }

    @Test
    void updateKeepsAServiceThatWasTurnedOffAfterTheTreatment() {
        Treatment existing = treatment(31L, 11L);
        when(repository.findById(31L)).thenReturn(Optional.of(existing));
        when(catalog.findOfferings(any())).thenReturn(Map.of(11L, new OfferingInfo(11L, "Axilas", 1L, "Depilación láser", false, true, 30)));

        TreatmentResponse response = service.update(31L, new TreatmentUpdateRequest(11L, DAY, new BigDecimal("500"), null));

        assertThat(response.serviceName()).isEqualTo("Axilas");
        verify(catalog, never()).findOffering(11L);
    }

    @Test
    void updateRejectsChangingToAServiceThatIsNotOffered() {
        when(repository.findById(31L)).thenReturn(Optional.of(treatment(31L, 10L)));
        when(catalog.findOffering(11L)).thenReturn(Optional.of(new OfferingInfo(11L, "Axilas", 1L, "Depilación láser", false, true, 30)));

        assertThatThrownBy(() -> service.update(31L, new TreatmentUpdateRequest(11L, DAY, new BigDecimal("500"), null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateAndDeleteFailWhenTheTreatmentDoesNotExist() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9L, new TreatmentUpdateRequest(10L, DAY, BigDecimal.ONE, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("El tratamiento no existe");
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesTheTreatment() {
        Treatment existing = treatment(31L, 10L);
        when(repository.findById(31L)).thenReturn(Optional.of(existing));

        service.delete(31L);

        verify(repository).delete(existing);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchReturnsAPageWithNamesAndKeepsTheServerPaging() {
        Page<Treatment> page = new PageImpl<>(List.of(treatment(31L, 10L)), PageRequest.of(0, 20), 41);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<TreatmentResponse> result = service.search(7L, null, null, null, null, 0, 20);

        assertThat(result.content()).extracting(TreatmentResponse::serviceName).containsExactly("Bozo");
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchByCategoryAnswersEmptyWithoutQueryingWhenTheCategoryHasNoServices() {
        when(catalog.offeringIdsInCategory(5L)).thenReturn(List.of());

        PageResponse<TreatmentResponse> result = service.search(null, null, 5L, null, null, 0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchByServiceOutsideTheChosenCategoryFindsNothing() {
        when(catalog.offeringIdsInCategory(1L)).thenReturn(List.of(10L, 11L));

        PageResponse<TreatmentResponse> result = service.search(null, 99L, 1L, null, null, 0, 20);

        assertThat(result.content()).isEmpty();
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchRejectsAnInvertedDateRange() {
        assertThatThrownBy(() -> service.search(null, null, null, DAY, DAY.minusDays(1), 0, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no puede ser posterior");
    }
}
