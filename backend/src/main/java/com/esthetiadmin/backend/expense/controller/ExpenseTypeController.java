package com.esthetiadmin.backend.expense.controller;

import com.esthetiadmin.backend.expense.dto.ExpenseTypeResponse;
import com.esthetiadmin.backend.expense.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/expense-types")
@RequiredArgsConstructor
public class ExpenseTypeController implements ExpenseTypeControllerDocs {

    private final ExpenseService expenseService;

    @Override
    @GetMapping
    public List<ExpenseTypeResponse> list() {
        return expenseService.listTypes();
    }
}
