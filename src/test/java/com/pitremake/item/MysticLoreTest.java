package com.pitremake.item;

import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.list.AquaEnchants;
import com.pitremake.enchant.list.BowEnchants;
import com.pitremake.enchant.list.SewerEnchants;
import com.pitremake.enchant.list.DarkEnchants;
import com.pitremake.enchant.list.PantsEnchants;
import com.pitremake.enchant.list.RageEnchants;
import com.pitremake.enchant.list.ResourceEnchants;
import com.pitremake.enchant.list.SwordEnchants;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MysticLoreTest {

    private EnchantRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EnchantRegistry();
        ResourceEnchants.registerAll(registry);
        SwordEnchants.registerAll(registry);
        BowEnchants.registerAll(registry);
        PantsEnchants.registerAll(registry);
        RageEnchants.registerAll(registry);
        DarkEnchants.registerAll(registry);
        SewerEnchants.registerAll(registry);
        AquaEnchants.registerAll(registry);
    }

    @Test
    void pantsLoreEndsWithAsStrongAsIronInPantsColor() {
        MysticData data = tierOne("damage_reduction", 1);
        List<String> lore = MysticLore.build(data, MysticType.PANTS, PantsColor.BLUE, registry);

        assertEquals(ChatColor.GRAY + "Lives: " + ChatColor.GREEN + "4" + ChatColor.GRAY + "/4", lore.get(0));
        assertEquals("", lore.get(lore.size() - 2));
        assertEquals(ChatColor.BLUE + "As strong as iron", lore.get(lore.size() - 1));

        List<String> rageLore = MysticLore.build(data, MysticType.PANTS, PantsColor.RAGE, registry);
        assertEquals(ChatColor.DARK_RED + "As strong as iron", rageLore.get(rageLore.size() - 1));
    }

    @Test
    void swordAndBowLoreHaveNoIronLine() {
        for (String line : MysticLore.build(tierOne("plain_melee_damage", 1), MysticType.SWORD, null, registry)) {
            assertFalse(line.contains("As strong as iron"));
        }
        for (String line : MysticLore.build(tierOne("bow_damage", 1), MysticType.BOW, null, registry)) {
            assertFalse(line.contains("As strong as iron"));
        }
    }

    @Test
    void rareLookingRageEnchantShowsRareTag() {
        MysticData data = tierOne("regularity", 2);
        data.setRagePants(true);
        List<String> lore = MysticLore.build(data, MysticType.PANTS, PantsColor.RAGE, registry);
        assertTrue(lore.contains("§dRARE! §9Regularity II"), lore.toString());
    }

    @Test
    void gemmedItemShowsDiamondNextToLives() {
        MysticData data = tierOne("plain_melee_damage", 2);
        data.setGemmed(true);
        List<String> lore = MysticLore.build(data, MysticType.SWORD, null, registry);
        assertEquals(ChatColor.GRAY + "Lives: " + ChatColor.GREEN + "4" + ChatColor.GRAY + "/4 " + ChatColor.GREEN + "♦", lore.get(0));
    }

    @Test
    void darkPantsLoreMatchesScreenshot() {
        MysticData data = tierOne("somber", 1);
        data.setDarkPants(true);
        data.setTier(2);
        data.setEnchant("misery", 1);
        data.setMaxLives(10);
        data.setLives(10);

        // 最後は "Enchants require heresy" / "As strong as leather" (パンツの色)
        assertEquals(Arrays.asList(
                ChatColor.GRAY + "Lives: " + ChatColor.GREEN + "10" + ChatColor.GRAY + "/10",
                "",
                "§9Somber",
                "§7You are unaffected by mystical",
                "§7enchantments.",
                "",
                "§9Misery",
                "§7Deal §c+0.5❤ §7true damage against",
                "§7players wearing leather armor but",
                "§7take §c0.3❤§7",
                "",
                ChatColor.DARK_PURPLE + "Enchants require heresy",
                ChatColor.DARK_PURPLE + "As strong as leather"
        ), MysticLore.build(data, MysticType.PANTS, PantsColor.DARK, registry));
        assertEquals(ChatColor.DARK_PURPLE + "Tier II Dark Pants", MysticLore.name(data, MysticType.PANTS, PantsColor.DARK, registry));

        data.setMaxLives(100);
        data.setLives(100);
        // Lives 100 以上で Evil Pants。Artifact などの称号は付かない
        assertEquals(ChatColor.DARK_PURPLE + "Tier II Evil Pants", MysticLore.name(data, MysticType.PANTS, PantsColor.DARK, registry));
    }

    @Test
    void sewerPantsLoreMatchesScreenshot() {
        MysticData data = tierOne("hidden_jewel", 1);
        data.setSpecialPants(SpecialPants.SEWER);
        data.setMaxLives(10);
        data.setLives(10);
        assertEquals(Arrays.asList(
                ChatColor.GRAY + "Lives: " + ChatColor.GREEN + "10" + ChatColor.GRAY + "/10",
                "",
                "§9Hidden Jewel",
                "§7Kill §c117 §7players to recycle",
                "§7into Tier I pants with a Tier III",
                "§7enchant.",
                ChatColor.GRAY + "Kills: " + ChatColor.GREEN + "0",
                "",
                ChatColor.DARK_AQUA + "As strong as iron"
        ), MysticLore.build(data, MysticType.PANTS, PantsColor.SEWER, registry));
    }

    @Test
    void aquaPantsLoreMatchesScreenshot() {
        MysticData data = tierOne("fishing_rod_enchant", 1);
        data.setSpecialPants(SpecialPants.AQUA);
        data.setEnchant("stereo", 1);
        data.setMaxLives(5);
        data.setLives(5);
        assertEquals(Arrays.asList(
                ChatColor.GRAY + "Lives: " + ChatColor.GREEN + "5" + ChatColor.GRAY + "/5",
                "",
                "§9Club Rod",
                "§7Spawn with a fishing rod",
                "",
                "§dRARE! §9Stereo",
                "§7You play a tune while cruising",
                "§7around",
                "",
                ChatColor.AQUA + "As strong as iron"
        ), MysticLore.build(data, MysticType.PANTS, PantsColor.AQUA, registry));
    }

    @Test
    void sewerAndAquaPantsNames() {
        MysticData sewer = tierOne("worm", 1);
        sewer.setSpecialPants(SpecialPants.SEWER);
        assertEquals(ChatColor.DARK_AQUA + "Tier I Sewer Pants", MysticLore.name(sewer, MysticType.PANTS, PantsColor.SEWER, registry));

        MysticData aqua = tierOne("fishing_rod_enchant", 1);
        aqua.setSpecialPants(SpecialPants.AQUA);
        assertEquals(ChatColor.AQUA + "Tier I Aqua Pants", MysticLore.name(aqua, MysticType.PANTS, PantsColor.AQUA, registry));
    }

    @Test
    void ragePantsName() {
        MysticData data = tierOne("sybil", 1);
        data.setRagePants(true);
        assertEquals(ChatColor.DARK_RED + "Tier I Rage Pants", MysticLore.name(data, MysticType.PANTS, PantsColor.RAGE, registry));
    }

    @Test
    void pantsNameUsesPantsColor() {
        String name = MysticLore.name(tierOne("damage_reduction", 1), MysticType.PANTS, PantsColor.BLUE, registry);
        assertEquals(ChatColor.BLUE + "Tier I Blue Pants", name);
    }

    @Test
    void enchantLinesComeFromPitpalDescriptions() {
        List<String> lore = MysticLore.build(tierOne("damage_reduction", 2), MysticType.PANTS, PantsColor.RED, registry);
        assertTrue(lore.contains("§9Protection II"), lore.toString());
    }

    private static MysticData tierOne(String key, int level) {
        MysticData data = new MysticData();
        data.setTier(1);
        data.setMaxLives(4);
        data.setLives(4);
        data.setEnchant(key, level);
        return data;
    }
}
