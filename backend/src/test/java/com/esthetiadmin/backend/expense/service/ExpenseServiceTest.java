package com.esthetiadmin.backend.expense.service;

import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import com.esthetiadmin.backend.expense.dto.ExpenseRequest;
import com.esthetiadmin.backend.expense.entity.Expense;
import com.esthetiadmin.backend.expense.entity.ExpenseType;
import com.esthetiadmin.backend.expense.repository.ExpenseRepository;
import com.esthetiadmin.backend.expense.repository.ExpenseTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    private ExpenseRepository expenseRepository;
    private ExpenseTypeRepository expenseTypeRepository;
    private ExpenseService service;

    @BeforeEach
    void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        expenseTypeRepository = mock(ExpenseTypeRepository.class);
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ExpenseService(expenseRepository, expenseTypeRepository, clock);
    }

    @Test
    void createTrimsTheDescriptionAndReturnsTheTypeName() {
        ExpenseType type = type(3L, "Insumos");
        when(expenseTypeRepository.findById(3L)).thenReturn(Optional.of(type));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(call -> {
            Expense expense = call.getArgument(0);
            ReflectionTestUtils.setField(expense, "id", 8L);
            return expense;
        });

        var response = service.create(request(TODAY, "  Algodón  ", "1250.50", 3L));

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.description()).isEqualTo("Algodón");
        assertThat(response.amount()).isEqualByComparingTo("1250.50");
        assertThat(response.expenseTypeName()).isEqualTo("Insumos");
    }

    @Test
    void createRejectsAFutureDate() {
        when(expenseTypeRepository.findById(3L)).thenReturn(Optional.of(type(3L, "Insumos")));

        assertThatThrownBy(() -> service.create(request(TODAY.plusDays(1), "Algodón", "100", 3L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("La fecha no puede ser futura");
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createRejectsAnUnknownType() {
        when(expenseTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(TODAY, "Algodón", "100", 99L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("El tipo de gasto no existe");
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchReturnsNewestFirstDataAndThePeriodTotal() {
        ExpenseType supplies = type(3L, "Insumos");
        Expense older = expense(1L, 3L, LocalDate.of(2026, 9, 1), "Gasas", "100.25");
        Expense newer = expense(2L, 3L, LocalDate.of(2026, 9, 20), "Cremas", "250.75");
        when(expenseRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(newer, older));
        when(expenseTypeRepository.findAllById(any())).thenReturn(List.of(supplies));

        var response = service.search(LocalDate.of(2026, 9, 1), TODAY, null);

        assertThat(response.content()).extracting(item -> item.id()).containsExactly(2L, 1L);
        assertThat(response.totalAmount()).isEqualByComparingTo("351.00");
        verify(expenseRepository).findAll(any(Specification.class), eq(Sort.by(
                Sort.Order.desc("spentOn"), Sort.Order.desc("id"))));
    }

    @Test
    void searchRejectsAnInvertedDateRange() {
        assertThatThrownBy(() -> service.search(TODAY, TODAY.minusDays(1), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desde");
    }

    @Test
    void searchRejectsAnUnknownType() {
        when(expenseTypeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.search(null, null, 99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("El tipo de gasto no existe");
    }

    @Test
    void updateChangesEveryEditableField() {
        Expense expense = expense(8L, 3L, TODAY.minusDays(1), "Gasas", "100");
        ExpenseType services = type(2L, "Servicios");
        when(expenseRepository.findById(8L)).thenReturn(Optional.of(expense));
        when(expenseTypeRepository.findById(2L)).thenReturn(Optional.of(services));

        var response = service.update(8L, request(TODAY, "  Internet  ", "4500", 2L));

        assertThat(response.description()).isEqualTo("Internet");
        assertThat(response.expenseTypeId()).isEqualTo(2L);
        assertThat(response.expenseTypeName()).isEqualTo("Servicios");
        assertThat(response.amount()).isEqualByComparingTo("4500");
    }

    @Test
    void deleteRemovesTheExpense() {
        Expense expense = expense(8L, 3L, TODAY, "Gasas", "100");
        when(expenseRepository.findById(8L)).thenReturn(Optional.of(expense));

        service.delete(8L);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void listTypesReturnsAlphabeticalRepositoryResult() {
        when(expenseTypeRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(type(1L, "Alquiler"), type(3L, "Insumos")));

        assertThat(service.listTypes()).extracting(item -> item.name())
                .containsExactly("Alquiler", "Insumos");
    }

    private ExpenseRequest request(LocalDate spentOn, String description, String amount, Long typeId) {
        return new ExpenseRequest(spentOn, description, new BigDecimal(amount), typeId);
    }

    private Expense expense(Long id, Long typeId, LocalDate spentOn, String description, String amount) {
        Expense expense = new Expense(typeId, spentOn, description, new BigDecimal(amount));
        ReflectionTestUtils.setField(expense, "id", id);
        return expense;
    }

    private ExpenseType type(Long id, String name) {
        ExpenseType type = new ExpenseType(name);
        ReflectionTestUtils.setField(type, "id", id);
        return type;
    }
}
