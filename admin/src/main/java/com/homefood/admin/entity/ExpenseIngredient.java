package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One raw-ingredient line within a single expense - lets one purchase (one receipt/trip)
 * replenish several ingredients at once instead of forcing a separate expense per ingredient. */
@Entity
@Table(name = "expense_ingredients")
@Getter
@Setter
@NoArgsConstructor
public class ExpenseIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @NotNull(message = "Укажите количество")
    @Positive(message = "Количество должно быть больше нуля")
    @Column(nullable = false)
    private BigDecimal quantity;
}
