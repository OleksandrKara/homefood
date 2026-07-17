package com.homefood.admin.repository;

import java.math.BigDecimal;

/** One row of the "paid orders by payment method" breakdown - see OrderRepository. */
public record PaymentMethodStats(String paymentMethod, long orderCount, BigDecimal total) {
}
