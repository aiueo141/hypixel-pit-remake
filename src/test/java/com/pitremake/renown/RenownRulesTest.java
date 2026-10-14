package com.pitremake.renown;

import com.pitremake.map.PitMapType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Renown Shop の Upgrades (録画の配置と wiki の数値) とマップの真ん中の判定。 */
class RenownRulesTest {

    @Test
    void recordedLayoutHasNoOverlaps() {
        Set<String> slots = new HashSet<>();
        for (RenownUpgrade upgrade : RenownUpgrade.values()) {
            assertTrue(slots.add(upgrade.getPage() + ":" + upgrade.getSlot()), upgrade + " slot");
            assertTrue(upgrade.getPage() < RenownUpgrade.PAGES);
        }
        assertEquals(21, RenownUpgrade.EXTRA_PERK_SLOT.getSlot());
        assertEquals(34, RenownUpgrade.APOLLO.getSlot());
        assertFalse(RenownUpgrade.APOLLO.isAvailable());
        assertEquals(1, RenownUpgrade.CELEBRITY.getPage());
        assertEquals(28, RenownUpgrade.CELEBRITY.getSlot());
        assertEquals(25, RenownUpgrade.CELEBRITY.getRequiredPrestige());
    }

    @Test
    void wikiCosts() {
        assertEquals(10, RenownUpgrade.RENOWN_GOLD_BOOST.getMaxTier());
        assertEquals(5, RenownUpgrade.RENOWN_GOLD_BOOST.cost(1));
        assertEquals(160, RenownUpgrade.RENOWN_XP_BUMP.cost(10));
        assertEquals(22, RenownUpgrade.RENOWN_XP_BUMP.requiredPrestige(10));
        assertEquals(18, RenownUpgrade.DIVINE_INTERVENTION.requiredPrestige(3));
        assertEquals(40, RenownUpgrade.EXTRA_PERK_SLOT.cost(1));
    }

    @Test
    void mysticismDropChance() {
        assertEquals(0, RenownEffects.mysticDropChance(0), 1e-12);
        assertEquals(0.002, RenownEffects.mysticDropChance(1), 1e-12);
        assertEquals(0.0026, RenownEffects.mysticDropChance(2), 1e-12);
        // 録画: IX で +150%、X で +200% (wiki: 約 0.5% → 0.6%)
        assertEquals(1.5, RenownEffects.mysticismBonus(9), 1e-12);
        assertEquals(0.006, RenownEffects.mysticDropChance(10), 1e-12);
    }

    @Test
    void scamArtistDiscount() {
        assertEquals(150, RenownEffects.discounted(150, 0));
        assertEquals(143, RenownEffects.discounted(150, 1));
        assertEquals(113, RenownEffects.discounted(150, 5));
    }

    @Test
    void seasonsCenterCircle() {
        PitMapType seasons = PitMapType.SEASONS;
        assertTrue(seasons.isCenter(0, 82, 0));
        // 置けない範囲は床の円 (9.9) より 1 ブロック小さい
        assertTrue(seasons.isCenter(9, 90, 0));
        assertFalse(seasons.isCenter(10, 82, 0));
        assertFalse(seasons.isCenter(0, 81, 0));
        assertEquals(seasons, PitMapType.fromName("seasons"));
    }
}
