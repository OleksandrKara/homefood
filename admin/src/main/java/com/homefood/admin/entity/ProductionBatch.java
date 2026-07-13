package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "production_batches")
@Getter
@Setter
@NoArgsConstructor
public class ProductionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull(message = "Укажите количество")
    @Positive(message = "Количество должно быть больше нуля")
    @Column(name = "quantity_produced", nullable = false)
    private Integer quantityProduced;

    @NotNull(message = "Укажите дату")
    @Column(name = "batch_date", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate batchDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.DONE;

    @Size(max = 500, message = "Слишком длинный текст")
    @Column(name = "pickup_location")
    private String pickupLocation;

    @Size(max = 255, message = "Слишком длинный текст")
    @Column(name = "pickup_window")
    private String pickupWindow;

    @Size(max = 2000, message = "Слишком длинный текст")
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
