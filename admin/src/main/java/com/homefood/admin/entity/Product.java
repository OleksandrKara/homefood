package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Укажите название")
    @Size(max = 255, message = "Слишком длинное название")
    @Column(nullable = false)
    private String name;

    @Size(max = 50, message = "Слишком длинный размер")
    @Column(name = "size_label")
    private String sizeLabel;

    @Column(nullable = false)
    private boolean active = true;

    @NotNull(message = "Укажите цену")
    @PositiveOrZero(message = "Цена не может быть отрицательной")
    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice = BigDecimal.ZERO;

    @NotNull(message = "Укажите остаток")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    /** Free-text approximate prep time shown on the public shop for made-to-order items,
     * e.g. "~2 часа" or "1-2 дня" - not a structured duration since it's always a rough estimate. */
    @Size(max = 255, message = "Слишком длинный текст")
    @Column(name = "prep_time_label")
    private String prepTimeLabel;
}
