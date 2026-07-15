package com.homefood.admin.repository;

import com.homefood.admin.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    java.util.List<PaymentMethod> findAllByOrderByNameAsc();

    java.util.Optional<PaymentMethod> findByNameIgnoreCase(String name);
}
