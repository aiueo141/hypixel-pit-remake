package com.pitremake.enchant.list;

import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.EnchantRegistry;

import static com.pitremake.enchant.DescribedEnchant.levels;
import static com.pitremake.enchant.DescribedEnchant.lines;
import static com.pitremake.enchant.EnchantCategory.RAGE;
import static com.pitremake.enchant.EnchantRarity.COMMON;

/**
 * Rage Pants 専用の Rage エンチャント。Rage Pants を Tier I に強化したときだけ付く。
 * 分類は pit.wiki/Rage_Pants、キーと各レベルの説明文は pitpal.rocks のデータ。
 */
public final class RageEnchants {

    private RageEnchants() {
    }

    public static void registerAll(EnchantRegistry registry) {
        registry.register(new DescribedEnchant("ace_of_spades", "Ace of Spades", RAGE, COMMON, levels(
                lines("§7Receive §c-0.5❤ §7from §bCombat", "§bSpades"),
                lines("§7Receive §c-1❤ §7from §bCombat", "§bSpades"),
                lines("§7Receive §c-1.5❤ §7from §bCombat", "§bSpades"))));
        registry.register(new DescribedEnchant("brakes", "Brakes!", RAGE, COMMON, levels(
                lines("§7Downgrade the speed effect of your", "§7foes to §eSpeed I §7on hit"),
                lines("§7Downgrade the speed effect of your", "§7foes to §eSpeed I §7on hit.", "§7Receive §9-5% §7damage from", "§7players above you"),
                lines("§7Downgrade the speed effect of your", "§7foes to §eSpeed I §7on hit.", "§7Receive §9-10% §7damage from", "§7players above you"))));
        registry.register(new DescribedEnchant("breaching_charge", "Combo: Breaching Charge", RAGE, COMMON, levels(
                lines("§7Every §efourth §7strike clears out", "§7§eResistance §7from your target"),
                lines("§7Every §ethird §7strike clears out", "§7§eResistance §7from your target"),
                lines("§7Every §esecond §7strike clears out", "§7§eResistance §7from your target"))));
        registry.register(new DescribedEnchant("do_it_like_the_french", "Do it like the French", RAGE, COMMON, levels(
                lines("§7Deal §c+0.5❤ §7to players wearing", "§7Golden Helmets"),
                lines("§7Deal §c+1❤ §7to players wearing", "§7Golden Helmets"),
                lines("§7Deal §c+1.5❤ §7to players wearing", "§7Golden Helmets"))));
        // pitpal に説明文が無いため pit.wiki の説明から作成
        registry.register(new DescribedEnchant("heigh_ho", "Heigh-Ho", RAGE, COMMON, levels(
        lines("§7Deal §c+1 §7damage §7against", "§fMirror §7wearers"),
        lines("§7Deal §c+2 §7damage §7against", "§fMirror §7wearers"),
        lines("§7Deal §c+3 §7damage §7against", "§fMirror §7wearers"))));
        registry.register(new DescribedEnchant("new_deal", "New Deal", RAGE, COMMON, levels(
                lines("§7You are immune to §6Billionaire"),
                lines("§7Receive §9-4% §7damage and you are", "§7immune to §6Billionaire"),
                lines("§7Receive §9-8% §7damage and you are", "§7immune to §6Billionaire"))));
        registry.register(new DescribedEnchant("really_toxic", "Really Toxic", RAGE, COMMON, levels(
                lines("§7Strikes apply 1 stack of", "§7§aToxicity§7. Stacks last §430s", "§4§7and reduces all healing by", "§7§a2%§7, up to §a20%"),
                lines("§7Strikes apply 1 stack of", "§7§aToxicity§7. Stacks last §430s", "§4§7and reduces all healing by", "§7§a2%§7, up to §a30%"),
                lines("§7Strikes apply 1 stack of", "§7§aToxicity§7. Stacks last §430s", "§4§7and reduces all healing by", "§7§a2%§7, up to §a40%"))));
        registry.register(new DescribedEnchant("sybil", "Sybil", RAGE, COMMON, levels(
                lines("§7You count for §42 §7players for", "§7enchants/perks which count players"),
                lines("§7You count for §43 §7players for", "§7enchants/perks which count players"),
                lines("§7You count for §44 §7players for", "§7enchants/perks which count players"))));

        // 以下 2 つは見た目だけ RARE (Lore に "RARE!" が付くが、ルール上は COMMON)
        registry.register(new DescribedEnchant("regularity", "Regularity", RAGE, COMMON, levels(
                lines("§7If the final damage of your strike", "§7deals less than §c1.1❤ §7damage, strike", "§7again in §a0.1s §7for §c50% §7damage.", "§8Extra strike doesn't combo."),
                lines("§7If the final damage of your strike", "§7deals less than §c1.3❤ §7damage, strike", "§7again in §a0.1s §7for §c60% §7damage.", "§8Extra strike doesn't combo."),
                lines("§7If the final damage of your strike", "§7deals less than §c1.5❤ §7damage, strike", "§7again in §a0.1s §7for §c75% §7damage.", "§8Extra strike doesn't combo.")))
                .withRareTag());
        // pitpal に説明文が無いため pit.wiki の説明から作成
        registry.register(new DescribedEnchant("think_of_the_people", "Think of the People", RAGE, COMMON, levels(
        lines("§7People within §e10 §7blocks §7are", "§7immune from §cExecute §7and", "§ePerun's Wrath."),
        lines("§7People within §e10 §7blocks §7are", "§7immune from §cExecute §7and", "§ePerun's Wrath. §7The People also", "§7receive §9-10% §7damage from", "§7attackers with those enchants."),
        lines("§7People within §e10 §7blocks §7are", "§7immune from §cExecute §7and", "§ePerun's Wrath. §7The People also", "§7receive §9-20% §7damage from", "§7attackers with those enchants.")))
                .withRareTag());
    }
}
