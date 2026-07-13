package com.homefood.admin.web;

import com.homefood.admin.entity.Product;
import com.homefood.admin.entity.Recipe;
import com.homefood.admin.repository.ProductRepository;
import com.homefood.admin.repository.RecipeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;

    @GetMapping
    public String list(Model model) {
        List<Product> products = productRepository.findAllByOrderByNameAsc();
        Map<Long, Integer> possibleBatches = new HashMap<>();
        for (Product p : products) {
            List<Recipe> recipes = recipeRepository.findByProductId(p.getId());
            if (recipes.isEmpty()) {
                continue;
            }
            int min = Integer.MAX_VALUE;
            for (Recipe r : recipes) {
                BigDecimal stock = r.getIngredient().getStockQuantity();
                if (stock.signum() <= 0) {
                    min = 0;
                    break;
                }
                int possible = stock.divide(r.getQuantityPerUnit(), 0, RoundingMode.DOWN).intValue();
                min = Math.min(min, possible);
            }
            possibleBatches.put(p.getId(), Math.max(min, 0));
        }
        model.addAttribute("products", products);
        model.addAttribute("possibleBatches", possibleBatches);
        return "products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("product", new Product());
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") Product product, BindingResult result) {
        if (result.hasErrors()) {
            return "products/form";
        }
        productRepository.save(product);
        return "redirect:/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        model.addAttribute("product", product);
        return "products/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("product") Product product, BindingResult result) {
        if (result.hasErrors()) {
            return "products/form";
        }
        product.setId(id);
        productRepository.save(product);
        return "redirect:/products";
    }
}
