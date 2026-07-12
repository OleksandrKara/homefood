package com.homefood.admin.pricing;

import java.math.BigDecimal;

/**
 * Jar #1 is full price; jars #2 and #3 are discounted by $1 each; jar #4
 * onward is back to full price (the 3-jar discount doesn't keep stacking).
 * With the current $5 base price this reproduces 1=$5, 2=$9, 3=$13, 4=$18.
 */
public final class OrderPricing {

    private OrderPricing() {
    }

    public static BigDecimal calculateTotal(BigDecimal basePrice, int quantity) {
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
