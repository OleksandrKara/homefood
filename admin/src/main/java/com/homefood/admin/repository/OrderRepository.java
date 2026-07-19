package com.homefood.admin.repository;

import com.homefood.admin.entity.Order;
import com.homefood.admin.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // spring.jpa.open-in-view is disabled (see application.yml), so any template that walks
    // o.items/item.product needs those eagerly fetched here - the Hibernate session is long gone
    // by render time otherwise. DISTINCT dedupes the parent row once per matching join row.
    // o.payments is deliberately NOT joined here too - see Order.payments javadoc for why
    // (fetch-joining it alongside items duplicated line items). It's mapped fetch=EAGER instead,
    // so Hibernate loads it via its own per-order SELECT within the same session.

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

    long countByStatus(OrderStatus status);

    /** Unique customers with at least one paid order - see DashboardController. */
    @Query("SELECT COUNT(DISTINCT o.client.id) FROM Order o WHERE o.status = :status")
    long countDistinctClientsByStatus(OrderStatus status);

    /** Revenue for one delivery date (paid orders only) - see DashboardController. */
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = :status AND o.deliveryDate = :date")
    BigDecimal sumTotalPriceByStatusAndDeliveryDate(OrderStatus status, LocalDate date);

    /** Revenue over a delivery-date range, inclusive - see DashboardController. */
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o " +
            "WHERE o.status = :status AND o.deliveryDate BETWEEN :start AND :end")
    BigDecimal sumTotalPriceByStatusAndDeliveryDateBetween(OrderStatus status, LocalDate start, LocalDate end);

    @Query("SELECT DISTINCT o.deliveryAddress FROM Order o WHERE o.deliveryAddress IS NOT NULL AND o.deliveryAddress <> ''")
    List<String> findDistinctDeliveryAddresses();

    /** One row per checkout now that a whole cart becomes a single Order (see
     * PublicShopController.reserve) - no longer needs the distinct-createdAt trick this method
     * used before order line items existed. */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.client.phone = :phone AND o.createdAt >= :since")
    long countByClientPhoneSince(String phone, Instant since);
}
