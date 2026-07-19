package com.homefood.admin.repository;

import com.homefood.admin.entity.OrderPayment;
import com.homefood.admin.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
    void deleteByOrderId(Long orderId);

    /** Paid-orders breakdown by payment method - see PaymentMethodController. Aggregates payment
     * rows rather than orders, since one order can now be split across several methods. */
    @Query("SELECT new com.homefood.admin.repository.PaymentMethodStats(" +
            "op.paymentMethod, COUNT(op), COALESCE(SUM(op.amount), 0)) " +
            "FROM OrderPayment op WHERE op.order.status = :status " +
            "GROUP BY op.paymentMethod ORDER BY SUM(op.amount) DESC")
    List<PaymentMethodStats> sumAndCountByPaymentMethodAndOrderStatus(OrderStatus status);
}
