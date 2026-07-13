package com.homefood.admin.repository;

import com.homefood.admin.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    java.util.List<Recipe> findAllByOrderByProductNameAsc();

    java.util.List<Recipe> findByProductId(Long productId);

    /**
     * A bulk JPQL delete (as opposed to Spring Data's default derived-delete, which loads each
     * row and calls EntityManager.remove()) executes immediately against the DB rather than
     * being queued as a Hibernate action that flushes after pending inserts. RecipeController
     * relies on this delete-then-reinsert running in that order: without it, re-saving a recipe
     * that keeps an already-existing ingredient row trips the (product_id, ingredient_id)
     * unique constraint, because the INSERT would flush before the (still-pending) DELETE.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Recipe r WHERE r.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
