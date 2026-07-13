package com.homefood.admin.web;

import com.homefood.admin.entity.Ingredient;
import com.homefood.admin.entity.Product;
import com.homefood.admin.entity.Recipe;
import com.homefood.admin.repository.IngredientRepository;
import com.homefood.admin.repository.ProductRepository;
import com.homefood.admin.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;

    @GetMapping
    public String list(Model model) {
        Map<Product, List<Recipe>> grouped = new LinkedHashMap<>();
        for (Recipe r : recipeRepository.findAllByOrderByProductNameAsc()) {
            grouped.computeIfAbsent(r.getProduct(), k -> new ArrayList<>()).add(r);
        }
        model.addAttribute("groupedRecipes", grouped);
        return "recipes/list";
    }

    /** Pick which product to configure a recipe for. */
    @GetMapping("/new")
    public String pickProduct(Model model) {
        model.addAttribute("products", productRepository.findAllByOrderByNameAsc());
        return "recipes/new";
    }

    @GetMapping("/product/{productId}")
    public String editForProduct(@PathVariable Long productId, Model model) {
        Product product = productRef(productId);
        model.addAttribute("product", product);
        model.addAttribute("rows", recipeRepository.findByProductId(productId));
        model.addAttribute("ingredients", ingredientRepository.findAllByOrderByNameAsc());
        return "recipes/product-form";
    }

    @Transactional
    @PostMapping("/product/{productId}")
    public String saveForProduct(@PathVariable Long productId,
                                  @RequestParam(required = false) List<String> ingredientId,
                                  @RequestParam(required = false) List<String> quantityPerUnit,
                                  RedirectAttributes redirectAttributes) {
        Product product = productRef(productId);

        List<Recipe> newRows = new ArrayList<>();
        Set<Long> seenIngredients = new HashSet<>();
        String error = null;

        int rowCount = ingredientId == null ? 0 : ingredientId.size();
        for (int i = 0; i < rowCount && error == null; i++) {
            String ingIdStr = ingredientId.get(i);
            String qtyStr = (quantityPerUnit != null && i < quantityPerUnit.size()) ? quantityPerUnit.get(i) : null;
            boolean ingBlank = ingIdStr == null || ingIdStr.isBlank();
            boolean qtyBlank = qtyStr == null || qtyStr.isBlank();
            if (ingBlank && qtyBlank) {
                continue; // an empty spare row - just ignore it
            }
            if (ingBlank || qtyBlank) {
                error = "Заполните и сырьё, и количество в каждой добавленной строке";
                break;
            }
            Long ingId = Long.valueOf(ingIdStr);
            if (!seenIngredients.add(ingId)) {
                error = "Одно и то же сырьё выбрано дважды";
                break;
            }
            BigDecimal qty;
            try {
                qty = new BigDecimal(qtyStr);
            } catch (NumberFormatException e) {
                error = "Некорректное количество";
                break;
            }
            if (qty.signum() <= 0) {
                error = "Количество должно быть больше нуля";
                break;
            }
            Ingredient ingredient = ingredientRepository.findById(ingId).orElse(null);
            if (ingredient == null) {
                error = "Сырьё не найдено";
                break;
            }
            Recipe row = new Recipe();
            row.setProduct(product);
            row.setIngredient(ingredient);
            row.setQuantityPerUnit(qty);
            newRows.add(row);
        }
        if (error == null && newRows.isEmpty()) {
            error = "Добавьте хотя бы одно сырьё";
        }
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/recipes/product/" + productId;
        }

        recipeRepository.deleteByProductId(productId);
        recipeRepository.saveAll(newRows);
        return "redirect:/recipes";
    }

    @Transactional
    @PostMapping("/product/{productId}/delete")
    public String deleteForProduct(@PathVariable Long productId) {
        recipeRepository.deleteByProductId(productId);
        return "redirect:/recipes";
    }

    private Product productRef(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }
}
