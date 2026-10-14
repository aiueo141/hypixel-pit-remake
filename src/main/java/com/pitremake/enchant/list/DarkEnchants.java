package com.pitremake.enchant.list;

import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.EnchantRegistry;

import static com.pitremake.enchant.DescribedEnchant.levels;
import static com.pitremake.enchant.DescribedEnchant.lines;
import static com.pitremake.enchant.EnchantCategory.DARK;
import static com.pitremake.enchant.EnchantRarity.COMMON;
import static com.pitremake.enchant.EnchantRarity.RARE;

/**
 * Dark Pants 専用の Dark エンチャント (レベルは I のみ)。
 * Somber は Tier I で必ず付き、Tier II でそれ以外から 1 つ付く。
 * 分類は pit.wiki/Dark_Pants、キーと説明文は pitpal.rocks のデータ。
 */
public final class DarkEnchants {

    /** Tier I で必ず付くエンチャント。 */
    public static final String SOMBER = "somber";
    /** これが付いた強化では Lives が増えない。 */
    public static final String HEARTRIPPER = "heartripper";

    private DarkEnchants() {
    }

    public static void registerAll(EnchantRegistry registry) {
        registry.register(new DescribedEnchant("somber", "Somber", DARK, COMMON, levels(
                lines("§7You are unaffected by mystical", "§7enchantments."))));
        registry.register(new DescribedEnchant("grim_reaper", "Grim Reaper", DARK, COMMON, levels(
                lines("§7Deal §c2❤ §7damage to players", "§7within §f10 blocks §7right before", "§7the blow that kills them.", "§5Receive -80% rewards from your kills"))));
        registry.register(new DescribedEnchant("hedge_fund", "Hedge Fund", DARK, COMMON, levels(
                lines("§7Gain §6+6g §7on kills. Gain", "§7§6+66g §7from killing players", "§7wearing §dTier I+ pants§7", "§7Dying costs you §650g"))));
        registry.register(new DescribedEnchant("lycanthropy", "Lycanthropy", DARK, COMMON, levels(
                lines("§7At night, gain §6+10% gold§7 &", "§7§b+10% XP §7from kills aswell as", "§7perma §cRegen II §7& §eSpeed I§7.", "§7§cCannot gain speed for a", "§cday after being worn."))));
        registry.register(new DescribedEnchant("mind_assault", "Mind Assault", DARK, COMMON, levels(
                lines("§7Deal §5-60% §7damage. Once", "§7applied, deal §c+0.75❤ §7damage", "§7per player with leather armor", "§7within §f11 blocks§7, up to §c+8❤", "§c§7damage"))));
        registry.register(new DescribedEnchant("misery", "Misery", DARK, COMMON, levels(
                lines("§7Deal §c+0.5❤ §7true damage against", "§7players wearing leather armor but", "§7take §c0.3❤§7"))));
        registry.register(new DescribedEnchant("needless_suffering", "Needless Suffering", DARK, COMMON, levels(
                lines("§7Respawn with §630❤ §7absorption.", "§cCannot heal!"))));
        registry.register(new DescribedEnchant("sanguisuge", "Sanguisuge", DARK, COMMON, levels(
                lines("§7Heal §c0.5❤ §7when hitting players", "§7wearing leather armor (2s cd).", "§7Heal doubled when §cVampire §7is", "§7equipped.", "§cRegeneration never heals."))));
        registry.register(new DescribedEnchant("spite", "Spite", DARK, COMMON, levels(
                lines("§7Deal §c+20% damage §7but receive", "§7§c+5% damage §7versus players", "§7wearing leather armor"))));
        registry.register(new DescribedEnchant("venom", "Combo: Venom", DARK, RARE, levels(
                lines("§7Every §ethird §7strike §apoisons", "§a§7enemies, disabling their mystic", "§7enchants for §512 seconds.", "§7Also §apoisons §7yourself!"))));
        registry.register(new DescribedEnchant("golden_handcuffs", "Golden Handcuffs", DARK, RARE, levels(
                lines("§7Gain §6+400% gold §7from bounties.", "§7The extra gold is shared with ALL", "§7players on the server.", "§7§o(min 30 players)"))));
        registry.register(new DescribedEnchant("heartripper", "Heartripper", DARK, RARE, levels(
                lines("§7Gain §c+1 bloodshard §7for each", "§7life you take from mystic items on", "§7kill. Accumulating §c27", "§cbloodshards §7consumes 2 lives of", "§7this item and grants §dfresh", "§dpants§7."))));
        registry.register(new DescribedEnchant("nostalgia", "Nostalgia", DARK, RARE, levels(
                lines("§7Receive §9-10% damage§7.", "§7Equipped diamond armor §bglows§7."))));
    }
}
