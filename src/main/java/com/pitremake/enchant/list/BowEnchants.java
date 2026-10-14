package com.pitremake.enchant.list;

import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.EnchantRegistry;

import static com.pitremake.enchant.DescribedEnchant.levels;
import static com.pitremake.enchant.DescribedEnchant.lines;
import static com.pitremake.enchant.EnchantCategory.BOW;
import static com.pitremake.enchant.EnchantRarity.COMMON;
import static com.pitremake.enchant.EnchantRarity.RARE;
import static com.pitremake.enchant.EnchantRarity.UNCOMMON;

/**
 * 弓専用エンチャント。分類は pit.wiki/Mystic_Bow、キーと各レベルの説明文は pitpal.rocks のデータ。
 * 全アイテム共通のリソース系は {@link ResourceEnchants} にある。
 */
public final class BowEnchants {

    private BowEnchants() {
    }

    public static void registerAll(EnchantRegistry registry) {
        // Common
        registry.register(new DescribedEnchant("gain_arrows_on_hit", "Bottomless Quiver", BOW, COMMON, levels(
                lines("§7Get §f1 arrow §7on arrow hit"),
                lines("§7Get §f3 arrows §7on arrow hit"),
                lines("§7Get §f8 arrows §7on arrow hit"))));
        registry.register(new DescribedEnchant("first_shot", "First Shot", BOW, COMMON, levels(
                lines("§7First arrow hit on a player deals", "§7§c+10% damage"),
                lines("§7First arrow hit on a player deals", "§7§c+16% damage"),
                lines("§7First arrow hit on a player deals", "§7§c+25% damage"))));
        registry.register(new DescribedEnchant("bow_damage", "Fletching", BOW, COMMON, levels(
                lines("§7Deal §c+7% §7bow damage"),
                lines("§7Deal §c+12% §7bow damage"),
                lines("§7Deal §c+20% §7bow damage"))));
        registry.register(new DescribedEnchant("jump_spammer", "Jumpspammer", BOW, COMMON, levels(
                lines("§7Deal §c+10% §7damage while midair", "§7on arrow hit"),
                lines("§7While midair, your arrows deal", "§7§c+16% §7damage. While midair,", "§7receive §9-10% §7damage from melee", "§7and ranged attacks"),
                lines("§7While midair, your arrows deal", "§7§c+24% §7damage. While midair,", "§7receive §9-20% §7damage from melee", "§7and ranged attacks"))));
        registry.register(new DescribedEnchant("sniper", "Sniper", BOW, COMMON, levels(
                lines("§7Deal §c+9% §7damage when shooting", "§7from over §f24 §7blocks"),
                lines("§7Deal §c+18% §7damage when shooting", "§7from over §f24 §7blocks"),
                lines("§7Deal §c+30% §7damage when shooting", "§7from over §f24 §7blocks"))));
        registry.register(new DescribedEnchant("bow_spammer", "Spammer and Proud", BOW, COMMON, levels(
                lines("§7Deal §c+9% §7damage when shooting", "§7within §f8 §7blocks"),
                lines("§7Deal §c+15% §7damage when shooting", "§7within §f8 §7blocks"),
                lines("§7Deal §c+21% §7damage when shooting", "§7within §f8 §7blocks"))));

        // Uncommon
        registry.register(new DescribedEnchant("damage_per_arrow", "Arrow Armory", BOW, UNCOMMON, levels(
                lines("§7Deals §c+12% §7damage but uses §f3", "§farrows §7per shot, if available"),
                lines("§7Deals §c+25% §7damage but uses §f5", "§farrows §7per shot, if available"),
                lines("§7Deals §c+60% §7damage but uses §f8", "§farrows §7per shot, if available"))));
        registry.register(new DescribedEnchant("arrow_true_damage", "Chipping", BOW, UNCOMMON, levels(
                lines("§7Deals §c0.5❤ §7extra true damage"),
                lines("§7Deals §c1.0❤ §7extra true damage"),
                lines("§7Deals §c1.5❤ §7extra true damage"))));
        registry.register(new DescribedEnchant("bow_combo_speed", "Faster than their shadow", BOW, UNCOMMON, levels(
                lines("§7Hitting §f3 §7shots without", "§7missing grants §eSpeed II §7(4s)"),
                lines("§7Hitting §f2 §7shots without", "§7missing grants §eSpeed III §7(4s)"),
                lines("§7Hitting §f2 §7shots without", "§7missing grants §eSpeed IV §7(4s)"))));
        registry.register(new DescribedEnchant("mixed_combat", "Mixed Combat", BOW, UNCOMMON, levels(
                lines("§7Shooting an enemy empowers your", "§7next melee strike against them for", "§7§c+10% §7extra damage"),
                lines("§7Shooting an enemy empowers your", "§7next melee strike against them for", "§7§c+20% §7extra damage"),
                lines("§7Shooting an enemy empowers your", "§7next melee strike against them for", "§7§c+30% §7extra damage"))));
        registry.register(new DescribedEnchant("parasite", "Parasite", BOW, UNCOMMON, levels(
                lines("§7Heal §c0.25❤ §7on arrow hit"),
                lines("§7Heal §c0.5❤ §7on arrow hit"),
                lines("§7Heal §c1.0❤ §7on arrow hit"))));
        registry.register(new DescribedEnchant("pin_down", "Pin down", BOW, UNCOMMON, levels(
                lines("§7Fully charged shots pin the victim", "§7down, preventing them from gaining", "§7Speed and Jump Boost (3s)"),
                lines("§7Fully charged shots pin the victim", "§7down, preventing them from gaining", "§7Speed and Jump Boost (5s)"),
                lines("§7Fully charged shots pin the victim", "§7down, preventing them from gaining", "§7Speed and Jump Boost (10s)"))));
        registry.register(new DescribedEnchant("punch_once_in_a_while", "Push comes to shove", BOW, UNCOMMON, levels(
                lines("§7Every 3rd shot on a player has", "§7§bPunch"),
                lines("§7Every 3rd shot on a player has", "§7§bPunch V §7and deals §c+0.5❤", "§c§7extra damage"),
                lines("§7Every 3rd shot on a player has", "§7§bPunch VII §7and deals §c+1❤", "§c§7extra damage"))));
        registry.register(new DescribedEnchant("bow_slow", "Sprint Drain", BOW, UNCOMMON, levels(
                lines("§7Arrow shots grant you §eSpeed I", "§e§7(s)"),
                lines("§7Arrow shots grant you §eSpeed I", "§e§7(5s) and apply §9Slowness I", "§9§7(3s)"),
                lines("§7Arrow shots grant you §eSpeed II", "§e§7(7s) and apply §9Slowness I", "§9§7(3s)"))));
        registry.register(new DescribedEnchant("heal_on_shoot_self", "What doesn't kill you", BOW, UNCOMMON, levels(
                lines("§7Shooting yourself heals you §c1.5❤"),
                lines("§7Shooting yourself heals you §c2.5❤", "§c§7(3s cd)"),
                lines("§7Shooting yourself heals you §c3.5❤"))));
        registry.register(new DescribedEnchant("bow_weakness_on_hit", "Wasp", BOW, UNCOMMON, levels(
                lines("§7Apply §cWeakness II §7(6s) on hit"),
                lines("§7Apply §cWeakness III §7(11s) on", "§7hit"),
                lines("§7Apply §cWeakness IV §7(16s) on hit"))));

        // Rare
        registry.register(new DescribedEnchant("explosive_chickens", "Devil Chicks!", BOW, RARE, levels(
                lines("§7Arrows spawn an explosive chicken"),
                lines("§7Arrows spawn many explosive", "§7chickens"),
                lines("§7Arrows spawn too many explosive", "§7chickens"))));
        registry.register(new DescribedEnchant("explosive_bow", "Explosive", BOW, RARE, levels(
                lines("§7Arrows go POP! (5s cooldown)"),
                lines("§7Arrows go BOOM! (3s cooldown)"),
                lines("§7Arrows go BOOM! (5s cooldown)"))));
        registry.register(new DescribedEnchant("lucky_shot", "Lucky Shot", BOW, RARE, levels(
                lines("§7§e2% §7chance for a shot to deal", "§7quadruple damage"),
                lines("§7§e5% §7chance for a shot to deal", "§7quadruple damage"),
                lines("§7§e10% §7chance for a shot to deal", "§7quadruple damage"))));
        registry.register(new DescribedEnchant("instant_shot", "Mega Longbow", BOW, RARE, levels(
                lines("§7One shot per second, this bow is", "§7automatically fully drawn and", "§7grants §aJump Boost II §7(2s)"),
                lines("§7One shot per second, this bow is", "§7automatically fully drawn and", "§7grants §aJump Boost III §7(2s)"),
                lines("§7One shot per second, this bow is", "§7automatically fully drawn and", "§7grants §aJump Boost IV §7(2s)"))));
        registry.register(new DescribedEnchant("pullbow", "Pullbow", BOW, RARE, levels(
                lines("§7Hitting a player pulls them toward", "§7you (8s cooldown per player)"),
                lines("§7Hitting a player pulls them and", "§7nearby players toward you (8s", "§7cooldown)"),
                lines("§7Hitting a player pulls them and", "§7nearby players toward you (8s", "§7cooldown)"))));
        registry.register(new DescribedEnchant("telebow", "Telebow", BOW, RARE, levels(
                lines("§7Sneak to shoot a teleportation", "§7arrow (90s cooldown, -3s per bow", "§7hit)"),
                lines("§7Sneak to shoot a teleportation", "§7arrow (45s cooldown, -3s per bow", "§7hit)"),
                lines("§7Sneak to shoot a teleportation", "§7arrow (20s cooldown, -3s per bow", "§7hit)"))));
        registry.register(new DescribedEnchant("bow_to_true_damage", "True Shot", BOW, RARE, levels(
                lines("§7Deal §c25% §7of arrow damage as", "§7true damage (ignores armor)"),
                lines("§7Deal §c35% + 0.25❤ §7of arrow", "§7damage as true damage (ignores", "§7armor)"),
                lines("§7Deal §c45% + 0.5❤ §7of arrow", "§7damage as true damage (ignores", "§7armor)"))));
        registry.register(new DescribedEnchant("volley", "Volley", BOW, RARE, levels(
                lines("§7Shoot §f3 arrows §7at once"),
                lines("§7Shoot §f4 arrows §7at once"),
                lines("§7Shoot §f5 arrows §7at once"))));
    }
}
