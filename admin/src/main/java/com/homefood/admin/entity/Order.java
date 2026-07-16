package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    /** An order can contain several products - see OrderItem. Populated/replaced by
     * OrderController from the submitted form rows, not bound directly by Spring MVC. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

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

    @PositiveOrZero(message = "Чаевые не могут быть отрицательными")
    @Column(name = "tip_amount")
    private BigDecimal tipAmount;

    @Size(max = 2000, message = "Слишком длинный текст")
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
