package com.pitremake.perk;

import com.pitremake.stats.KillRewards;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** パークの数値 (説明文・pit.wiki) と録画 2026-10-14_08-53-15.mcrc の画面の配置。 */
class PerkRulesTest {

    private static final double EPS = 1e-9;

    @Test
    void gladiatorNeedsThreePlayersAndStopsAtTen() {
        assertEquals(0, PerkRules.gladiatorReduction(2), EPS);
        assertEquals(0.09, PerkRules.gladiatorReduction(3), EPS);
        assertEquals(0.30, PerkRules.gladiatorReduction(10), EPS);
        assertEquals(0.30, PerkRules.gladiatorReduction(25), EPS);
    }

    @Test
    void strengthChainingIsEightPercentPerLevelUpToFive() {
        assertEquals(0.08, PerkRules.strengthBonus(1), EPS);
        assertEquals(0.40, PerkRules.strengthBonus(5), EPS);
        assertEquals(0.40, PerkRules.strengthBonus(9), EPS);
    }

    @Test
    void bountyHunterPerHundredGold() {
        assertEquals(0, PerkRules.bountyHunterBonus(99), EPS);
        assertEquals(0.05, PerkRules.bountyHunterBonus(500), EPS);
        assertEquals(0.12, PerkRules.bountyHunterBonus(1250), EPS);
    }

    @Test
    void conglomerateGivesTwentyPercentOfXpAsGold() {
        assertEquals(5.0, PerkRules.conglomerateGold(25), EPS);
        assertEquals(2.6, PerkRules.conglomerateGold(13), EPS);
    }

    @Test
    void overhealDoublesLimits() {
        assertEquals(2, PerkRules.healingLimit(PerkRules.GOLDEN_HEAD_LIMIT, false));
        assertEquals(4, PerkRules.healingLimit(PerkRules.GOLDEN_HEAD_LIMIT, true));
        assertEquals(2, PerkRules.healingLimit(PerkRules.OLYMPUS_LIMIT, true));
    }

    @Test
    void slotLevels() {
        assertEquals(10, PerkRules.slotLevel(0));
        assertEquals(35, PerkRules.slotLevel(1));
        assertEquals(70, PerkRules.slotLevel(2));
        assertEquals(100, PerkRules.slotLevel(3));
    }

    @Test
    void streakerTriplesStreakXp() {
        // ストリーク 5 のボーナス +5 XP が +15 XP になる
        KillRewards.Kill plain = new KillRewards.Kill(50, 50, 5, 0, 0, 0, 0, Collections.emptyList());
        KillRewards.Boosts boosts = new KillRewards.Boosts();
        boosts.streakXpMultiplier = PerkRules.STREAKER_MULTIPLIER;
        KillRewards.Kill streaker = new KillRewards.Kill(50, 50, 5, 0, 0, 0, 0, Collections.emptyList()).with(boosts);
        assertEquals(KillRewards.xp(plain) + 10, KillRewards.xp(streaker));
    }

    @Test
    void recordedLayout() {
        Set<Integer> slots = new HashSet<>();
        Set<Integer> renownSlots = new HashSet<>();
        for (Perk perk : Perk.values()) {
            assertTrue(slots.add(perk.getSlot()), perk + " slot");
            assertTrue(perk.getSlot() >= 10 && perk.getSlot() <= 43 && perk.getSlot() % 9 != 0 && perk.getSlot() % 9 != 8);
            if (perk.isRenownPerk()) {
                assertTrue(renownSlots.add(perk.getRenownSlot()), perk + " renown slot");
                assertTrue(perk.getRequiredPrestige() > 0 && perk.getRenownCost() > 0);
            }
        }
        assertEquals(28, Perk.values().length);
        assertEquals(14, renownSlots.size());
        assertEquals(31, Perk.VAMPIRE.getSlot());
        assertEquals(4000, Perk.VAMPIRE.getCost());
        assertFalse(Perk.VAMPIRE.isRenownPerk());
        assertEquals(19, Perk.BONK.getSlot());
        assertEquals(45, Perk.THICK.getRenownCost());
        assertEquals(11, Perk.THICK.getRequiredPrestige());
    }
}
