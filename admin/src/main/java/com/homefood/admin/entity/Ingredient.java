package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Укажите название")
    @Size(max = 255, message = "Слишком длинное название")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Укажите единицу измерения")
    @Size(max = 50, message = "Слишком длинная единица измерения")
    @Column(nullable = false)
    private String unit;

    @NotNull(message = "Укажите остаток")
    @Column(name = "stock_quantity", nullable = false)
    private BigDecimal stockQuantity = BigDecimal.ZERO;
}
