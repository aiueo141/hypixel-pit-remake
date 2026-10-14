package com.pitremake.enchant.list;

import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.EnchantRegistry;

import static com.pitremake.enchant.DescribedEnchant.levels;
import static com.pitremake.enchant.DescribedEnchant.lines;
import static com.pitremake.enchant.EnchantCategory.SEWER;
import static com.pitremake.enchant.EnchantRarity.COMMON;
import static com.pitremake.enchant.EnchantRarity.RARE;

/**
 * Sewer Pants 専用の Sewer エンチャント (レベルは I のみ)。Tier I への強化で 1 つ付く。
 * 分類は pit.wiki/Sewer_Pants、キーと説明文は pitpal.rocks のデータ。
 */
public final class SewerEnchants {

    /** これが付いた強化では Lives が 30 か 40 になる (通常は 10 か 20)。 */
    public static final String TRASH_PANDA = "trash_panda";

    private SewerEnchants() {
    }

    public static void registerAll(EnchantRegistry registry) {
        registry.register(new DescribedEnchant("aegis", "Aegis", SEWER, COMMON, levels(
                lines("§7Gain a §9magic shield §7blocking", "§7one blow every 9 seconds"))));
        // Hidden Jewel は剣にも付き、強化しても I / II / III で説明は変わらない (ユーザーの指定)
        registry.register(new DescribedEnchant("hidden_jewel", "Hidden Jewel", SEWER, COMMON, levels(
                lines("§7Kill §c117 §7players to recycle", "§7into Tier I pants with a Tier III", "§7enchant."),
                lines("§7Kill §c117 §7players to recycle", "§7into Tier I pants with a Tier III", "§7enchant."),
                lines("§7Kill §c117 §7players to recycle", "§7into Tier I pants with a Tier III", "§7enchant."))));
        registry.register(new DescribedEnchant("worm", "Worm", SEWER, COMMON, levels(
                lines("§7Perma §eSpeed I §7and §b+20% XP", "§b§7from kills in the sewers."))));
        registry.register(new DescribedEnchant("trash_panda", "Trash Panda", SEWER, RARE, levels(
                lines("§7Respawn in the sewers. Can open", "§7spawn menus remotely.", "§bRight-click to open menus!"))));
    }
}
