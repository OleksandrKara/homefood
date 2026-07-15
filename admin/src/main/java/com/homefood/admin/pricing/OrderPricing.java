package com.homefood.admin.pricing;

import java.math.BigDecimal;

/**
 * Flat price x quantity by default. When tieredDiscountEnabled is true (currently only Квашеная
 * капуста, see Product.tieredDiscountEnabled): jar #1 full price, jars #2-3 discounted $1 each,
 * jar #4 onward back to full price (the discount doesn't keep stacking).
 */
public final class OrderPricing {

    private OrderPricing() {
    }

    public static BigDecimal calculateTotal(BigDecimal basePrice, int quantity, boolean tieredDiscountEnabled) {
        if (!tieredDiscountEnabled) {
            return basePrice.multiply(BigDecimal.valueOf(quantity));
        }
        BigDecimal total = BigDecimal.ZERO;
        for (int jarIndex = 1; jarIndex <= quantity; jarIndex++) {
            BigDecimal jarPrice = (jarIndex == 2 || jarIndex == 3)
                    ? basePrice.subtract(BigDecimal.ONE)
                    : basePrice;
            total = total.add(jarPrice);
        }
        return total;
    }
}
