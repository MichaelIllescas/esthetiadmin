package com.esthetiadmin.backend.expense.controller;

import com.esthetiadmin.backend.expense.dto.ExpenseListResponse;
import com.esthetiadmin.backend.expense.dto.ExpenseRequest;
import com.esthetiadmin.backend.expense.dto.ExpenseResponse;
import com.esthetiadmin.backend.expense.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController implements ExpenseControllerDocs {

    private final ExpenseService expenseService;

    @Override
    @GetMapping
    public ExpenseListResponse search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long expenseTypeId) {
        return expenseService.search(from, to, expenseTypeId);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(request);
    }

    @Override
    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(id, request);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }
}
