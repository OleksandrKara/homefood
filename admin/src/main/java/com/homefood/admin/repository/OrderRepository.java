package com.homefood.admin.repository;

import com.homefood.admin.entity.Order;
import com.homefood.admin.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // spring.jpa.open-in-view is disabled (see application.yml), so any template that walks
    // o.items/item.product needs those eagerly fetched here - the Hibernate session is long gone
    // by render time otherwise. DISTINCT dedupes the parent row once per matching join row.

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
            "ORDER BY o.deliveryDate ASC, o.deliveryTime ASC")
    List<Order> findAllByOrderByDeliveryDateAscDeliveryTimeAsc();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
            "WHERE o.archived = true ORDER BY o.deliveryDate DESC, o.createdAt DESC")
    List<Order> findByArchivedTrueOrderByDeliveryDateDescCreatedAtDesc();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
            "WHERE o.archived = false AND o.status IN :statuses ORDER BY o.deliveryDate DESC, o.createdAt DESC")
    List<Order> findByArchivedFalseAndStatusInOrderByDeliveryDateDescCreatedAtDesc(Collection<OrderStatus> statuses);

    long countByArchivedFalseAndStatusIn(Collection<OrderStatus> statuses);

    long countByArchivedTrue();

    /** Full order history for a client (every status, including archived) - most recent first. */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
            "WHERE o.client.id = :clientId ORDER BY o.deliveryDate DESC, o.createdAt DESC")
    List<Order> findByClientIdOrderByDeliveryDateDescCreatedAtDesc(Long clientId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findWithItemsById(Long id);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalPriceByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.client.id = :clientId AND o.status = :status")
    BigDecimal sumTotalPriceByClientIdAndStatus(Long clientId, OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.tipAmount), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumTipAmountByStatus(OrderStatus status);

    @Query("SELECT DISTINCT o.deliveryAddress FROM Order o WHERE o.deliveryAddress IS NOT NULL AND o.deliveryAddress <> ''")
    List<String> findDistinctDeliveryAddresses();

    /** One row per checkout now that a whole cart becomes a single Order (see
     * PublicShopController.reserve) - no longer needs the distinct-createdAt trick this method
     * used before order line items existed. */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.client.phone = :phone AND o.createdAt >= :since")
    long countByClientPhoneSince(String phone, Instant since);
}
