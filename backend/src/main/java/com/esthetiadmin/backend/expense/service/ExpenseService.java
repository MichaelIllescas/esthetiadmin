package com.esthetiadmin.backend.expense.service;

import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import com.esthetiadmin.backend.expense.dto.ExpenseListResponse;
import com.esthetiadmin.backend.expense.dto.ExpenseRequest;
import com.esthetiadmin.backend.expense.dto.ExpenseResponse;
import com.esthetiadmin.backend.expense.dto.ExpenseTypeResponse;
import com.esthetiadmin.backend.expense.entity.Expense;
import com.esthetiadmin.backend.expense.entity.ExpenseType;
import com.esthetiadmin.backend.expense.repository.ExpenseRepository;
import com.esthetiadmin.backend.expense.repository.ExpenseSpecifications;
import com.esthetiadmin.backend.expense.repository.ExpenseTypeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("spentOn"), Sort.Order.desc("id"));
    private static final String UNKNOWN_TYPE = "(desconocido)";

    private final ExpenseRepository expenseRepository;
    private final ExpenseTypeRepository expenseTypeRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ExpenseTypeResponse> listTypes() {
        return expenseTypeRepository.findAllByOrderByNameAsc().stream()
                .map(type -> new ExpenseTypeResponse(type.getId(), type.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseListResponse search(LocalDate from, LocalDate to, Long expenseTypeId) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\"");
        }
        if (expenseTypeId != null && !expenseTypeRepository.existsById(expenseTypeId)) {
            throw new NotFoundException("El tipo de gasto no existe");
        }

        List<Expense> expenses = expenseRepository.findAll(
                ExpenseSpecifications.matching(from, to, expenseTypeId), NEWEST_FIRST);
        List<ExpenseResponse> content = toResponses(expenses);
        BigDecimal total = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ExpenseListResponse(content, total);
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        ExpenseType type = getType(request.expenseTypeId());
        ensureDateIsNotFuture(request.spentOn());
        Expense saved = expenseRepository.save(new Expense(type.getId(), request.spentOn(),
                request.description().trim(), request.amount()));
        log.info("Expense created: expenseId={}, expenseTypeId={}", saved.getId(), type.getId());
        return toResponse(saved, type.getName());
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = getExpense(id);
        ExpenseType type = getType(request.expenseTypeId());
        ensureDateIsNotFuture(request.spentOn());
        expense.update(type.getId(), request.spentOn(), request.description().trim(), request.amount());
        log.info("Expense updated: expenseId={}, expenseTypeId={}", expense.getId(), type.getId());
        return toResponse(expense, type.getName());
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = getExpense(id);
        expenseRepository.delete(expense);
        log.info("Expense deleted: expenseId={}", id);
    }

    private Expense getExpense(Long id) {
        return expenseRepository.findById(id).orElseThrow(() -> new NotFoundException("El gasto no existe"));
    }

    private ExpenseType getType(Long id) {
        return expenseTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El tipo de gasto no existe"));
    }

    private void ensureDateIsNotFuture(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("La fecha no puede ser futura");
        }
    }

    private List<ExpenseResponse> toResponses(List<Expense> expenses) {
        Map<Long, ExpenseType> types = expenseTypeRepository.findAllById(
                        expenses.stream().map(Expense::getExpenseTypeId).distinct().toList()).stream()
                .collect(Collectors.toMap(ExpenseType::getId, Function.identity()));
        return expenses.stream()
                .map(expense -> toResponse(expense,
                        types.containsKey(expense.getExpenseTypeId())
                                ? types.get(expense.getExpenseTypeId()).getName()
                                : UNKNOWN_TYPE))
                .toList();
    }

    private ExpenseResponse toResponse(Expense expense, String typeName) {
        return new ExpenseResponse(expense.getId(), expense.getSpentOn(), expense.getDescription(),
                expense.getAmount(), expense.getExpenseTypeId(), typeName);
    }
}
