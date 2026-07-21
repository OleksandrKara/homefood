package com.homefood.admin.web;

import com.homefood.admin.entity.Expense;
import com.homefood.admin.entity.ExpenseCategory;
import com.homefood.admin.entity.ExpenseIngredient;
import com.homefood.admin.entity.FundingSource;
import com.homefood.admin.entity.Ingredient;
import com.homefood.admin.repository.ExpenseIngredientRepository;
import com.homefood.admin.repository.ExpenseRepository;
import com.homefood.admin.repository.IngredientRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    /** DB column is NUMERIC(10,2) - largest value that fits without a "numeric field overflow". */
    static final BigDecimal MAX_QUANTITY = new BigDecimal("99999999.99");

    private final ExpenseRepository expenseRepository;
    private final ExpenseIngredientRepository expenseIngredientRepository;
    private final IngredientRepository ingredientRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("expenses", expenseRepository.findAllByOrderByExpenseDateDescCreatedAtDesc());
        model.addAttribute("investmentTotal", expenseRepository.sumAmountByCategory(ExpenseCategory.INVESTMENT));
        model.addAttribute("operationalTotal", expenseRepository.sumAmountByCategory(ExpenseCategory.OPERATIONAL));
        model.addAttribute("investorTotal", expenseRepository.sumAmountByFundingSource(FundingSource.INVESTOR));
        model.addAttribute("workingCapitalTotal", expenseRepository.sumAmountByFundingSource(FundingSource.WORKING_CAPITAL));

        Map<Long, List<ExpenseIngredient>> ingredientsByExpense = new LinkedHashMap<>();
        for (ExpenseIngredient ei : expenseIngredientRepository.findAll()) {
            ingredientsByExpense.computeIfAbsent(ei.getExpense().getId(), k -> new ArrayList<>()).add(ei);
        }
        model.addAttribute("ingredientsByExpense", ingredientsByExpense);
        return "expenses/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        addFormAttributes(model);
        model.addAttribute("expense", new Expense());
        model.addAttribute("rows", List.of());
        return "expenses/form";
    }

    @Transactional
    @PostMapping
    public String create(@Valid @ModelAttribute("expense") Expense expense, BindingResult result,
                          HttpServletRequest request, Model model) {
        List<ExpenseIngredient> rows = parseIngredientRows(expense, request, result);
        if (result.hasErrors()) {
            addFormAttributes(model);
            model.addAttribute("rows", List.of());
            return "expenses/form";
        }
        expenseRepository.save(expense);
        saveRowsAndApplyStock(expense, rows);
        return "redirect:/expenses";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Expense expense = expenseRef(id);
        addFormAttributes(model);
        model.addAttribute("expense", expense);
        model.addAttribute("rows", expenseIngredientRepository.findByExpenseId(id));
        return "expenses/form";
    }

    @Transactional
    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("expense") Expense expense, BindingResult result,
                          HttpServletRequest request, Model model) {
        List<ExpenseIngredient> rows = parseIngredientRows(expense, request, result);
        Expense existing = expenseRef(id);
        if (result.hasErrors()) {
            addFormAttributes(model);
            model.addAttribute("rows", expenseIngredientRepository.findByExpenseId(id));
            return "expenses/form";
        }

        reverseStockEffect(id);
        expenseIngredientRepository.deleteByExpenseId(id);

        expense.setId(id);
        expense.setCreatedAt(existing.getCreatedAt());
        expenseRepository.save(expense);
        saveRowsAndApplyStock(expense, rows);
        return "redirect:/expenses";
    }

    @Transactional
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        reverseStockEffect(id);
        expenseIngredientRepository.deleteByExpenseId(id);
        expenseRepository.deleteById(id);
        return "redirect:/expenses";
    }

    /**
     * Reads raw servlet parameters instead of binding to @RequestParam List<String>: when a form
     * submits exactly one ingredient row, Spring's collection conversion silently comma-splits a
     * single value (e.g. a comma-decimal "1,5" becomes ["1","5"]), corrupting the quantity with no
     * error shown at all. Same fix as RecipeController.saveForProduct.
     */
    private List<ExpenseIngredient> parseIngredientRows(Expense expense, HttpServletRequest request, BindingResult result) {
        List<ExpenseIngredient> rows = new ArrayList<>();
        if (expense.getCategory() != ExpenseCategory.OPERATIONAL) {
            return rows;
        }

        String[] ingredientIds = request.getParameterValues("ingredientId");
        String[] quantities = request.getParameterValues("quantity");
        Set<Long> seenIngredients = new HashSet<>();

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
                result.reject("ingredientRows", "Заполните и сырьё, и количество в каждой добавленной строке");
                return rows;
            }
            Long ingId;
            try {
                ingId = Long.valueOf(ingIdStr.trim());
            } catch (NumberFormatException e) {
                result.reject("ingredientRows", "Некорректное сырьё");
                return rows;
            }
            if (!seenIngredients.add(ingId)) {
                result.reject("ingredientRows", "Одно и то же сырьё выбрано дважды");
                return rows;
            }
            BigDecimal qty;
            try {
                qty = new BigDecimal(qtyStr.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                result.reject("ingredientRows", "Некорректное количество");
                return rows;
            }
            if (qty.signum() <= 0) {
                result.reject("ingredientRows", "Количество должно быть больше нуля");
                return rows;
            }
            qty = qty.setScale(2, RoundingMode.HALF_UP);
            if (qty.compareTo(MAX_QUANTITY) > 0) {
                result.reject("ingredientRows", "Количество слишком большое");
                return rows;
            }
            Ingredient ingredient = ingredientRepository.findById(ingId).orElse(null);
            if (ingredient == null) {
                result.reject("ingredientRows", "Сырьё не найдено");
                return rows;
            }
            ExpenseIngredient row = new ExpenseIngredient();
            row.setIngredient(ingredient);
            row.setQuantity(qty);
            rows.add(row);
        }
        return rows;
    }

    /** Buying operational ingredients adds each purchased quantity to that ingredient's stock. */
    private void saveRowsAndApplyStock(Expense expense, List<ExpenseIngredient> rows) {
        for (ExpenseIngredient row : rows) {
            row.setExpense(expense);
            Ingredient ingredient = row.getIngredient();
            ingredient.setStockQuantity(ingredient.getStockQuantity().add(row.getQuantity()));
            ingredientRepository.save(ingredient);
        }
        expenseIngredientRepository.saveAll(rows);
    }

    private void reverseStockEffect(Long expenseId) {
        for (ExpenseIngredient row : expenseIngredientRepository.findByExpenseId(expenseId)) {
            Ingredient ingredient = row.getIngredient();
            ingredient.setStockQuantity(ingredient.getStockQuantity().subtract(row.getQuantity()));
            ingredientRepository.save(ingredient);
        }
    }

    private Expense expenseRef(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
    }

    private void addFormAttributes(Model model) {
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("fundingSources", FundingSource.values());
        model.addAttribute("ingredients", ingredientRepository.findAllByOrderByNameAsc());
    }
}
