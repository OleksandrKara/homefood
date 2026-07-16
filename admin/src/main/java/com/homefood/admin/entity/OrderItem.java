package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One product + quantity line within an {@link Order} - an order can hold several of these. */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull(message = "Укажите количество")
    @Min(value = 1, message = "Количество должно быть не меньше 1")
    @Column(nullable = false)
    private Integer quantity;

    /** Price for this line at the time the order was placed - stored (not recomputed live) so a
     * later change to the product's base price never rewrites the price of a past order. */
    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;
}
