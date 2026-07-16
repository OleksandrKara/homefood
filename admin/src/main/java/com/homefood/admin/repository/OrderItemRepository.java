package com.homefood.admin.repository;

import com.homefood.admin.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteByOrderId(Long orderId);
}
