package com.homefood.admin.repository;

import com.homefood.admin.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    java.util.List<Ingredient> findAllByOrderByNameAsc();
}
