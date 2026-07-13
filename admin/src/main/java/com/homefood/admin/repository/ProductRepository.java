package com.homefood.admin.repository;

import com.homefood.admin.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.List<Product> findAllByOrderByNameAsc();

    java.util.List<Product> findAllByActiveTrueOrderByNameAsc();
}
