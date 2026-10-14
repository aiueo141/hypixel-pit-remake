package com.pitremake.enchant.list;

import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.EnchantRegistry;

import static com.pitremake.enchant.DescribedEnchant.levels;
import static com.pitremake.enchant.DescribedEnchant.lines;
import static com.pitremake.enchant.EnchantCategory.RESOURCE;
import static com.pitremake.enchant.EnchantCategory.SWORD;
import static com.pitremake.enchant.EnchantRarity.COMMON;

/**
 * リソース (ゴールド・XP などが増える) 系エンチャント。Tier I から出るので COMMON 扱い。
 * 分類は pit.wiki/Mystic_Sword、キーと各レベルの説明文は pitpal.rocks のデータ。
 * pitpal で剣専用 (Type: sword) のものは SWORD 系統 + リソース扱い (resource = true)、
 * 全アイテム共通 (Type: any) のものは RESOURCE 系統にする。
 */
public final class ResourceEnchants {

    private ResourceEnchants() {
    }

    public static void registerAll(EnchantRegistry registry) {
        registry.register(new DescribedEnchant("streak_xp", "Sweaty", RESOURCE, COMMON, levels(
                lines("§7§b+20% §7XP from streak XP bonus"),
                lines("§7Earn §b+40% XP §7from streak XP", "§7bonus and §b+50 max XP §7per kill"),
                lines("§7Earn §b+60% XP §7from streak XP", "§7bonus and §b+100 max XP §7per kill"))));
        registry.register(new DescribedEnchant("xp_boost", "XP Boost", RESOURCE, COMMON, levels(
                lines("§7Earn §b+10% XP §7from kills"),
                lines("§7Earn §b+20% XP §7from kills"),
                lines("§7Earn §b+30% XP §7from kills"))));
        registry.register(new DescribedEnchant("xp_per_kill", "XP Bump", RESOURCE, COMMON, levels(
                lines("§7Earn §b+2 XP §7from kills"),
                lines("§7Earn §b+4 XP §7from kills"),
                lines("§7Earn §b+6 XP §7from kills"))));
        registry.register(new DescribedEnchant("gold_per_crit", "Critically Rich", RESOURCE, COMMON, levels(
                lines("§7Earn §6+§62g §7per critical strike"),
                lines("§7Earn §6+§64g §7per critical strike"),
                lines("§7Earn §6+§66g §7per critical strike"))));
        registry.register(new DescribedEnchant("gold_boost", "Gold Boost", RESOURCE, COMMON, levels(
                lines("§7Earn §6+15% gold (g) §7from kills"),
                lines("§7Earn §6+30% gold (g) §7from kills"),
                lines("§7Earn §6+45% gold (g) §7from kills"))));
        registry.register(new DescribedEnchant("gold_per_kill", "Gold Bump", RESOURCE, COMMON, levels(
                lines("§7Earn §6+4g §7per kill"),
                lines("§7Earn §6+8g §7per kill"),
                lines("§7Earn §6+12g §7per kill"))));
        registry.register(new DescribedEnchant("gold_strictly_kills", "Moctezuma", RESOURCE, COMMON, levels(
                lines("§7Earn §6+7g §7on kill (assists", "§7excluded)"),
                lines("§7Earn §6+12g §7on kill (assists", "§7excluded)"),
                lines("§7Earn §6+18g §7on kill (assists", "§7excluded)"))));
        registry.register(new DescribedEnchant("pickpocket", "Pitpocket", SWORD, COMMON, levels(
                lines("§7Steal §6§615g §7on melee hit (25s", "§7cooldown)"),
                lines("§7Steal §6§620g §7on melee hit (20s", "§7cooldown)"),
                lines("§7Steal §6§625g §7on melee hit (13s", "§7cooldown)")), true));
        registry.register(new DescribedEnchant("gold_per_diamond_piece", "Sierra", SWORD, COMMON, levels(
                lines("§7Earn §6+§630g §7per §bdiamond", "§b§7pieces your victims wear"),
                lines("§7Earn §6+§660g §7per §bdiamond", "§b§7pieces your victims wear"),
                lines("§7Earn §6+§690g §7per §bdiamond", "§b§7pieces your victims wear")), true));
        registry.register(new DescribedEnchant("gold_per_hit", "Strike Gold", RESOURCE, COMMON, levels(
                lines("§7Earn §6+1g §7per hit (1s cooldown)"),
                lines("§7Earn §6+2g §7per hit (1s cooldown)"),
                lines("§7Earn §6+3g §7per hit (1s cooldown)"))));
        registry.register(new DescribedEnchant("pants_radar", "Pants Radar", RESOURCE, COMMON, levels(
                lines("§7Pants, golden swords and enchanted", "§7bows drop §d+30% §7more frequently"),
                lines("§7Pants, golden swords and enchanted", "§7bows drop §d+60% §7more frequently"),
                lines("§7Pants, golden swords and enchanted", "§7bows drop §d+90% §7more frequently"))));
    }
}
