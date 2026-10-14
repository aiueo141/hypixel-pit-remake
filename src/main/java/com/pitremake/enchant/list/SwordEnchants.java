package com.pitremake.enchant.list;

import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.EnchantRegistry;

import static com.pitremake.enchant.DescribedEnchant.levels;
import static com.pitremake.enchant.DescribedEnchant.lines;
import static com.pitremake.enchant.EnchantCategory.SWORD;
import static com.pitremake.enchant.EnchantRarity.COMMON;
import static com.pitremake.enchant.EnchantRarity.RARE;
import static com.pitremake.enchant.EnchantRarity.UNCOMMON;

/**
 * 剣専用エンチャント。分類は pit.wiki/Mystic_Sword、キーと各レベルの説明文は pitpal.rocks のデータ。
 */
public final class SwordEnchants {

    private SwordEnchants() {
    }

    public static void registerAll(EnchantRegistry registry) {
        // Common
        registry.register(new DescribedEnchant("melee_damage_vs_bows", "Beat the Spammers", SWORD, COMMON, levels(
                lines("§7Deal §c+10% §7damage vs. players", "§7holding a bow"),
                lines("§7Deal §c+25% §7damage vs. players", "§7holding a bow"),
                lines("§7Deal §c+40% §7damage vs. players", "§7holding a bow"))));
        registry.register(new DescribedEnchant("melee_damage_vs_bounties", "Bounty Reaper", SWORD, COMMON, levels(
                lines("§7Deal §c+7% §7damage vs. players", "§7with a bounty"),
                lines("§7Deal §c+15% §7damage vs. players", "§7with a bounty"),
                lines("§7Deal §c+25% §7damage vs. players", "§7with a bounty"))));
        registry.register(new DescribedEnchant("melee_combo_damage", "Combo: Damage", SWORD, COMMON, levels(
                lines("§7Every §efourth §7strike deals", "§7§c+20% §7damage"),
                lines("§7Every §ethird §7strike deals", "§7§c+30% §7damage"),
                lines("§7Every §ethird §7strike deals", "§7§c+45% §7damage"))));
        registry.register(new DescribedEnchant("melee_damage_vs_diamond", "Diamond Stomp", SWORD, COMMON, levels(
                lines("§7Deal §c+6% §7damage vs. players", "§7wearing diamond armor"),
                lines("§7Deal §c+12% §7damage vs. players", "§7wearing diamond armor"),
                lines("§7Deal §c+25% §7damage vs. players", "§7wearing diamond armor"))));
        registry.register(new DescribedEnchant("melee_damage_vs_leather", "Fancy Raider", SWORD, COMMON, levels(
                lines("§7Deal §c+5% §7damage vs. players", "§7wearing leather armor"),
                lines("§7Deal §c+9% §7damage vs. players", "§7wearing leather armor"),
                lines("§7Deal §c+16% §7damage vs. players", "§7wearing leather armor"))));
        registry.register(new DescribedEnchant("melee_damage_when_on_grass", "Grasshopper", SWORD, COMMON, levels(
                lines("§7Deal §c+5% §7damage when you or", "§7your victim are standing on grass"),
                lines("§7Deal §c+9% §7damage when you or", "§7your victim are standing on grass"),
                lines("§7Deal §c+15% §7damage when you or", "§7your victim are standing on grass"))));
        registry.register(new DescribedEnchant("melee_damage_when_absorption", "Gold and Boosted", SWORD, COMMON, levels(
                lines("§7Deal §c+5% §7damage when you have", "§7absorption hearts"),
                lines("§7Deal §c+9% §7damage when you have", "§7absorption hearts"),
                lines("§7Deal §c+15% §7damage when you have", "§7absorption hearts"))));
        registry.register(new DescribedEnchant("melee_damage_vs_high_hp", "King Buster", SWORD, COMMON, levels(
                lines("§7Deal §c+7% §7damage vs. players", "§7above 50% HP"),
                lines("§7Deal §c+13% §7damage vs. players", "§7above 50% HP"),
                lines("§7Deal §c+20% §7damage vs. players", "§7above 50% HP"))));
        registry.register(new DescribedEnchant("melee_damage_when_low", "Pain Focus", SWORD, COMMON, levels(
                lines("§7Deal §c+1% §7damage per §c❤", "§c§7you're missing"),
                lines("§7Deal §c+2% §7damage per §c❤", "§c§7you're missing"),
                lines("§7Deal §c+5% §7damage per §c❤", "§c§7you're missing"))));
        registry.register(new DescribedEnchant("melee_damage_vs_low_hp", "Punisher", SWORD, COMMON, levels(
                lines("§7Deal §c+6% §7damage vs. players", "§7below 50% HP"),
                lines("§7Deal §c+12% §7damage vs. players", "§7below 50% HP"),
                lines("§7Deal §c+18% §7damage vs. players", "§7below 50% HP"))));
        registry.register(new DescribedEnchant("melee_damage_when_close_low_players", "Shark", SWORD, COMMON, levels(
                lines("§7Deal §c+2% §7damage per other", "§7player below §c6❤ §7within 12", "§7blocks"),
                lines("§7Deal §c+4% §7damage per other", "§7player below §c6❤ §7within 12", "§7blocks"),
                lines("§7Deal §c+7% §7damage per other", "§7player below §c6❤ §7within 12", "§7blocks"))));
        registry.register(new DescribedEnchant("plain_melee_damage", "Sharp", SWORD, COMMON, levels(
                lines("§7Deal §c+4% §7melee damage"),
                lines("§7Deal §c+7% §7melee damage"),
                lines("§7Deal §c+12% §7melee damage"))));
        registry.register(new DescribedEnchant("combo_xp", "Combo: XP", SWORD, COMMON, levels(
                lines("§7Every §efifth §7strike rewards", "§7§b+20 XP"),
                lines("§7Every §efifth §7strike rewards", "§7§b+28 XP"),
                lines("§7Every §efifth §7strike rewards", "§7§b+36 XP"))));

        // Uncommon
        registry.register(new DescribedEnchant("melee_crit_midair", "Berserker", SWORD, UNCOMMON, levels(
                lines("§7You can now critical hit on the", "§7ground. §a12% chance §7to crit for", "§7§c50% extra §7damage"),
                lines("§7You can now critical hit on the", "§7ground. §a20% chance §7to crit for", "§7§c50% extra §7damage"),
                lines("§7You can now critical hit on the", "§7ground. §a30% chance §7to crit for", "§7§c50% extra §7damage"))));
        registry.register(new DescribedEnchant("increased_blocking", "Bruiser", SWORD, UNCOMMON, levels(
                lines("§7Blocking with your swords reduces", "§7received damage by §c0.5❤"),
                lines("§7Blocking with your swords reduces", "§7received damage by §c1❤"),
                lines("§7Blocking with your swords reduces", "§7received damage by §c2❤"))));
        registry.register(new DescribedEnchant("blocking_cancels_projectiles", "Bullet Time", SWORD, UNCOMMON, levels(
                lines("§7Blocking destroys arrows that hit", "§7you"),
                lines("§7Blocking destroys arrows hitting", "§7you. Destroying arrows this way", "§7heals §c1❤"),
                lines("§7Blocking destroys arrows hitting", "§7you. Destroying arrows this way", "§7heals §c1.5❤"))));
        registry.register(new DescribedEnchant("melee_combo_heal", "Combo: Heal", SWORD, UNCOMMON, levels(
                lines("§7Every §efourth §7strike heals", "§7§c0.4❤ §7and grants §60.4❤", "§6§7absorption"),
                lines("§7Every §efourth §7strike heals", "§7§c0.8❤ §7and grants §60.8❤", "§6§7absorption"),
                lines("§7Every §efourth §7strike heals", "§7§c1.2❤ §7and grants §61.2❤", "§6§7absorption"))));
        registry.register(new DescribedEnchant("melee_combo_speed", "Combo: Swift", SWORD, UNCOMMON, levels(
                lines("§7Every §efourth §7strike gain", "§7§eSpeed I §7(3s)"),
                lines("§7Every §ethird §7strike gain", "§7§eSpeed II §7(4s)"),
                lines("§7Every §ethird §7strike gain", "§7§eSpeed II §7(5s)"))));
        registry.register(new DescribedEnchant("resistance_on_kill", "Counter-Janitor", SWORD, UNCOMMON, levels(
                lines("§7Gain §eResistance I §7(2s§7) on", "§7kill"),
                lines("§7Gain §eResistance I §7(3s§7) on", "§7kill"),
                lines("§7Gain §eResistance I §7(5s§7) on", "§7kill"))));
        registry.register(new DescribedEnchant("melee_weakness", "Crush", SWORD, UNCOMMON, levels(
                lines("§7Strikes apply §cWeakness V", "§c§7(lasts 0.2s, 2s cooldown)"),
                lines("§7Strikes apply §cWeakness VI", "§c§7(lasts 0.4s, 2s cooldown)"),
                lines("§7Strikes apply §cWeakness VII", "§c§7(lasts 0.5s, 2s cooldown)"))));
        registry.register(new DescribedEnchant("melee_strike_after_block", "Duelist", SWORD, UNCOMMON, levels(
                lines("§7Blocking two hits from the same", "§7player empowers your next strike", "§7against them for §c+20% §7damage", "§7and heals §c0.5❤"),
                lines("§7Blocking two hits from the same", "§7player empowers your next strike", "§7against them for §c+40% §7damage", "§7and heals §c1❤"),
                lines("§7Blocking two hits from the same", "§7player empowers your next strike", "§7against them for §c+75% §7damage", "§7and heals §c1.5❤"))));
        registry.register(new DescribedEnchant("melee_heal_on_kill", "Guts", SWORD, UNCOMMON, levels(
                lines("§7Heal §c0.25❤ §7on kill"),
                lines("§7Heal §c0.5❤ §7on kill"),
                lines("§7Heal §c1❤ §7on kill"))));
        registry.register(new DescribedEnchant("melee_heal_on_hit", "Lifesteal", SWORD, UNCOMMON, levels(
                lines("§7Heal for §c4% §7of damage dealt up", "§7to §c1.5❤"),
                lines("§7Heal for §c8% §7of damage dealt up", "§7to §c1.5❤"),
                lines("§7Heal for §c13% §7of damage dealt up", "§7to §c1.5❤"))));
        registry.register(new DescribedEnchant("melee_avenge", "Revengeance", SWORD, UNCOMMON, levels(
                lines("§7Deal §c+8% §7damage vs. the last", "§7player who killed you"),
                lines("§7Deal §c+15% §7damage vs. the last", "§7player who killed you"),
                lines("§7Deal §c+25% §7damage vs. the last", "§7player who killed you"))));
        registry.register(new DescribedEnchant("etspeed_on_kill", "Speedy Kill", SWORD, UNCOMMON, levels(
                lines("§7Gain §eSpeed I §7(4s) on kill"),
                lines("§7Gain §eSpeed I §7(7s) on kill"),
                lines("§7Gain §eSpeed I §7(12s) on kill"))));

        // Rare
        registry.register(new DescribedEnchant("melee_literally_p2w", "Billionaire", SWORD, RARE, levels(
                lines("§7Hits with this swords deal", "§c+33.3% §cdamage §7but cost §6100g"),
                lines("§7Hits with this swords deal", "§c66.7% §cdamage §7but cost §6200g"),
                lines("§7Hits with this swords deal", "§c+100% §cdamage §7but cost §6350g"))));
        registry.register(new DescribedEnchant("melee_lightning", "Combo: Perun's Wrath", SWORD, RARE, levels(
                lines("§7Each §efifth §7hit strikes", "§7§elightning §7for §c1.5❤§7.", "§7§oLightning deals true damage"),
                lines("§7Each §efourth §7hit strikes", "§7§elightning §7for §c2❤§7.", "§7§oLightning deals true damage"),
                lines("§7Every §efourth §7hit strikes", "§7§elightning §7for §c1❤ §7+ §c1❤", "§c§7per §bdiamond piece §7on your", "§7victim.", "§7§oLightning deals true damage"))));
        registry.register(new DescribedEnchant("melee_stun", "Combo: Stun", SWORD, RARE, levels(
                lines("§7The §efifth §7strike on an enemy", "§7stuns them for 0.5 seconds", "§7§o(Can only be stunned every 8s)"),
                lines("§7The §efifth §7strike on an enemy", "§7stuns them for 0.8 seconds", "§7§o(Can only be stunned every 8s)"),
                lines("§7The §efifth §7strike on an enemy", "§7stuns them for 1.5 seconds"))));
        registry.register(new DescribedEnchant("melee_execute", "Executioner", SWORD, RARE, levels(
                lines("§7Hitting an enemy to below §c1❤", "§c§7instantly kills them"),
                lines("§7Hitting an enemy to below §c1.5❤", "§c§7instantly kills them"),
                lines("§7Hitting an enemy to below §c2❤", "§c§7instantly kills them"))));
        registry.register(new DescribedEnchant("melee_gamble", "Gamble", SWORD, RARE, levels(
                lines("§7§d50% chance §7to deal §c1❤ §7true", "§7damage to whoever you hit, or to", "§7yourself"),
                lines("§7§d50% chance §7to deal §c2❤ §7true", "§7damage to whoever you hit, or to", "§7yourself"),
                lines("§7§d50% chance §7to deal §c3❤ §7true", "§7damage to whoever you hit, or to", "§7yourself"))));
        registry.register(new DescribedEnchant("melee_healer", "Healer", SWORD, RARE, levels(
                lines("§7Your hits §aheal §7you for §c0.5❤", "§7and them for §c2❤ §7(1s cd)"),
                lines("§7Your hits §aheal §7you for §c0.75❤", "§7and them for §c4❤ §7(1s cd)"),
                lines("§7Your hits §aheal §7you for §c1❤", "§7and them for §c5❤ §7(1s cd)"))));
        registry.register(new DescribedEnchant("melee_bleed", "Hemorrhage", SWORD, RARE, levels(
                lines("§7Strikes bleed enemies for §c4s§7,", "§7stopping them from gaining", "§7§6Absorption ❤ §7and slowing them.", "§7(6s cooldown)"),
                lines("§7Strikes bleed enemies for §c4s§7,", "§7stopping them from gaining", "§7§6Absorption ❤ §7and slowing them.", "§7(4s cooldown)"),
                lines("§7Strikes bleed enemies for §c5s§7,", "§7stopping them from gaining", "§7§6Absorption ❤ §7and slowing them.", "§7(2s cooldown)"))));
        registry.register(new DescribedEnchant("melee_knockback", "Knockback", SWORD, RARE, levels(
                lines("§7Increases knockback taken by", "§7enemies by §f3 blocks"),
                lines("§7Increases knockback taken by", "§7enemies by §f6 blocks"),
                lines("§7Increases knockback taken by", "§7enemies by §f9 blocks"))));
        registry.register(new DescribedEnchant("melee_speed_on_hit", "Speedy Hit", SWORD, RARE, levels(
                lines("§7Gain Speed I for §e5s §7on hit (3s", "§7cooldown)"),
                lines("§7Gain Speed I for §e7s §7on hit (2s", "§7cooldown)"),
                lines("§7Gain Speed I for §e9s §7on hit (1s", "§7cooldown)"))));
        registry.register(new DescribedEnchant("melee_launch", "The Punch", SWORD, RARE, levels(
                lines("§7Hitting a player launches them up", "§7in the air (30s cooldown)"),
                lines("§7Hitting a player launches them up", "§7in the air (25s cooldown)"),
                lines("§7Hitting a player launches them up", "§7in the air (20s cooldown)"))));
    }
}
