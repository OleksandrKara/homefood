package com.homefood.admin.pricing;

import java.math.BigDecimal;

/**
 * Flat price x quantity by default. When tieredDiscountEnabled is true (currently only Квашеная
 * капуста, see Product.tieredDiscountEnabled): unit #1 full price, units #2-3 discounted $1 each,
 * unit #4 onward back to full price (the discount doesn't keep stacking).
 */
public final class OrderPricing {

    private OrderPricing() {
    }

    public static BigDecimal calculateTotal(BigDecimal basePrice, int quantity, boolean tieredDiscountEnabled) {
        if (!tieredDiscountEnabled) {
            return basePrice.multiply(BigDecimal.valueOf(quantity));
        }
        BigDecimal total = BigDecimal.ZERO;
        for (int unitIndex = 1; unitIndex <= quantity; unitIndex++) {
            BigDecimal unitPrice = (unitIndex == 2 || unitIndex == 3)
                    ? basePrice.subtract(BigDecimal.ONE)
                    : basePrice;
            total = total.add(unitPrice);
        }
        return total;
    }
}
