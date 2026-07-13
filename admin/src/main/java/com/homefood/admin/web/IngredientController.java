package com.homefood.admin.web;

import com.homefood.admin.entity.Ingredient;
import com.homefood.admin.repository.IngredientRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("ingredients", ingredientRepository.findAllByOrderByNameAsc());
        return "ingredients/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("ingredient", new Ingredient());
        return "ingredients/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("ingredient") Ingredient ingredient, BindingResult result) {
        if (result.hasErrors()) {
            return "ingredients/form";
        }
        ingredientRepository.save(ingredient);
        return "redirect:/ingredients";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found: " + id));
        model.addAttribute("ingredient", ingredient);
        return "ingredients/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("ingredient") Ingredient ingredient, BindingResult result) {
        if (result.hasErrors()) {
            return "ingredients/form";
        }
        ingredient.setId(id);
        ingredientRepository.save(ingredient);
        return "redirect:/ingredients";
    }
}
