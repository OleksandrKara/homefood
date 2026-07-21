package com.homefood.admin.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPricingTest {

    private static final BigDecimal BASE_PRICE = new BigDecimal("5");

    @Test
    void flatPricing_multipliesRegardlessOfQuantity() {
        assertThat(OrderPricing.calculateTotal(BASE_PRICE, 1, false)).isEqualByComparingTo("5");
        assertThat(OrderPricing.calculateTotal(BASE_PRICE, 6, false)).isEqualByComparingTo("30");
    }

    @Test
    void tieredDiscount_singleUnit_isFullPrice() {
        assertThat(OrderPricing.calculateTotal(BASE_PRICE, 1, true)).isEqualByComparingTo("5");
    }

    @Test
    void tieredDiscount_threeUnits_secondAndThirdDiscounted() {
        // $5 + $4 + $4
        assertThat(OrderPricing.calculateTotal(BASE_PRICE, 3, true)).isEqualByComparingTo("13");
    }

    @Test
    void tieredDiscount_fourthUnit_backToFullPrice() {
        // $5 + $4 + $4 + $5
        assertThat(OrderPricing.calculateTotal(BASE_PRICE, 4, true)).isEqualByComparingTo("18");
    }

    /** Regression test: the discount must repeat for every group of 3, not just the first 3
     * units of the whole order - 6 units was previously miscalculated as $28 instead of $26. */
    @Test
    void tieredDiscount_sixUnits_discountRepeatsPerGroupOfThree() {
        // ($5 + $4 + $4) + ($5 + $4 + $4) = $13 + $13
        assertThat(OrderPricing.calculateTotal(BASE_PRICE, 6, true)).isEqualByComparingTo("26");
    }

    @Test
    void tieredDiscount_nineUnits_threeFullGroups() {
        // ($5 + $4 + $4) x 3
        assertThat(OrderPricing.calculateTotal(BASE_PRICE, 9, true)).isEqualByComparingTo("39");
    }
}
