package com.homefood.admin.web;

import com.homefood.admin.entity.Expense;
import com.homefood.admin.entity.ExpenseCategory;
import com.homefood.admin.entity.Ingredient;
import com.homefood.admin.repository.ExpenseRepository;
import com.homefood.admin.repository.IngredientRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final IngredientRepository ingredientRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("expenses", expenseRepository.findAllByOrderByExpenseDateDescCreatedAtDesc());
        model.addAttribute("investmentTotal", expenseRepository.sumAmountByCategory(ExpenseCategory.INVESTMENT));
        model.addAttribute("operationalTotal", expenseRepository.sumAmountByCategory(ExpenseCategory.OPERATIONAL));
        return "expenses/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Expense expense = new Expense();
        addFormAttributes(model);
        model.addAttribute("expense", expense);
        return "expenses/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("expense") Expense expense, BindingResult result,
                          @RequestParam(required = false) Long ingredientId, Model model) {
        if (result.hasErrors()) {
            addFormAttributes(model);
            return "expenses/form";
        }
        applyIngredientRef(expense, ingredientId);
        expenseRepository.save(expense);
        applyStockEffect(expense);
        return "redirect:/expenses";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
        addFormAttributes(model);
        model.addAttribute("expense", expense);
        return "expenses/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("expense") Expense expense, BindingResult result,
                          @RequestParam(required = false) Long ingredientId, Model model) {
        if (result.hasErrors()) {
            addFormAttributes(model);
            return "expenses/form";
        }
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
        reverseStockEffect(existing);

        expense.setId(id);
        expense.setCreatedAt(existing.getCreatedAt());
        applyIngredientRef(expense, ingredientId);
        expenseRepository.save(expense);
        applyStockEffect(expense);
        return "redirect:/expenses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
        reverseStockEffect(existing);
        expenseRepository.deleteById(id);
        return "redirect:/expenses";
    }

    private void applyIngredientRef(Expense expense, Long ingredientId) {
        if (ingredientId == null) {
            expense.setIngredient(null);
            return;
        }
        expense.setIngredient(ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found: " + ingredientId)));
    }

    /** Buying an operational ingredient adds the purchased quantity to that ingredient's stock. */
    private void applyStockEffect(Expense expense) {
        if (expense.getCategory() == ExpenseCategory.OPERATIONAL
                && expense.getIngredient() != null && expense.getQuantity() != null) {
            Ingredient ingredient = expense.getIngredient();
            ingredient.setStockQuantity(ingredient.getStockQuantity().add(expense.getQuantity()));
            ingredientRepository.save(ingredient);
        }
    }

    private void reverseStockEffect(Expense expense) {
        if (expense.getCategory() == ExpenseCategory.OPERATIONAL
                && expense.getIngredient() != null && expense.getQuantity() != null) {
            Ingredient ingredient = expense.getIngredient();
            ingredient.setStockQuantity(ingredient.getStockQuantity().subtract(expense.getQuantity()));
            ingredientRepository.save(ingredient);
        }
    }

    private void addFormAttributes(Model model) {
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("ingredients", ingredientRepository.findAllByOrderByNameAsc());
    }
}
