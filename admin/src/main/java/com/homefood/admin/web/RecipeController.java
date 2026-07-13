package com.homefood.admin.web;

import com.homefood.admin.entity.Recipe;
import com.homefood.admin.repository.IngredientRepository;
import com.homefood.admin.repository.ProductRepository;
import com.homefood.admin.repository.RecipeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("recipes", recipeRepository.findAllByOrderByProductNameAsc());
        return "recipes/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("recipe", new Recipe());
        addFormAttributes(model);
        return "recipes/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("recipe") Recipe recipe, BindingResult result,
                          @RequestParam Long productId, @RequestParam Long ingredientId, Model model) {
        if (result.hasErrors()) {
            addFormAttributes(model);
            return "recipes/form";
        }
        recipe.setProduct(productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId)));
        recipe.setIngredient(ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found: " + ingredientId)));
        recipeRepository.save(recipe);
        return "redirect:/recipes";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + id));
        model.addAttribute("recipe", recipe);
        addFormAttributes(model);
        return "recipes/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("recipe") Recipe recipe, BindingResult result,
                          @RequestParam Long productId, @RequestParam Long ingredientId, Model model) {
        if (result.hasErrors()) {
            addFormAttributes(model);
            return "recipes/form";
        }
        recipe.setId(id);
        recipe.setProduct(productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId)));
        recipe.setIngredient(ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found: " + ingredientId)));
        recipeRepository.save(recipe);
        return "redirect:/recipes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        recipeRepository.deleteById(id);
        return "redirect:/recipes";
    }

    private void addFormAttributes(Model model) {
        model.addAttribute("products", productRepository.findAllByOrderByNameAsc());
        model.addAttribute("ingredients", ingredientRepository.findAllByOrderByNameAsc());
    }
}
