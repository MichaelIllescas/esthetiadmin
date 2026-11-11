package com.esthetiadmin.backend.expense.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expense")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_type_id", nullable = false)
    private Long expenseTypeId;

    @Column(name = "spent_on", nullable = false)
    private LocalDate spentOn;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    public Expense(Long expenseTypeId, LocalDate spentOn, String description, BigDecimal amount) {
        update(expenseTypeId, spentOn, description, amount);
    }

    public final void update(Long expenseTypeId, LocalDate spentOn, String description, BigDecimal amount) {
        this.expenseTypeId = expenseTypeId;
        this.spentOn = spentOn;
        this.description = description;
        this.amount = amount;
    }
}
