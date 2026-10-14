package com.pitremake.gem;

import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.list.PantsEnchants;
import com.pitremake.enchant.list.RageEnchants;
import com.pitremake.enchant.list.ResourceEnchants;
import com.pitremake.enchant.list.SwordEnchants;
import com.pitremake.item.MysticData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemRulesTest {

    private EnchantRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EnchantRegistry();
        ResourceEnchants.registerAll(registry);
        SwordEnchants.registerAll(registry);
        PantsEnchants.registerAll(registry);
        RageEnchants.registerAll(registry);
    }

    @Test
    void tierThreeWithUpgradableEnchantIsEligible() {
        // 動画と同じ: King Buster III, XP Bump I, Pitpocket II (合計 6)
        assertTrue(GemRules.isEligible(item(3, false, "melee_damage_vs_high_hp", 3, "xp_per_kill", 1, "pickpocket", 2), registry));
    }

    @Test
    void notTierThree() {
        assertFalse(GemRules.isEligible(item(2, false, "plain_melee_damage", 1), registry));
    }

    @Test
    void alreadyGemmed() {
        assertFalse(GemRules.isEligible(item(3, true, "plain_melee_damage", 1), registry));
    }

    @Test
    void eightTokensAlready() {
        assertFalse(GemRules.isEligible(item(3, false, "plain_melee_damage", 3, "xp_boost", 3, "gold_boost", 2), registry));
    }

    @Test
    void onlyRareOrMaxedEnchants() {
        assertFalse(GemRules.isEligible(item(3, false, "melee_literally_p2w", 1, "plain_melee_damage", 3), registry));
    }

    @Test
    void rareCannotBeUpgradedButRareLookingCan() {
        assertFalse(GemRules.canUpgrade(registry.get("melee_gamble"), 1));
        // Regularity は見た目だけ RARE なので Gem で上げられる
        assertTrue(GemRules.canUpgrade(registry.get("regularity"), 1));
        assertFalse(GemRules.canUpgrade(registry.get("plain_melee_damage"), 3));
    }

    private static MysticData item(int tier, boolean gemmed, Object... enchants) {
        MysticData data = new MysticData();
        data.setTier(tier);
        data.setMaxLives(38);
        data.setLives(38);
        data.setGemmed(gemmed);
        for (int i = 0; i < enchants.length; i += 2) {
            data.setEnchant((String) enchants[i], (Integer) enchants[i + 1]);
        }
        return data;
    }
}
