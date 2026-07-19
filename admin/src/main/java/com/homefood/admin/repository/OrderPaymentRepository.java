package com.homefood.admin.repository;

import com.homefood.admin.entity.OrderPayment;
import com.homefood.admin.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
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

    /** "Nam dolzhny" (owed to us) block on the orders list - see OrderController. Depends on a
     * payment method literally named this being used to record partial/unpaid amounts. */
    @Query("SELECT COALESCE(SUM(op.amount), 0) FROM OrderPayment op " +
            "WHERE op.paymentMethod = :method AND op.order.status = :status")
    BigDecimal sumAmountByPaymentMethodAndOrderStatus(String method, OrderStatus status);

    @Query("SELECT COUNT(DISTINCT op.order.client.id) FROM OrderPayment op " +
            "WHERE op.paymentMethod = :method AND op.order.status = :status")
    long countDistinctClientsByPaymentMethodAndOrderStatus(String method, OrderStatus status);
}
