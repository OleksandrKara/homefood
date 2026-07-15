package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category = ExpenseCategory.OPERATIONAL;

    @NotBlank(message = "Укажите описание")
    @Size(max = 500, message = "Слишком длинное описание")
    @Column(nullable = false)
    private String description;

    @NotNull(message = "Укажите сумму")
    @PositiveOrZero(message = "Сумма не может быть отрицательной")
    @Column(nullable = false)
    private BigDecimal amount;

    @NotNull(message = "Укажите дату")
    @Column(name = "expense_date", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
