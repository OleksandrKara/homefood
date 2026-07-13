package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull(message = "Укажите количество")
    @Min(value = 1, message = "Количество должно быть не меньше 1")
    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType deliveryType = DeliveryType.PICKUP;

    @Size(max = 500, message = "Слишком длинный адрес")
    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Size(max = 255, message = "Слишком длинный район")
    private String district;

    @Size(max = 2000, message = "Слишком длинный текст")
    @Column(name = "delivery_details", columnDefinition = "TEXT")
    private String deliveryDetails;

    @Column(name = "delivery_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate;

    @Column(name = "delivery_time")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime deliveryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Size(max = 50, message = "Слишком длинный способ оплаты")
    @Column(name = "payment_method")
    private String paymentMethod;

    @Size(max = 2000, message = "Слишком длинный текст")
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
