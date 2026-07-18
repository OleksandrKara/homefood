package com.homefood.admin.repository;

import java.math.BigDecimal;

/** One row of the "top-selling products" leaderboard - see OrderItemRepository, DashboardController. */
public record ProductSalesStats(String productName, String unit, long totalQuantity, BigDecimal totalRevenue) {
}
