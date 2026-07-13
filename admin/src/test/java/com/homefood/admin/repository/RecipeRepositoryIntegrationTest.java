package com.homefood.admin.repository;

import com.homefood.admin.entity.Ingredient;
import com.homefood.admin.entity.Product;
import com.homefood.admin.entity.Recipe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises real Hibernate flush ordering against a real Postgres instance - this class of bug
 * (INSERTs flushing before a pending DELETE, tripping a unique constraint) cannot be reproduced
 * with a mocked repository, which is why RecipeControllerTest alone didn't catch it.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecipeRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void deleteByProductId_isImmediate_soReplacingARecipeThatKeepsAnExistingIngredientDoesNotCollide() {
        Product product = new Product();
        product.setName("ZZZ_IT_Product");
        product.setBasePrice(BigDecimal.ONE);
        product.setStockQuantity(0);
        entityManager.persist(product);

        Ingredient keptIngredient = new Ingredient();
        keptIngredient.setName("ZZZ_IT_Kept");
        keptIngredient.setUnit("кг");
        entityManager.persist(keptIngredient);

        Ingredient newIngredient = new Ingredient();
        newIngredient.setName("ZZZ_IT_New");
        newIngredient.setUnit("шт");
        entityManager.persist(newIngredient);

        Recipe original = new Recipe();
        original.setProduct(product);
        original.setIngredient(keptIngredient);
        original.setQuantityPerUnit(new BigDecimal("0.55"));
        entityManager.persist(original);
        entityManager.flush();
        entityManager.clear();

        // Mirrors RecipeController.saveForProduct's "replace the whole recipe" pattern: delete
        // every existing row for the product, then insert the full new set - which here still
        // includes the ingredient that was already there (the user just added one more row).
        recipeRepository.deleteByProductId(product.getId());

        Recipe keptRow = new Recipe();
        keptRow.setProduct(product);
        keptRow.setIngredient(keptIngredient);
        keptRow.setQuantityPerUnit(new BigDecimal("0.55"));

        Recipe addedRow = new Recipe();
        addedRow.setProduct(product);
        addedRow.setIngredient(newIngredient);
        addedRow.setQuantityPerUnit(BigDecimal.ONE);

        recipeRepository.saveAll(List.of(keptRow, addedRow));
        entityManager.flush();

        List<Recipe> rows = recipeRepository.findByProductId(product.getId());
        assertThat(rows).hasSize(2);
    }
}
