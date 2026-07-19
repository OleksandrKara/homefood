package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One payment method + amount within an {@link Order} - an order can be paid via several
 * methods at once (e.g. partially cash, partially Venmo). */
@Entity
@Table(name = "order_payments")
@Getter
@Setter
@NoArgsConstructor
public class OrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @NotBlank(message = "Укажите способ оплаты")
    @Size(max = 50, message = "Слишком длинный способ оплаты")
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @NotNull(message = "Укажите сумму")
    @Positive(message = "Сумма должна быть больше нуля")
    @Column(nullable = false)
    private BigDecimal amount;
}
