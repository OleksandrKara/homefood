package com.homefood.admin.repository;

import com.homefood.admin.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    java.util.List<Order> findAllByOrderByDeliveryDateAscDeliveryTimeAsc();
}
