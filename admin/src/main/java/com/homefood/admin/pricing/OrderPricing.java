package com.homefood.admin.pricing;

import java.math.BigDecimal;

/**
 * Flat price x quantity by default. When tieredDiscountEnabled is true (currently only Квашеная
 * капуста, see Product.tieredDiscountEnabled): within every group of 3 units, the 1st is full
 * price and the 2nd/3rd are discounted $1 each - and the pattern repeats for each further group
 * of 3, it doesn't just apply once to the first 3 units of the whole order. E.g. for 6 units:
 * $5 + $4 + $4 + $5 + $4 + $4 (two groups of 3), not $5+$4+$4+$5+$5+$5 (discount only once).
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
            int positionInGroup = ((unitIndex - 1) % 3) + 1;
            BigDecimal unitPrice = (positionInGroup == 2 || positionInGroup == 3)
                    ? basePrice.subtract(BigDecimal.ONE)
                    : basePrice;
            total = total.add(unitPrice);
        }
        return total;
    }
}
