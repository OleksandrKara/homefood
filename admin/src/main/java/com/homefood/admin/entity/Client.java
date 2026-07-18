package com.homefood.admin.entity;

import com.homefood.admin.phone.PhoneNumbers;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Укажите имя")
    @Size(max = 255, message = "Слишком длинное имя")
    @Column(nullable = false)
    private String name;

    @Size(max = 50, message = "Слишком длинный телефон")
    private String phone;

    @Size(max = 500, message = "Слишком длинный адрес")
    private String address;

    @Size(max = 2000, message = "Слишком длинный текст")
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean archived = false;

    /** Keeps every stored phone in one consistent format (see PhoneNumbers.canonicalize)
     * regardless of which path saved it - public shop, admin form, anywhere else later. */
    @PrePersist
    @PreUpdate
    private void normalizePhone() {
        phone = PhoneNumbers.canonicalize(phone);
    }
}
