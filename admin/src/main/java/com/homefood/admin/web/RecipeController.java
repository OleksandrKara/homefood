package com.homefood.admin.web;

import com.homefood.admin.entity.Ingredient;
import com.homefood.admin.entity.Product;
import com.homefood.admin.entity.Recipe;
import com.homefood.admin.repository.IngredientRepository;
import com.homefood.admin.repository.ProductRepository;
import com.homefood.admin.repository.RecipeRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** DB column is NUMERIC(10,2) - largest value that fits without a "numeric field overflow". */
    static final BigDecimal MAX_QUANTITY_PER_UNIT = new BigDecimal("99999999.99");

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
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        Product product = productRef(productId);

        // Read the raw servlet parameters instead of binding to @RequestParam List<String>:
        // when a form submits exactly one "quantityPerUnit" value, Spring's collection
        // conversion silently splits it on commas (e.g. a comma-decimal "1,5" becomes ["1","5"]),
        // which corrupts the saved value with no error shown at all.
        String[] ingredientIds = request.getParameterValues("ingredientId");
        String[] quantities = request.getParameterValues("quantityPerUnit");

        List<Recipe> newRows = new ArrayList<>();
        Set<Long> seenIngredients = new HashSet<>();
        String error = null;

        int rowCount = ingredientIds == null ? 0 : ingredientIds.length;
        for (int i = 0; i < rowCount; i++) {
            String ingIdStr = ingredientIds[i];
            String qtyStr = (quantities != null && i < quantities.length) ? quantities[i] : null;
            boolean ingBlank = ingIdStr == null || ingIdStr.isBlank();
            boolean qtyBlank = qtyStr == null || qtyStr.isBlank();
            if (ingBlank && qtyBlank) {
                continue; // an empty spare row - just ignore it
            }
            if (ingBlank || qtyBlank) {
                error = "Заполните и сырьё, и количество в каждой добавленной строке";
                break;
            }
            Long ingId;
            try {
                ingId = Long.valueOf(ingIdStr.trim());
            } catch (NumberFormatException e) {
                error = "Некорректное сырьё";
                break;
            }
            if (!seenIngredients.add(ingId)) {
                error = "Одно и то же сырьё выбрано дважды";
                break;
            }
            BigDecimal qty;
            try {
                // Accept a comma as a decimal separator too (common on RU/UA keyboards).
                qty = new BigDecimal(qtyStr.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                error = "Некорректное количество";
                break;
            }
            if (qty.signum() <= 0) {
                error = "Количество должно быть больше нуля";
                break;
            }
            qty = qty.setScale(2, RoundingMode.HALF_UP);
            if (qty.compareTo(MAX_QUANTITY_PER_UNIT) > 0) {
                error = "Количество слишком большое";
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

    /** Defense in depth: any constraint violation that slips past the checks above (e.g. a
     * concurrent save) still lands the user back on the form with a readable message. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleSaveFailure(DataIntegrityViolationException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Не удалось сохранить рецепт: проверьте данные");
        String productId = request.getRequestURI().replaceAll(".*/product/(\\d+).*", "$1");
        return "redirect:/recipes/product/" + productId;
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
