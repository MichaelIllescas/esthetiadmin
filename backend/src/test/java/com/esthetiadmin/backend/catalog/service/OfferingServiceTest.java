package com.esthetiadmin.backend.catalog.service;

import com.esthetiadmin.backend.catalog.dto.OfferingRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingResponse;
import com.esthetiadmin.backend.catalog.entity.Offering;
import com.esthetiadmin.backend.catalog.repository.CategoryRepository;
import com.esthetiadmin.backend.catalog.repository.OfferingRepository;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfferingServiceTest {

    private OfferingRepository offeringRepository;
    private CategoryRepository categoryRepository;
    private OfferingService service;

    @BeforeEach
    void setUp() {
        offeringRepository = mock(OfferingRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        service = new OfferingService(offeringRepository, categoryRepository);
    }

    @Test
    void createSavesTheServiceWithTrimmedNameAndNoBlankDescription() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(offeringRepository.existsByCategoryIdAndName(1L, "Bozo")).thenReturn(false);
        when(offeringRepository.save(any(Offering.class))).thenAnswer(call -> call.getArgument(0));

        OfferingResponse response = service.create(request(1L, "  Bozo ", "   ", "15000.00"));

        assertThat(response.name()).isEqualTo("Bozo");
        assertThat(response.description()).isNull();
        assertThat(response.price()).isEqualByComparingTo("15000.00");
        assertThat(response.active()).isTrue();
        assertThat(response.bookable()).isTrue();
    }

    @Test
    void createCanLeaveTheServiceOffThePublicPage() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(offeringRepository.save(any(Offering.class))).thenAnswer(call -> call.getArgument(0));

        OfferingResponse response = service.create(
                new OfferingRequest(1L, "Bozo", null, new BigDecimal("15000"), false));

        assertThat(response.bookable()).isFalse();
    }

    @Test
    void updateWithoutBookableKeepsTheCurrentValue() {
        Offering offering = offering(5L, 1L, "Bozo");
        offering.update(1L, "Bozo", null, new BigDecimal("15000"), false);
        when(offeringRepository.findById(5L)).thenReturn(Optional.of(offering));
        when(categoryRepository.existsById(1L)).thenReturn(true);

        OfferingResponse response = service.update(5L, request(1L, "Bozo", null, "15000"));

        assertThat(response.bookable()).isFalse();
    }

    @Test
    void updateCanChangeBookable() {
        when(offeringRepository.findById(5L)).thenReturn(Optional.of(offering(5L, 1L, "Bozo")));
        when(categoryRepository.existsById(1L)).thenReturn(true);

        OfferingResponse response = service.update(5L,
                new OfferingRequest(1L, "Bozo", null, new BigDecimal("15000"), false));

        assertThat(response.bookable()).isFalse();
    }

    @Test
    void createFailsWhenTheCategoryDoesNotExist() {
        when(categoryRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request(9L, "Bozo", null, "15000")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("La categoría no existe");
        verify(offeringRepository, never()).save(any());
    }

    @Test
    void createRejectsARepeatedNameInTheSameCategory() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(offeringRepository.existsByCategoryIdAndName(1L, "Bozo")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(1L, "Bozo", null, "15000")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un servicio con ese nombre en esta categoría");
    }

    @Test
    void updateCanMoveTheServiceToAnotherCategoryAndChangeThePrice() {
        Offering offering = offering(5L, 1L, "Bozo");
        when(offeringRepository.findById(5L)).thenReturn(Optional.of(offering));
        when(categoryRepository.existsById(2L)).thenReturn(true);
        when(offeringRepository.existsByCategoryIdAndNameAndIdNot(2L, "Bozo", 5L)).thenReturn(false);

        OfferingResponse response = service.update(5L, request(2L, "Bozo", "Zona del rostro", "18000.00"));

        assertThat(response.categoryId()).isEqualTo(2L);
        assertThat(response.price()).isEqualByComparingTo("18000.00");
        assertThat(response.description()).isEqualTo("Zona del rostro");
    }

    @Test
    void updateRejectsANameAlreadyUsedInTheTargetCategory() {
        when(offeringRepository.findById(5L)).thenReturn(Optional.of(offering(5L, 1L, "Bozo")));
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(offeringRepository.existsByCategoryIdAndNameAndIdNot(1L, "Axilas", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request(1L, "Axilas", null, "15000")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateFailsWhenTheServiceDoesNotExist() {
        when(offeringRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9L, request(1L, "Bozo", null, "15000")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("El servicio no existe");
    }

    @Test
    void setActiveTurnsTheServiceOffAndOn() {
        when(offeringRepository.findById(5L)).thenReturn(Optional.of(offering(5L, 1L, "Bozo")));

        assertThat(service.setActive(5L, false).active()).isFalse();
        assertThat(service.setActive(5L, true).active()).isTrue();
    }

    @Test
    void deleteRemovesAServiceThatWasNeverUsed() {
        Offering offering = offering(5L, 1L, "Bozo");
        when(offeringRepository.findById(5L)).thenReturn(Optional.of(offering));
        when(offeringRepository.countTreatments(5L)).thenReturn(0L);

        service.delete(5L);

        verify(offeringRepository).delete(offering);
    }

    @Test
    void deleteRejectsAServiceThatHasTreatments() {
        when(offeringRepository.findById(5L)).thenReturn(Optional.of(offering(5L, 1L, "Bozo")));
        when(offeringRepository.countTreatments(5L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Desactivalo");
        verify(offeringRepository, never()).delete(any());
    }

    private OfferingRequest request(Long categoryId, String name, String description, String price) {
        return new OfferingRequest(categoryId, name, description, new BigDecimal(price), null);
    }

    private Offering offering(Long id, Long categoryId, String name) {
        Offering offering = new Offering(categoryId, name, null, new BigDecimal("15000.00"), true);
        ReflectionTestUtils.setField(offering, "id", id);
        return offering;
    }
}
