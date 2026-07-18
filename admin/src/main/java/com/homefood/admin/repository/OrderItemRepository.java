package com.homefood.admin.repository;

import com.homefood.admin.entity.OrderItem;
import com.homefood.admin.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteByOrderId(Long orderId);

    /** Best-selling products by revenue (paid orders only) - see DashboardController. */
    @Query("SELECT new com.homefood.admin.repository.ProductSalesStats(" +
            "p.name, p.unit, SUM(oi.quantity), COALESCE(SUM(oi.lineTotal), 0)) " +
            "FROM OrderItem oi JOIN oi.product p JOIN oi.order o " +
            "WHERE o.status = :status GROUP BY p.id, p.name, p.unit ORDER BY SUM(oi.lineTotal) DESC")
    List<ProductSalesStats> topProductsByRevenue(OrderStatus status);
}
