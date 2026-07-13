package com.homefood.admin.web;

import com.homefood.admin.entity.Ingredient;
import com.homefood.admin.entity.Product;
import com.homefood.admin.entity.Recipe;
import com.homefood.admin.repository.IngredientRepository;
import com.homefood.admin.repository.ProductRepository;
import com.homefood.admin.repository.RecipeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeControllerTest {

    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private HttpServletRequest request;

    private RecipeController controller;
    private RedirectAttributesModelMap redirectAttributes;

    @BeforeEach
    void setUp() {
        controller = new RecipeController(recipeRepository, productRepository, ingredientRepository);
        redirectAttributes = new RedirectAttributesModelMap();
    }

    private Product product(long id) {
        Product p = new Product();
        p.setId(id);
        p.setName("Квашеная капуста");
        p.setSizeLabel("1lb");
        return p;
    }

    private Ingredient ingredient(long id) {
        Ingredient i = new Ingredient();
        i.setId(id);
        i.setName("Капуста");
        i.setUnit("кг");
        return i;
    }

    private void stubParams(String[] ingredientIds, String[] quantities) {
        when(request.getParameterValues("ingredientId")).thenReturn(ingredientIds);
        when(request.getParameterValues("quantityPerUnit")).thenReturn(quantities);
    }

    // --- list / pickProduct / editForProduct ---

    @Test
    void list_groupsRecipesByProduct() {
        Product p1 = product(1);
        Product p2 = product(2);
        Recipe r1 = new Recipe();
        r1.setProduct(p1);
        r1.setIngredient(ingredient(10));
        r1.setQuantityPerUnit(new BigDecimal("1.00"));
        Recipe r2 = new Recipe();
        r2.setProduct(p1);
        r2.setIngredient(ingredient(11));
        r2.setQuantityPerUnit(new BigDecimal("2.00"));
        Recipe r3 = new Recipe();
        r3.setProduct(p2);
        r3.setIngredient(ingredient(10));
        r3.setQuantityPerUnit(new BigDecimal("3.00"));
        when(recipeRepository.findAllByOrderByProductNameAsc()).thenReturn(List.of(r1, r2, r3));

        Model model = new ExtendedModelMap();
        String view = controller.list(model);

        assertThat(view).isEqualTo("recipes/list");
        @SuppressWarnings("unchecked")
        var grouped = (java.util.Map<Product, List<Recipe>>) model.getAttribute("groupedRecipes");
        assertThat(grouped).hasSize(2);
        assertThat(grouped.get(p1)).containsExactly(r1, r2);
        assertThat(grouped.get(p2)).containsExactly(r3);
    }

    @Test
    void pickProduct_listsAllProducts() {
        List<Product> products = List.of(product(1), product(2));
        when(productRepository.findAllByOrderByNameAsc()).thenReturn(products);

        Model model = new ExtendedModelMap();
        String view = controller.pickProduct(model);

        assertThat(view).isEqualTo("recipes/new");
        assertThat(model.getAttribute("products")).isEqualTo(products);
    }

    @Test
    void editForProduct_loadsExistingRowsAndIngredients() {
        Product p = product(5);
        List<Recipe> rows = List.of(new Recipe());
        List<Ingredient> ingredients = List.of(ingredient(1));
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(recipeRepository.findByProductId(5L)).thenReturn(rows);
        when(ingredientRepository.findAllByOrderByNameAsc()).thenReturn(ingredients);

        Model model = new ExtendedModelMap();
        String view = controller.editForProduct(5L, model);

        assertThat(view).isEqualTo("recipes/product-form");
        assertThat(model.getAttribute("product")).isEqualTo(p);
        assertThat(model.getAttribute("rows")).isEqualTo(rows);
        assertThat(model.getAttribute("ingredients")).isEqualTo(ingredients);
    }

    @Test
    void editForProduct_unknownProduct_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> controller.editForProduct(99L, new ExtendedModelMap()));
    }

    // --- saveForProduct: structural / row-shape validation ---

    @Test
    void save_noRowsAtAll_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(null, null);

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Добавьте хотя бы одно сырьё");
        verify(recipeRepository, never()).saveAll(any());
    }

    @Test
    void save_nullIngredientIdElement_treatedAsBlank() {
        // getParameterValues elements are never actually null in a real servlet request, but the
        // null-check half of the ingBlank/qtyBlank OR is still exercised defensively - cover it directly.
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(new String[]{null}, new String[]{null});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Добавьте хотя бы одно сырьё");
    }

    @Test
    void save_onlyBlankSpareRow_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(new String[]{""}, new String[]{""});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Добавьте хотя бы одно сырьё");
        verify(recipeRepository, never()).saveAll(any());
    }

    @Test
    void save_ingredientChosenButQuantityBlank_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(new String[]{"1"}, new String[]{""});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Заполните и сырьё, и количество в каждой добавленной строке");
    }

    @Test
    void save_quantityFilledButIngredientBlank_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(new String[]{""}, new String[]{"2"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Заполните и сырьё, и количество в каждой добавленной строке");
    }

    @Test
    void save_quantitiesArrayShorterThanIngredientIds_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient(1)));
        stubParams(new String[]{"1", "2"}, new String[]{"3"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Заполните и сырьё, и количество в каждой добавленной строке");
    }

    @Test
    void save_quantitiesArrayEntirelyMissing_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(new String[]{"1"}, null);

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Заполните и сырьё, и количество в каждой добавленной строке");
    }

    // --- saveForProduct: value validation ---

    @Test
    void save_nonNumericIngredientId_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(new String[]{"abc"}, new String[]{"1"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Некорректное сырьё");
    }

    @Test
    void save_duplicateIngredient_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient(1)));
        stubParams(new String[]{"1", "1"}, new String[]{"1", "2"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Одно и то же сырьё выбрано дважды");
    }

    @Test
    void save_nonNumericQuantity_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(new String[]{"1"}, new String[]{"not-a-number"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Некорректное количество");
    }

    @Test
    void save_zeroQuantity_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(new String[]{"1"}, new String[]{"0"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Количество должно быть больше нуля");
    }

    @Test
    void save_negativeQuantity_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        stubParams(new String[]{"1"}, new String[]{"-3"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Количество должно быть больше нуля");
    }

    @Test
    void save_quantityExceedsColumnCapacity_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        BigDecimal tooBig = RecipeController.MAX_QUANTITY_PER_UNIT.add(BigDecimal.ONE);
        stubParams(new String[]{"1"}, new String[]{tooBig.toPlainString()});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Количество слишком большое");
    }

    @Test
    void save_unknownIngredientId_errors() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5)));
        when(ingredientRepository.findById(99L)).thenReturn(Optional.empty());
        stubParams(new String[]{"99"}, new String[]{"1"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/5");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Сырьё не найдено");
    }

    @Test
    void save_unknownProduct_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> controller.saveForProduct(99L, request, redirectAttributes));
    }

    // --- saveForProduct: happy paths ---

    @Test
    @SuppressWarnings("unchecked")
    void save_validMultiRowRecipe_replacesExistingRowsAndRedirects() {
        Product p = product(5);
        Ingredient ing1 = ingredient(1);
        Ingredient ing2 = ingredient(2);
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ing1));
        when(ingredientRepository.findById(2L)).thenReturn(Optional.of(ing2));
        stubParams(new String[]{"1", "2"}, new String[]{"3", "4.5"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("error");

        var inOrder = inOrder(recipeRepository);
        inOrder.verify(recipeRepository).deleteByProductId(5L);
        ArgumentCaptor<List<Recipe>> captor = (ArgumentCaptor<List<Recipe>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        inOrder.verify(recipeRepository).saveAll(captor.capture());

        List<Recipe> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getProduct()).isEqualTo(p);
        assertThat(saved.get(0).getIngredient()).isEqualTo(ing1);
        assertThat(saved.get(0).getQuantityPerUnit()).isEqualByComparingTo("3.00");
        assertThat(saved.get(1).getIngredient()).isEqualTo(ing2);
        assertThat(saved.get(1).getQuantityPerUnit()).isEqualByComparingTo("4.50");
    }

    @Test
    @SuppressWarnings("unchecked")
    void save_skipsBlankSpareRowAmongValidOnes() {
        Product p = product(5);
        Ingredient ing1 = ingredient(1);
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ing1));
        stubParams(new String[]{"1", ""}, new String[]{"3", ""});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes");
        ArgumentCaptor<List<Recipe>> captor = (ArgumentCaptor<List<Recipe>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(recipeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void save_commaDecimalQuantity_isNormalizedNotSilentlyCorrupted() {
        // Regression test: a form with exactly one ingredient row used to rely on Spring's
        // @RequestParam List<String> binding, which silently comma-splits a single value like
        // "1,5" into ["1","5"] with no error at all, corrupting the saved quantity to "1".
        // saveForProduct must read raw parameters and treat the comma as a decimal separator.
        Product p = product(5);
        Ingredient ing1 = ingredient(1);
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ing1));
        stubParams(new String[]{"1"}, new String[]{"1,5"});

        String view = controller.saveForProduct(5L, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes");
        ArgumentCaptor<List<Recipe>> captor = (ArgumentCaptor<List<Recipe>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(recipeRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getQuantityPerUnit()).isEqualByComparingTo("1.50");
    }

    @Test
    @SuppressWarnings("unchecked")
    void save_quantityRoundedHalfUpToColumnScale() {
        Product p = product(5);
        Ingredient ing1 = ingredient(1);
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ing1));
        stubParams(new String[]{"1"}, new String[]{"1.235"});

        controller.saveForProduct(5L, request, redirectAttributes);

        ArgumentCaptor<List<Recipe>> captor = (ArgumentCaptor<List<Recipe>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(recipeRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getQuantityPerUnit()).isEqualByComparingTo("1.24");
    }

    // --- handleSaveFailure / deleteForProduct ---

    @Test
    void handleSaveFailure_setsFriendlyErrorAndRedirectsToProductForm() {
        when(request.getRequestURI()).thenReturn("/admin/recipes/product/7");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("boom");

        String view = controller.handleSaveFailure(ex, request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/recipes/product/7");
        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Не удалось сохранить рецепт: проверьте данные");
    }

    @Test
    void deleteForProduct_deletesAndRedirects() {
        String view = controller.deleteForProduct(5L);

        assertThat(view).isEqualTo("redirect:/recipes");
        verify(recipeRepository).deleteByProductId(5L);
    }
}
