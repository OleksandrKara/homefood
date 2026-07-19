package com.homefood.admin.repository;

import java.math.BigDecimal;

/** One row of the "paid orders by payment method" breakdown - see OrderPaymentRepository.
 * paymentCount counts individual payment entries, not orders: an order split across several
 * methods (e.g. partially cash, partially Venmo) contributes one entry to each method's count. */
public record PaymentMethodStats(String paymentMethod, long paymentCount, BigDecimal total) {
}
