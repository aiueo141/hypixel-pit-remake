package com.pitremake.night;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NightRulesTest {

    @Test
    void nightfallAdvances117PerTickFor94Ticks() {
        // 録画: 4987 → 15985 (+10998 = 117 × 94)
        assertEquals(NightRules.DAY_TIME, NightRules.nightfall(0));
        assertEquals(NightRules.DAY_TIME + 117, NightRules.nightfall(1));
        assertEquals(NightRules.DAY_TIME + 10998, NightRules.nightfall(94));
        assertEquals(NightRules.NIGHT_TIME, NightRules.nightfall(200));
    }

    @Test
    void morningComesBackToTheDayTime() {
        assertEquals(NightRules.NIGHT_TIME, NightRules.morning(0));
        assertEquals(NightRules.DAY_TIME, NightRules.morning(NightRules.MORNING_TICKS));
        assertEquals(NightRules.DAY_TIME, NightRules.morning(NightRules.MORNING_TICKS + 50));
    }
}
