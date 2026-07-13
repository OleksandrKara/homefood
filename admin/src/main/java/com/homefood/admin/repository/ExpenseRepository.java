package com.homefood.admin.repository;

import com.homefood.admin.entity.Expense;
import com.homefood.admin.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    java.util.List<Expense> findAllByOrderByExpenseDateDescCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.category = :category")
    BigDecimal sumAmountByCategory(ExpenseCategory category);
}
