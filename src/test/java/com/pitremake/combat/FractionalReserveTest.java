package com.pitremake.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FractionalReserveTest {

    @Test
    void onePercentPerTenThousandGoldUpToTheCap() {
        // pit.wiki: -1% per 10,000g, max -15% / -21% / -30%
        assertEquals(0.00, EnchantEffects.fractionalReserve(1, 9_999), 1e-9);
        assertEquals(0.05, EnchantEffects.fractionalReserve(1, 55_000), 1e-9);
        assertEquals(0.15, EnchantEffects.fractionalReserve(1, 1_000_000), 1e-9);
        assertEquals(0.21, EnchantEffects.fractionalReserve(2, 1_000_000), 1e-9);
        assertEquals(0.30, EnchantEffects.fractionalReserve(3, 300_000), 1e-9);
        assertEquals(0.25, EnchantEffects.fractionalReserve(3, 259_999), 1e-9);
    }
}
