package com.pitremake.bounty;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BountyRulesTest {

    @Test
    void noBountyBelowStreakSix() {
        assertEquals(0.0, BountyRules.chance(5, true));
        assertEquals(0, BountyRules.rollBump(5, 0, new Random(1)));
        assertTrue(BountyRules.chance(6, false) > 0);
    }

    @Test
    void quickKillsBumpMoreOften() {
        assertTrue(BountyRules.chance(20, true) > BountyRules.chance(20, false));
        assertTrue(BountyRules.chance(1000, true) <= 0.35);
        assertTrue(BountyRules.chance(1000, false) <= 0.25);
        assertEquals(0.07, BountyRules.chance(10, false), 1e-9);
        assertTrue(BountyRules.isQuickKill(5_000));
        assertTrue(!BountyRules.isQuickKill(20_000));
    }

    @Test
    void bumpsAreMultiplesOfFiftyAndAtMostThreeHundred() {
        Random random = new Random(42);
        for (int streak = 6; streak <= 300; streak++) {
            int bump = BountyRules.rollBump(streak, 0, random);
            assertEquals(0, bump % BountyRules.STEP, "streak " + streak);
            assertTrue(bump >= 50 && bump <= BountyRules.MAX_BUMP, "streak " + streak);
        }
    }

    @Test
    void higherStreakGivesBiggerBumps() {
        Random random = new Random(7);
        for (int i = 0; i < 100; i++) {
            assertTrue(BountyRules.rollBump(7, 0, random) <= 100);
            assertTrue(BountyRules.rollBump(150, 0, random) >= 250);
        }
    }

    @Test
    void bountyNeverExceedsFiveThousand() {
        assertEquals(50, BountyRules.rollBump(200, 4950, new Random(3)));
        assertEquals(0, BountyRules.rollBump(200, 5000, new Random(3)));
    }

    @Test
    void combatTimerGrowsFiveSecondsPerThousand() {
        assertEquals(0, BountyRules.combatExtensionMillis(950));
        assertEquals(5_000, BountyRules.combatExtensionMillis(1800));
        assertEquals(25_000, BountyRules.combatExtensionMillis(5000));
    }
}
