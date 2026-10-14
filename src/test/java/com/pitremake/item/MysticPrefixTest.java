package com.pitremake.item;

import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.list.PantsEnchants;
import com.pitremake.enchant.list.RageEnchants;
import com.pitremake.enchant.list.ResourceEnchants;
import com.pitremake.enchant.list.SwordEnchants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MysticPrefixTest {

    private static final String SHARP = "plain_melee_damage";
    private static final String XP_BOOST = "xp_boost";
    private static final String GOLD_BOOST = "gold_boost";
    private static final String PITPOCKET = "pickpocket";
    private static final String COMBO_XP = "combo_xp";
    private static final String SYBIL = "sybil";
    private static final String PROTECTION = "damage_reduction";
    private static final String GAMBLE = "melee_gamble";
    private static final String EXECUTIONER = "melee_execute";
    private static final String BILLIONAIRE = "melee_literally_p2w";

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
    void noPrefix() {
        assertNull(prefix(item(20, SHARP, 2, XP_BOOST, 1, GAMBLE, 1)));
    }

    @Test
    void legendaryWhenTotalIsEight() {
        assertEquals(MysticPrefix.LEGENDARY, prefix(item(20, SHARP, 3, XP_BOOST, 3, GAMBLE, 2)));
    }

    @Test
    void extraordinaryWithTwoRares() {
        assertEquals(MysticPrefix.EXTRAORDINARY, prefix(item(20, XP_BOOST, 1, GAMBLE, 1, BILLIONAIRE, 3)));
    }

    @Test
    void unthinkableWithThreeRares() {
        assertEquals(MysticPrefix.UNTHINKABLE, prefix(item(20, GAMBLE, 3, EXECUTIONER, 1, BILLIONAIRE, 1)));
    }

    @Test
    void legendaryBeatsUnthinkable() {
        // Gamble III, Executioner III, Billionaire II (合計 8)
        assertEquals(MysticPrefix.LEGENDARY, prefix(item(20, GAMBLE, 3, EXECUTIONER, 3, BILLIONAIRE, 2)));
    }

    @Test
    void artifactWithFullLives() {
        assertEquals(MysticPrefix.ARTIFACT, prefix(item(100, SHARP, 2, XP_BOOST, 1)));
    }

    @Test
    void notOverpoweredWithOnlyTwoEnchants() {
        // 合計 6 でも 2 種類なら Overpowered ではなく Artifact
        assertEquals(MysticPrefix.ARTIFACT, prefix(item(100, SHARP, 3, XP_BOOST, 3)));
    }

    @Test
    void notOverpoweredBelowSixLevels() {
        assertEquals(MysticPrefix.ARTIFACT, prefix(item(100, SHARP, 2, XP_BOOST, 2, GAMBLE, 1)));
    }

    @Test
    void overpoweredWhenArtifactThreeEnchantsAndTotalAtLeastSix() {
        assertEquals(MysticPrefix.OVERPOWERED, prefix(item(100, SHARP, 3, XP_BOOST, 2, GAMBLE, 1)));
        // 合計 8 でも Artifact なら Overpowered が優先
        assertEquals(MysticPrefix.OVERPOWERED, prefix(item(100, SHARP, 3, XP_BOOST, 3, GAMBLE, 2)));
    }

    @Test
    void miraculousWhenArtifactAndUnthinkable() {
        assertEquals(MysticPrefix.MIRACULOUS, prefix(item(100, GAMBLE, 3, EXECUTIONER, 2, BILLIONAIRE, 1)));
    }

    @Test
    void oneInAMillionWhenArtifactUnthinkableAndLegendary() {
        assertEquals(MysticPrefix.ONE_IN_A_MILLION, prefix(item(100, GAMBLE, 3, EXECUTIONER, 3, BILLIONAIRE, 2)));
    }

    @Test
    void bountifulWithThreeResources() {
        // Pitpocket は剣専用だがリソース扱い
        assertEquals(MysticPrefix.BOUNTIFUL, prefix(item(20, XP_BOOST, 1, GOLD_BOOST, 2, PITPOCKET, 1)));
    }

    @Test
    void comboXpIsNotResource() {
        assertNull(prefix(item(20, XP_BOOST, 1, GOLD_BOOST, 2, COMBO_XP, 1)));
    }

    @Test
    void legendaryBeatsBountiful() {
        assertEquals(MysticPrefix.LEGENDARY, prefix(item(20, XP_BOOST, 3, GOLD_BOOST, 3, PITPOCKET, 2)));
    }

    @Test
    void maldingWhenRageEnchantIsLevelThree() {
        MysticData data = item(9, SYBIL, 3, PROTECTION, 1);
        data.setRagePants(true);
        assertEquals(MysticPrefix.MALDING, prefix(data));
    }

    @Test
    void manicInsteadOfLegendaryForRagePants() {
        MysticData data = item(20, SYBIL, 3, PROTECTION, 3, GOLD_BOOST, 2);
        data.setRagePants(true);
        assertEquals(MysticPrefix.MANIC, prefix(data));
        // 通常のパンツなら Legendary
        assertEquals(MysticPrefix.LEGENDARY, prefix(item(20, PROTECTION, 3, GOLD_BOOST, 3, XP_BOOST, 2)));
    }

    @Test
    void ragePantsNeverUnthinkable() {
        MysticData data = item(20, "sneak_teleport", 1, "phoenix", 1, "singularity", 2);
        data.setRagePants(true);
        assertNull(prefix(data));
        // Artifact でも Miraculous にはならない
        MysticData artifact = item(100, "sneak_teleport", 1, "phoenix", 1, "singularity", 2);
        artifact.setRagePants(true);
        assertEquals(MysticPrefix.ARTIFACT, prefix(artifact));
    }

    @Test
    void rareLookingRageEnchantsDoNotCountAsRare() {
        // Regularity + Think of the People は見た目だけ RARE なので Extraordinary にならない
        MysticData data = item(20, "regularity", 1, "think_of_the_people", 1, PROTECTION, 1);
        data.setRagePants(true);
        assertNull(prefix(data));
    }

    private MysticPrefix prefix(MysticData data) {
        return MysticPrefix.of(data, registry);
    }

    /** item(maxLives, key1, level1, key2, level2, ...) */
    private static MysticData item(int maxLives, Object... enchants) {
        MysticData data = new MysticData();
        data.setTier(3);
        data.setMaxLives(maxLives);
        data.setLives(maxLives);
        for (int i = 0; i < enchants.length; i += 2) {
            data.setEnchant((String) enchants[i], (Integer) enchants[i + 1]);
        }
        return data;
    }
}
