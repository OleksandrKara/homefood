package com.homefood.admin.web;

import com.homefood.admin.entity.BatchStatus;
import com.homefood.admin.entity.Ingredient;
import com.homefood.admin.entity.Product;
import com.homefood.admin.entity.ProductionBatch;
import com.homefood.admin.entity.Recipe;
import com.homefood.admin.repository.IngredientRepository;
import com.homefood.admin.repository.ProductRepository;
import com.homefood.admin.repository.ProductionBatchRepository;
import com.homefood.admin.repository.RecipeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionBatchRepository productionBatchRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("batches", productionBatchRepository.findAllByOrderByBatchDateDescCreatedAtDesc());
        return "production/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        ProductionBatch batch = new ProductionBatch();
        batch.setQuantityProduced(1);
        model.addAttribute("batch", batch);
        model.addAttribute("products", productRepository.findAllByOrderByNameAsc());
        return "production/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("batch") ProductionBatch batch, BindingResult result,
                          @RequestParam Long productId, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("products", productRepository.findAllByOrderByNameAsc());
            return "production/form";
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        batch.setProduct(product);
        productionBatchRepository.save(batch);
        if (batch.getStatus() == BatchStatus.DONE) {
            applyProduction(batch);
        }
        return "redirect:/production";
    }

    /** A planned batch actually happened - apply its stock effects now. */
    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id) {
        ProductionBatch batch = productionBatchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production batch not found: " + id));
        if (batch.getStatus() == BatchStatus.PLANNED) {
            batch.setStatus(BatchStatus.DONE);
            productionBatchRepository.save(batch);
            applyProduction(batch);
        }
        return "redirect:/production";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        ProductionBatch batch = productionBatchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production batch not found: " + id));
        if (batch.getStatus() == BatchStatus.DONE) {
            reverseProduction(batch);
        }
        productionBatchRepository.deleteById(id);
        return "redirect:/production";
    }

    /** Consumes ingredient stock per the product's current recipe, and adds to the product's finished stock. */
    private void applyProduction(ProductionBatch batch) {
        BigDecimal qty = BigDecimal.valueOf(batch.getQuantityProduced());
        for (Recipe recipe : recipeRepository.findByProductId(batch.getProduct().getId())) {
            Ingredient ingredient = recipe.getIngredient();
            ingredient.setStockQuantity(ingredient.getStockQuantity().subtract(recipe.getQuantityPerUnit().multiply(qty)));
            ingredientRepository.save(ingredient);
        }
        Product product = batch.getProduct();
        product.setStockQuantity(product.getStockQuantity() + batch.getQuantityProduced());
        productRepository.save(product);
    }

    private void reverseProduction(ProductionBatch batch) {
        BigDecimal qty = BigDecimal.valueOf(batch.getQuantityProduced());
        for (Recipe recipe : recipeRepository.findByProductId(batch.getProduct().getId())) {
            Ingredient ingredient = recipe.getIngredient();
            ingredient.setStockQuantity(ingredient.getStockQuantity().add(recipe.getQuantityPerUnit().multiply(qty)));
            ingredientRepository.save(ingredient);
        }
        Product product = batch.getProduct();
        product.setStockQuantity(product.getStockQuantity() - batch.getQuantityProduced());
        productRepository.save(product);
    }
}
