package com.esthetiadmin.backend.catalog.service;

import com.esthetiadmin.backend.catalog.dto.CategoryRequest;
import com.esthetiadmin.backend.catalog.dto.CategoryResponse;
import com.esthetiadmin.backend.catalog.entity.Category;
import com.esthetiadmin.backend.catalog.entity.Offering;
import com.esthetiadmin.backend.catalog.repository.CategoryRepository;
import com.esthetiadmin.backend.catalog.repository.OfferingRepository;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    private CategoryRepository categoryRepository;
    private OfferingRepository offeringRepository;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        offeringRepository = mock(OfferingRepository.class);
        service = new CategoryService(categoryRepository, offeringRepository);
    }

    @Test
    void findAllGroupsTheServicesUnderTheirCategory() {
        Category lasers = category(1L, "Depilación láser");
        Category facials = category(2L, "Faciales");
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(lasers, facials));
        when(offeringRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                offering(10L, 1L, "Axilas"), offering(11L, 1L, "Bozo")));

        List<CategoryResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).services()).extracting("name").containsExactly("Axilas", "Bozo");
        assertThat(result.get(1).services()).isEmpty();
    }

    @Test
    void createTrimsTheNameAndSavesIt() {
        when(categoryRepository.existsByName("Masajes")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        CategoryResponse response = service.create(new CategoryRequest("  Masajes  ", null));

        assertThat(response.name()).isEqualTo("Masajes");
        assertThat(response.active()).isTrue();
        assertThat(response.services()).isEmpty();
        assertThat(response.appointmentMinutes()).isEqualTo(60);
    }

    @Test
    void createKeepsTheChosenAppointmentLength() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        CategoryResponse response = service.create(new CategoryRequest("Depilación", 30));

        assertThat(response.appointmentMinutes()).isEqualTo(30);
    }

    @Test
    void createRejectsAnAppointmentLengthOutsideTheGrid() {
        assertThatThrownBy(() -> service.create(new CategoryRequest("Masajes", 45)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("La duración del turno tiene que ser de 30 o 60 minutos");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createRejectsARepeatedName() {
        when(categoryRepository.existsByName("Masajes")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryRequest("Masajes", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe una categoría con ese nombre");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateChangesTheName() {
        Category category = category(1L, "Masaje");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Masajes", 1L)).thenReturn(false);
        when(offeringRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        CategoryResponse response = service.update(1L, new CategoryRequest("Masajes", null));

        assertThat(response.name()).isEqualTo("Masajes");
    }

    @Test
    void updateChangesTheAppointmentLength() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Depilación")));
        when(offeringRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        CategoryResponse response = service.update(1L, new CategoryRequest("Depilación", 30));

        assertThat(response.appointmentMinutes()).isEqualTo(30);
    }

    @Test
    void updateWithoutAppointmentLengthKeepsTheCurrentOne() {
        Category category = category(1L, "Depilación");
        category.update("Depilación", 30);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(offeringRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        CategoryResponse response = service.update(1L, new CategoryRequest("Depilación", null));

        assertThat(response.appointmentMinutes()).isEqualTo(30);
    }

    @Test
    void updateRejectsTheNameOfAnotherCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Masaje")));
        when(categoryRepository.existsByNameAndIdNot("Faciales", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new CategoryRequest("Faciales", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateFailsWhenTheCategoryDoesNotExist() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9L, new CategoryRequest("Masajes", null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("La categoría no existe");
    }

    @Test
    void setActiveTurnsTheCategoryOffAndOn() {
        Category category = category(1L, "Faciales");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(offeringRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        assertThat(service.setActive(1L, false).active()).isFalse();
        assertThat(service.setActive(1L, true).active()).isTrue();
    }

    @Test
    void deleteRemovesAnEmptyCategory() {
        Category category = category(1L, "Vacía");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(offeringRepository.existsByCategoryId(1L)).thenReturn(false);

        service.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteRejectsACategoryWithServices() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Faciales")));
        when(offeringRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("tiene servicios");
        verify(categoryRepository, never()).delete(any());
    }

    private Category category(Long id, String name) {
        Category category = new Category(name, Category.DEFAULT_APPOINTMENT_MINUTES);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Offering offering(Long id, Long categoryId, String name) {
        Offering offering = new Offering(categoryId, name, null, new BigDecimal("15000.00"), true);
        ReflectionTestUtils.setField(offering, "id", id);
        return offering;
    }
}
