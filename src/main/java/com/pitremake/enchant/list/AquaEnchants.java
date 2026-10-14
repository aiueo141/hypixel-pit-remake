package com.pitremake.enchant.list;

import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.EnchantRegistry;

import static com.pitremake.enchant.DescribedEnchant.levels;
import static com.pitremake.enchant.DescribedEnchant.lines;
import static com.pitremake.enchant.EnchantCategory.AQUA;
import static com.pitremake.enchant.EnchantRarity.COMMON;
import static com.pitremake.enchant.EnchantRarity.RARE;

/**
 * Aqua Pants 専用の Aqua エンチャント (レベルは I のみ)。
 * Tier I への強化で Club Rod と、それ以外から 1 つ付く。
 * 分類は pit.wiki/Aqua_Pants、キーと説明文は pitpal.rocks のデータ。
 */
public final class AquaEnchants {

    /** Tier I で必ず付くエンチャント (釣り竿を持ってスポーンする)。 */
    public static final String CLUB_ROD = "fishing_rod_enchant";

    private AquaEnchants() {
    }

    public static void registerAll(EnchantRegistry registry) {
        registry.register(new DescribedEnchant("fishing_rod_enchant", "Club Rod", AQUA, COMMON, levels(
                lines("§7Spawn with a fishing rod"))));
        registry.register(new DescribedEnchant("rod_true_damage", "Grandmaster", AQUA, COMMON, levels(
                lines("§7Deal §c0.5❤ §7true damage on rod", "§7hit"))));
        registry.register(new DescribedEnchant("luck_of_the_pond", "Luck of the Pond", AQUA, COMMON, levels(
                lines("§7Fish §b+10% §7faster"))));
        registry.register(new DescribedEnchant("water_bucket", "Portable Pond", AQUA, COMMON, levels(
                lines("§7Spawn with a water bucket. It can", "§7pour up to 4 blocks of water, but", "§7can't pick it up. Water lasts 30", "§7seconds."))));
        registry.register(new DescribedEnchant("fishing_rod_kb", "Rodback", AQUA, COMMON, levels(
                lines("§7Your fishing rod hooks knock back", "§7players"))));
        registry.register(new DescribedEnchant("rogue", "Rogue", AQUA, COMMON, levels(
                lines("§7Deal §c+25% §7and receive §9-10%", "§9§7damage vs. players wearing", "§7§baqua §7pants"))));
        registry.register(new DescribedEnchant("tough_crew", "Tough Crew", AQUA, COMMON, levels(
                lines("§7Receive §9-15% §7damage if there", "§7are at least 3 players wearing", "§7§baqua §7pants near you"))));
        registry.register(new DescribedEnchant("trophy", "Trophy", AQUA, COMMON, levels(
                lines("§7Fish §6+2 ingots §7at a time"))));
        registry.register(new DescribedEnchant("fishers_unite", "Unite", AQUA, COMMON, levels(
                lines("§7You don't deal damage to players", "§7wearing §baqua §7pants"))));
        registry.register(new DescribedEnchant("stereo", "Stereo", AQUA, RARE, levels(
                lines("§7You play a tune while cruising", "§7around"))));
    }
}
