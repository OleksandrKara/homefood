package com.homefood.admin.repository;

import com.homefood.admin.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    java.util.List<Recipe> findAllByOrderByProductNameAsc();

    java.util.List<Recipe> findByProductId(Long productId);

    void deleteByProductId(Long productId);
}
