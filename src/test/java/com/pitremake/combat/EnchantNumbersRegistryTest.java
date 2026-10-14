package com.pitremake.combat;

import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.enchant.list.BowEnchants;
import com.pitremake.enchant.list.PantsEnchants;
import com.pitremake.enchant.list.RageEnchants;
import com.pitremake.enchant.list.ResourceEnchants;
import com.pitremake.enchant.list.SwordEnchants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** EnchantEffects が使うキーが登録されていて、説明文から数値が読めるか。 */
class EnchantNumbersRegistryTest {

    private static final double EPS = 1e-9;
    private static final EnchantRegistry REGISTRY = new EnchantRegistry();

    @BeforeAll
    static void register() {
        ResourceEnchants.registerAll(REGISTRY);
        SwordEnchants.registerAll(REGISTRY);
        BowEnchants.registerAll(REGISTRY);
        PantsEnchants.registerAll(REGISTRY);
        RageEnchants.registerAll(REGISTRY);
    }

    private static PitEnchant get(String key) {
        PitEnchant enchant = REGISTRY.get(key);
        assertNotNull(enchant, key);
        return enchant;
    }

    @Test
    void percentEnchantsHaveNumbers() {
        String[] keys = {"plain_melee_damage", "melee_damage_vs_bows", "melee_damage_vs_bounties", "melee_damage_vs_diamond",
                "melee_damage_vs_leather", "melee_damage_vs_high_hp", "melee_damage_vs_low_hp", "melee_damage_when_low",
                "melee_damage_when_close_low_players", "melee_combo_damage", "melee_damage_when_on_grass",
                "melee_damage_when_absorption", "melee_avenge", "melee_crit_midair", "melee_heal_on_hit",
                "bow_damage", "first_shot", "jump_spammer", "sniper", "bow_spammer", "damage_per_arrow", "bow_to_true_damage",
                "lucky_shot", "mixed_combat", "damage_reduction", "less_damage_from_arrows", "less_damage_vs_diamond_weapons",
                "less_damage_vs_bounties", "less_damage_when_high_bounty", "less_damage_on_grass", "less_damage_when_swimming",
                "less_damage_nearby_players", "solitude", "power_against_crits", "regularity"};
        for (String key : keys) {
            PitEnchant enchant = get(key);
            for (int level = 1; level <= enchant.getMaxLevel(); level++) {
                assertTrue(EnchantNumbers.percent(enchant, level, 0) > 0, key + " " + level);
            }
        }
    }

    @Test
    void heartEnchantsHaveNumbers() {
        String[] keys = {"melee_execute", "melee_gamble", "melee_lightning", "melee_healer", "arrow_true_damage", "singularity",
                "thorns", "regularity"};
        for (String key : keys) {
            PitEnchant enchant = get(key);
            for (int level = 1; level <= enchant.getMaxLevel(); level++) {
                assertTrue(EnchantNumbers.hearts(enchant, level, 0) > 0, key + " " + level);
            }
        }
    }

    @Test
    void knownValues() {
        assertEquals(0.12, EnchantNumbers.percent(get("plain_melee_damage"), 3, 0), EPS);
        assertEquals(0.60, EnchantNumbers.percent(get("solitude"), 3, 0), EPS);
        assertEquals(0.10, EnchantNumbers.percent(get("damage_reduction"), 3, 0), EPS);
        // Billionaire III: "+100% damage"
        assertEquals(1.0, EnchantNumbers.percent(get("melee_literally_p2w"), 3, 0), EPS);
        assertEquals(0.333, EnchantNumbers.percent(get("melee_literally_p2w"), 1, 0), EPS);
        assertEquals(3.0, EnchantNumbers.hearts(get("regularity"), 3, 0), EPS);
        assertEquals(0.75, EnchantNumbers.percent(get("regularity"), 3, 0), EPS);
    }
}
