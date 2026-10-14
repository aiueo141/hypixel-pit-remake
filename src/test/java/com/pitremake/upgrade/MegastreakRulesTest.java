package com.pitremake.upgrade;

import org.junit.jupiter.api.Test;

import static com.pitremake.upgrade.Megastreak.BEASTMODE;
import static com.pitremake.upgrade.Megastreak.HERMIT;
import static com.pitremake.upgrade.Megastreak.HIGHLANDER;
import static com.pitremake.upgrade.Megastreak.OVERDRIVE;
import static com.pitremake.upgrade.Megastreak.TO_THE_MOON;
import static com.pitremake.upgrade.Megastreak.UBERSTREAK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** pit.wiki の各メガストリークのページの数値。 */
class MegastreakRulesTest {

    private static final double EPS = 1e-9;

    @Test
    void nothingBeforeTrigger() {
        for (Megastreak megastreak : Megastreak.values()) {
            int before = megastreak.getTrigger() - 1;
            assertFalse(MegastreakRules.isActive(megastreak, before));
            assertEquals(0, MegastreakRules.xpBonus(megastreak, before), EPS);
            assertEquals(0, MegastreakRules.goldBonus(megastreak, before), EPS);
            assertEquals(1, MegastreakRules.damageTakenMultiplier(megastreak, before), EPS);
            assertEquals(0, MegastreakRules.trueDamageTaken(megastreak, before), EPS);
        }
        assertEquals(50, OVERDRIVE.getTrigger());
        assertEquals(100, TO_THE_MOON.getTrigger());
        assertEquals(100, UBERSTREAK.getTrigger());
    }

    @Test
    void overdrive() {
        assertEquals(0.5, MegastreakRules.xpBonus(OVERDRIVE, 50), EPS);
        assertEquals(1.0, MegastreakRules.goldBonus(OVERDRIVE, 50), EPS);
        // 50 キルを超えて 5 キルごとに 0.1❤ (体力 0.2) の確定ダメージ
        assertEquals(0, MegastreakRules.trueDamageTaken(OVERDRIVE, 54), EPS);
        assertEquals(0.2, MegastreakRules.trueDamageTaken(OVERDRIVE, 55), EPS);
        assertEquals(2.0, MegastreakRules.trueDamageTaken(OVERDRIVE, 100), EPS);
    }

    @Test
    void beastmode() {
        assertEquals(1.25, MegastreakRules.damageDealtMultiplier(BEASTMODE, 50, true, false), EPS);
        assertEquals(0.5, MegastreakRules.xpBonus(BEASTMODE, 60), EPS);
        assertEquals(0.75, MegastreakRules.goldBonus(BEASTMODE, 60), EPS);
        assertEquals(0.4, MegastreakRules.extraDamageTaken(BEASTMODE, 60), EPS);
        assertEquals(0, MegastreakRules.trueDamageTaken(BEASTMODE, 60), EPS);
    }

    @Test
    void hermit() {
        assertEquals(0.05, MegastreakRules.xpBonus(HERMIT, 50), EPS);
        assertEquals(0.10, MegastreakRules.goldBonus(HERMIT, 60), EPS);
        assertEquals(0.75, MegastreakRules.xpBonus(HERMIT, 190), EPS);
        assertEquals(0.75, MegastreakRules.xpBonus(HERMIT, 500), EPS);
        assertEquals(1.03, MegastreakRules.damageTakenMultiplier(HERMIT, 60), EPS);
        assertTrue(MegastreakRules.isTrueDamageImmune(HERMIT, 50));
        assertFalse(MegastreakRules.isTrueDamageImmune(HERMIT, 49));
    }

    @Test
    void highlander() {
        assertEquals(1.1, MegastreakRules.goldBonus(HIGHLANDER, 50), EPS);
        assertEquals(1.33, MegastreakRules.damageDealtMultiplier(HIGHLANDER, 50, true, true), EPS);
        assertEquals(1.0, MegastreakRules.damageDealtMultiplier(HIGHLANDER, 50, true, false), EPS);
        assertEquals(10_000, MegastreakRules.maxBounty(HIGHLANDER, 50));
        assertEquals(5_000, MegastreakRules.maxBounty(HIGHLANDER, 49));
        // Bounty Hunter の相手からは 50 キルを超えた 1 キルごとに +0.3%
        assertEquals(0.03, MegastreakRules.bountyHunterDamageTaken(HIGHLANDER, 60), EPS);
        assertEquals(0, MegastreakRules.bountyHunterDamageTaken(HIGHLANDER, 49), EPS);
        assertEquals(5_000, MegastreakRules.maxBounty(OVERDRIVE, 200));
    }

    @Test
    void toTheMoon() {
        assertEquals(0.2, MegastreakRules.xpBonus(TO_THE_MOON, 100), EPS);
        assertEquals(100, MegastreakRules.maxXpBonus(TO_THE_MOON, 100));
        assertEquals(1.0, MegastreakRules.damageTakenMultiplier(TO_THE_MOON, 119), EPS);
        assertEquals(1.1, MegastreakRules.damageTakenMultiplier(TO_THE_MOON, 120), EPS);
        assertEquals(0, MegastreakRules.trueDamageTaken(TO_THE_MOON, 219), EPS);
        assertEquals(0.2, MegastreakRules.trueDamageTaken(TO_THE_MOON, 220), EPS);
        assertEquals(0.5, MegastreakRules.moonPayoutMultiplier(200), EPS);
        assertEquals(1.0, MegastreakRules.moonPayoutMultiplier(300), EPS);
        assertEquals(1.0, MegastreakRules.moonPayoutMultiplier(500), EPS);
    }

    @Test
    void uberstreak() {
        assertEquals(0.6, MegastreakRules.damageDealtMultiplier(UBERSTREAK, 100, true, false), EPS);
        assertEquals(1.1, MegastreakRules.damageTakenMultiplier(UBERSTREAK, 150), EPS);
        assertEquals(1.2, MegastreakRules.damageTakenMultiplier(UBERSTREAK, 200), EPS);
        assertEquals(0, MegastreakRules.lostMaxHealth(UBERSTREAK, 199));
        assertEquals(4, MegastreakRules.lostMaxHealth(UBERSTREAK, 200));
        assertTrue(MegastreakRules.canHeal(UBERSTREAK, 399));
        assertFalse(MegastreakRules.canHeal(UBERSTREAK, 400));
    }

    @Test
    void uberTagMatchesRecording() {
        assertEquals("§d§lUBER100", UBERSTREAK.tag(100));
        assertEquals("§d§lUBER100", UBERSTREAK.tag(180));
        assertEquals("§d§lUBER200", UBERSTREAK.tag(200));
    }
}
