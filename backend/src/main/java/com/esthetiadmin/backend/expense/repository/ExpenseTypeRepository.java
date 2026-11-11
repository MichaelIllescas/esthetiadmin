package com.esthetiadmin.backend.expense.repository;

import com.esthetiadmin.backend.expense.entity.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseTypeRepository extends JpaRepository<ExpenseType, Long> {

    List<ExpenseType> findAllByOrderByNameAsc();
}
