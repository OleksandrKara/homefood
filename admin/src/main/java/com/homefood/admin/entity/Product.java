package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
    @Column(nullable = false)
    private String name;

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
}
