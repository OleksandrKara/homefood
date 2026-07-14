package com.homefood.admin.repository;

import com.homefood.admin.entity.Order;
import com.homefood.admin.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByDeliveryDateAscDeliveryTimeAsc();

    List<Order> findByArchivedTrueOrderByDeliveryDateDescCreatedAtDesc();

    List<Order> findByArchivedFalseAndStatusInOrderByDeliveryDateDescCreatedAtDesc(Collection<OrderStatus> statuses);

    long countByArchivedFalseAndStatusIn(Collection<OrderStatus> statuses);

    long countByArchivedTrue();

    /** Full order history for a client (every status, including archived) - most recent first. */
    List<Order> findByClientIdOrderByDeliveryDateDescCreatedAtDesc(Long clientId);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalPriceByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.client.id = :clientId AND o.status = :status")
    BigDecimal sumTotalPriceByClientIdAndStatus(Long clientId, OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.tipAmount), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumTipAmountByStatus(OrderStatus status);

    @Query("SELECT DISTINCT o.deliveryAddress FROM Order o WHERE o.deliveryAddress IS NOT NULL AND o.deliveryAddress <> ''")
    List<String> findDistinctDeliveryAddresses();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.client.phone = :phone AND o.createdAt >= :since")
    long countByClientPhoneSince(String phone, Instant since);
}
