package com.homefood.admin.repository;

import com.homefood.admin.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {
    java.util.List<Order> findAllByOrderByDeliveryDateAscDeliveryTimeAsc();

    @Query("SELECT DISTINCT o.deliveryAddress FROM Order o WHERE o.deliveryAddress IS NOT NULL AND o.deliveryAddress <> ''")
    java.util.List<String> findDistinctDeliveryAddresses();
}
