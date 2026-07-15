package com.homefood.admin.repository;

import com.homefood.admin.entity.ExpenseIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseIngredientRepository extends JpaRepository<ExpenseIngredient, Long> {
    List<ExpenseIngredient> findByExpenseId(Long expenseId);

    /**
     * Bulk JPQL delete (not a derived delete, which loads+removes each row and lets Hibernate
     * queue the DELETE after pending INSERTs) - see RecipeRepository.deleteByProductId for the
     * exact bug this avoids: re-adding an ingredient that was already on the expense would
     * otherwise collide with the not-yet-deleted original row on the unique constraint.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ExpenseIngredient ei WHERE ei.expense.id = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);
}
