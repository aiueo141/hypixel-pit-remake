package com.pitremake.enchant.list;

import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.EnchantRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.pitremake.enchant.DescribedEnchant.levels;
import static com.pitremake.enchant.DescribedEnchant.lines;
import static com.pitremake.enchant.EnchantCategory.PANTS;
import static com.pitremake.enchant.EnchantRarity.COMMON;
import static com.pitremake.enchant.EnchantRarity.RARE;
import static com.pitremake.enchant.EnchantRarity.UNCOMMON;

/**
 * パンツ専用エンチャント。分類は pit.wiki/Mystic_Pants、キーと各レベルの説明文は pitpal.rocks のデータ。
 * 全アイテム共通のリソース系は {@link ResourceEnchants} にある。
 */
public final class PantsEnchants {

    public static final String PIT_MBA = "pit_mba";
    public static final String FRACTIONAL_RESERVE = "fractional_reserve";
    public static final String PAPARAZZI = "paparazzi";
    /** オークションでしか手に入らないエンチャント (pit.wiki/Auction)。Mystic Well の抽選には出ない。 */
    public static final Set<String> AUCTION_ONLY = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(PIT_MBA, FRACTIONAL_RESERVE, PAPARAZZI)));

    private PantsEnchants() {
    }

    public static void registerAll(EnchantRegistry registry) {
        // Common
        registry.register(new DescribedEnchant("less_damage_when_high_bounty", "Billy", PANTS, COMMON, levels(
                lines("§7Receive §9-2% §7damage per", "§61,000g bounty"),
                lines("§7Receive §9-3% §7damage per", "§61,000g bounty"),
                lines("§7Receive §9-4% §7damage per", "§61,000g bounty"))));
        registry.register(new DescribedEnchant("less_damage_on_grass", "Cricket", PANTS, COMMON, levels(
                lines("§7Receive §9-5% §7damage when you or", "§7your victims are standing on grass", "§aPerma §cRegen I §aon grass!"),
                lines("§7Receive §9-7% §7damage when you or", "§7your victims are standing on grass", "§aPerma §cRegen I §aon grass!"),
                lines("§7Receive §9-15% §7damage when you", "§7or your victims are standing on", "§7grass", "§aPerma §cRegen I §aon grass!"))));
        registry.register(new DescribedEnchant("less_damage_vs_bounties", "David and Goliath", PANTS, COMMON, levels(
                lines("§7Receive §9-15% §7damage from", "§7players with a bounty"),
                lines("§7Receive §9-25% §7damage from", "§7players with a bounty"),
                lines("§7Receive §9-40% §7damage from", "§7players with a bounty"))));
        registry.register(new DescribedEnchant("less_damage_vs_diamond_weapons", "Diamond Allergy", PANTS, COMMON, levels(
                lines("§7Receive §9-10% §7damage from", "§7diamond weapons"),
                lines("§7Receive §9-20% §7damage from", "§7diamond weapons"),
                lines("§7Receive §9-30% §7damage from", "§7diamond weapons"))));
        registry.register(new DescribedEnchant("less_damage_when_swimming", "McSwimmer", PANTS, COMMON, levels(
                lines("§7Receive §9-25% §7melee damage", "§7while swimming in water or lava"),
                lines("§7Receive §9-40% §7melee damage", "§7while swimming in water or lava"),
                lines("§7Receive §9-60% §7melee damage", "§7while swimming in water or lava"))));
        registry.register(new DescribedEnchant("less_damage_nearby_players", "\"Not\" Gladiator", PANTS, COMMON, levels(
                lines("§7Receive §9-1% §7damage per nearby", "§7player (max 10 players)"),
                lines("§7Receive §9-1.5% §7damage per", "§7nearby player (max 10 players)"),
                lines("§7Receive §9-2% §7damage per nearby", "§7player (max 10 players)"))));
        registry.register(new DescribedEnchant("damage_reduction", "Protection", PANTS, COMMON, levels(
                lines("§7Receive §9-4% §7damage"),
                lines("§7Receive §9-6% §7damage"),
                lines("§7Receive §9-10% §7damage"))));
        registry.register(new DescribedEnchant("less_damage_from_arrows", "Ring Armor", PANTS, COMMON, levels(
                lines("§7Receive §9-20% §7damage from", "§7arrows"),
                lines("§7Receive §9-40% §7damage from", "§7arrows"),
                lines("§7Receive §9-60% §7damage from", "§7arrows"))));

        // Resource (パンツ専用)
        registry.register(new DescribedEnchant("increase_armor_drops", "Lodbrok", PANTS, COMMON, levels(
                lines("§7Increases the chance for armor", "§7pieces to drop to 40% (normally", "§730%)"),
                lines("§7Increases the chance for armor", "§7pieces to drop to 55% (normally", "§730%)"),
                lines("§7Increases the chance for armor", "§7pieces to drop to 75% (normally", "§730%)")), true));
        registry.register(new DescribedEnchant("contract_rewards", "Negotiator", PANTS, COMMON, levels(
                lines("§7Earn §6+30% gold §7from contracts"),
                lines("§7Earn §6+60% gold §7from contracts"),
                lines("§7Earn §6+100% gold §7from contracts")), true));
        registry.register(new DescribedEnchant("increase_gold_pickup", "Pebble", PANTS, COMMON, levels(
                lines("§7Picked up gold rewards §6+10g"),
                lines("§7Picked up gold rewards §6+20g"),
                lines("§7Picked up gold grants §61❤ §7and", "§7rewards §6+30g")), true));
        registry.register(new DescribedEnchant("gold_break_obsidian", "Purple Gold", PANTS, COMMON, levels(
                lines("§7Gain §6+§67g §7from breaking", "§7obsidian"),
                lines("§7Gain §6+§611g §7and §cRegen III", "§c§7(2s) from breaking obsidian"),
                lines("§7Gain §6+§615g §7and §cRegen III", "§c§7(3s) from breaking obsidian")), true));
        registry.register(new DescribedEnchant("max_bounty_self_claim", "Self-checkout", PANTS, COMMON, levels(
                lines("§7Upon reaching a §6§65,000g", "§6§7bounty, clear it and gain", "§7§6+§62,000g§7. Consumes 1 life of", "§7this item"),
                lines("§7Upon reaching a §6§65,000g", "§6§7bounty, clear it and gain", "§7§6+§63,000g§7. Consumes 1 life of", "§7this item"),
                lines("§7Upon reaching a §6§65,000g", "§6§7bounty, clear it and gain", "§7§6+§65,000g§7. Consumes 1 life of", "§7this item")), true));

        // Uncommon
        registry.register(new DescribedEnchant("passive_health_regen", "Boo-boo", PANTS, UNCOMMON, levels(
                lines("§7Passively regain §c1❤ §7every 5", "§7seconds"),
                lines("§7Passively regain §c1❤ §7every 4", "§7seconds"),
                lines("§7Passively regain §c1❤ §7every 3", "§7seconds"))));
        registry.register(new DescribedEnchant("speed_when_hit_few_times", "Counter-Offensive", PANTS, UNCOMMON, levels(
                lines("§7Gain §eSpeed II §7(3s) when hit", "§7§e4 times §7by a player"),
                lines("§7Gain §eSpeed II §7(5s) when hit", "§7§e3 times §7by a player"),
                lines("§7Gain §eSpeed II §7(7s) when hit", "§7§e2 times §7by a player"))));
        registry.register(new DescribedEnchant("wood_blocks", "Creative", PANTS, UNCOMMON, levels(
                lines("§7Spawn with §f16 planks§7. Wood", "§7remains for 30 seconds. Gain §f+6", "§fblocks §7on kill"),
                lines("§7Spawn with §f32 planks§7. Wood", "§7remains for 30 seconds. Gain §f+12", "§fblocks §7on kill"),
                lines("§7Spawn with §f48 planks§7. Wood", "§7remains for 30 seconds. Gain §f+18", "§fblocks §7on kill"))));
        registry.register(new DescribedEnchant("power_against_crits", "Critically Funky", PANTS, UNCOMMON, levels(
                lines("§7Critical hits against you deal", "§7§980% §7of the damage they", "§7normally would"),
                lines("§7Critical hits against you deal", "§7§965% §7of the damage they", "§7normally would and empower your", "§7next strike for §c+14% §7damage"),
                lines("§7Critical hits against you deal", "§7§950% §7of the damage they", "§7normally would and empower your", "§7next strike for §c+30% §7damage"))));
        registry.register(new DescribedEnchant("superspeed_when_low", "Danger Close", PANTS, UNCOMMON, levels(
                lines("§7Gain §eSpeed III §7(3s) when", "§7reaching §c3❤ §7(10s cooldown)"),
                lines("§7Gain §eSpeed III §7(6s) when", "§7reaching §c4❤ §7(10s cooldown)"),
                lines("§7Gain §eSpeed III §7(9s) when", "§7reaching §c4❤ §7(10s cooldown)"))));
        registry.register(new DescribedEnchant("eggs", "Eggs", PANTS, UNCOMMON, levels(
                lines("§7Spawn with §a8 eggs§7. Gain §a+4", "§aeggs §7on kill"),
                lines("§7Spawn with §a8 eggs§7. Gain §a+8", "§aeggs §7on kill"),
                lines("§7Spawn with §a16 eggs§7. Gain §a+16", "§aeggs §7on kill"))));
        registry.register(new DescribedEnchant("refresh_speed_on_kill", "Electrolytes", PANTS, UNCOMMON, levels(
                lines("§7If you have §eSpeed §7on kill, add", "§7§e2 §7seconds to its duration.", "§7(Halved for Speed II+, Max 18s)"),
                lines("§7If you have §eSpeed §7on kill, add", "§7§e4 §7seconds to its duration.", "§7(Halved for Speed II+, Max 24s)"),
                lines("§7If you have §eSpeed §7on kill, add", "§7§e6 §7seconds to its duration.", "§7(Halved for Speed II+, Max 30s)"))));
        registry.register(new DescribedEnchant("overheal_enchant", "Excess", PANTS, UNCOMMON, levels(
                lines("§7Can hold §a+1 healing §7item"),
                lines("§7Can hold §a+2 healing §7item"),
                lines("§7Can hold §a+3 healing §7item"))));
        registry.register(new DescribedEnchant("absorption_on_kill", "Golden Heart", PANTS, UNCOMMON, levels(
                lines("§7Gain §6+0.5❤ §7absorption on kill", "§7(max §64❤§7)"),
                lines("§7Gain §6+1❤ §7absorption on kill", "§7(max §65❤§7)"),
                lines("§7Gain §6+2❤ §7absorption on kill", "§7(max §66❤§7)"))));
        registry.register(new DescribedEnchant("perma_speed", "Gotta go fast", PANTS, UNCOMMON, levels(
                lines("§7Move §e4% faster §7at all times"),
                lines("§7Move §e10% faster §7at all times"),
                lines("§7Move §e20% faster §7at all times"))));
        registry.register(new DescribedEnchant("higher_max_hp", "Hearts", PANTS, UNCOMMON, levels(
                lines("§7Increase your max health by", "§7§c0.25❤"),
                lines("§7Increase your max health by §c0.5❤"),
                lines("§7Increase your max health by §c1❤"))));
        registry.register(new DescribedEnchant("counter_bounty_hunter", "Hunt the Hunter", PANTS, UNCOMMON, levels(
                lines("§7Players using the §6Bounty Hunter", "§6§7perk only deal half their bonus", "§7damage against you"),
                lines("§7Players using the §6Bounty Hunter", "§6§7perk do not deal bonus damage", "§7against you"),
                lines("§7Players using the §6Bounty Hunter", "§6§7perk do not deal bonus damage", "§7against you and overall deal", "§7§9-20% §7damage to you"))));
        registry.register(new DescribedEnchant("resistance_when_low", "Last Stand", PANTS, UNCOMMON, levels(
                lines("§7Gain §9Resistance I §7(3 seconds)", "§7when reaching §c3❤"),
                lines("§7Gain §9Resistance II §7(4 seconds)", "§7when reaching §c3❤"),
                lines("§7Gain §9Resistance III §7(4", "§7seconds) when reaching §c3❤"))));
        registry.register(new DescribedEnchant("immune_true_damage", "Mirror", PANTS, UNCOMMON, levels(
                lines("§7You are immune to true damage"),
                lines("§7You do not take true damage and", "§7instead reflect §e25% §7of it to", "§7your attacker"),
                lines("§7You do not take true damage and", "§7instead reflect §e50% §7of it to", "§7your attacker"))));
        registry.register(new DescribedEnchant("regen_when_hit", "Peroxide", PANTS, UNCOMMON, levels(
                lines("§7Heal §c0.5❤ §7when hit (1.5s", "§7cooldown)"),
                lines("§7Gain §c0.7❤ §7when hit (1.5s", "§7cooldown)"),
                lines("§7Gain §c1❤ §7when hit (1.5s", "§7cooldown)"))));
        registry.register(new DescribedEnchant("thorns", "Prick", PANTS, UNCOMMON, levels(
                lines("§7Enemies hitting you receive", "§7§c0.25❤ §7true damage"),
                lines("§7Enemies hitting you receive", "§7§c0.375❤ §7true damage"),
                lines("§7Enemies hitting you receive §c0.5❤", "§c§7true damage"))));
        registry.register(new DescribedEnchant("respawn_with_absorption", "Respawn: Absorption", PANTS, UNCOMMON, levels(
                lines("§7Spawn with §65❤ §7absorption"),
                lines("§7Spawn with §610❤ §7absorption"),
                lines("§7Spawn with §615❤ §7absorption"))));
        registry.register(new DescribedEnchant("respawn_with_resistance", "Respawn: Resistance", PANTS, UNCOMMON, levels(
                lines("§7Respawn with §9Resistance I", "§9§7(20s)"),
                lines("§7Spawn with §9Resistance I §7(30s)"),
                lines("§7Spawn with §9Resistance II §7(40s)"))));
        registry.register(new DescribedEnchant("regen_speed_when_low", "Revitalize", PANTS, UNCOMMON, levels(
                lines("§7Gain §eSpeed I §7(5s) and §cRegen", "§cII §7(3s) when reaching §c3❤", "§c§7(45s cooldown)"),
                lines("§7Gain §eSpeed II §7(5s) and §cRegen", "§cIII §7(3s) when reaching §c3❤", "§c§7(45s cooldown)"),
                lines("§7Gain §eSpeed II §7(7s) and §cRegen", "§cIII §7(5s) when reaching §c3❤", "§c§7(45s cooldown)"))));
        registry.register(new DescribedEnchant("steaks_on_kill", "Steaks", PANTS, UNCOMMON, levels(
                lines("§7Gain a §2steak §7instead of golden", "§7apples on kill. §2Steaks §7are", "§7tasty and have good fibers for", "§7your muscles. Can hold up to 8", "§7steaks"),
                lines("§7Gain a §2steak §7instead of golden", "§7apples on kill. §2Steaks §7are", "§7tasty and have good fibers for", "§7your muscles. Can hold up to 10", "§7steaks"),
                lines("§7Gain a §2steak §7instead of golden", "§7apples on kill. §2Steaks §7are", "§7tasty and have good fibers for", "§7your muscles. Can hold up to 12", "§7steaks"))));
        registry.register(new DescribedEnchant("tnt", "TNT", PANTS, UNCOMMON, levels(
                lines("§7Spawn with §c1 TNT§7. §cTNT", "§c§7explodes after 1.5 seconds and", "§7deals §c1❤ §7in a 3 blocks radius.", "§7Gain §c+1 TNT §7on kill"),
                lines("§7Spawn with §c2 TNT§7. §cTNT", "§c§7explodes after 1.5 seconds and", "§7deals §c1.5❤ §7in a 3 blocks", "§7radius. Gain §c+1 TNT §7on kill"),
                lines("§7Spawn with §c3 TNT§7. §cTNT", "§c§7explodes after 1.5 seconds and", "§7deals §c2❤ §7in a 3 blocks radius.", "§7Gain §c+2 TNT §7on kill"))));

        // Rare
        registry.register(new DescribedEnchant("sneak_teleport", "Assassin", PANTS, RARE, levels(
                lines("§7Sneaking teleports you behind", "§7players bowing you. (10s cooldown)"),
                lines("§7Sneaking teleports you behind your", "§7attacker. (5s cooldown)"),
                lines("§7Sneaking teleports you behind your", "§7attacker. (3s cooldown)"))));
        registry.register(new DescribedEnchant("chance_dont_lose_life", "Divine Miracle", PANTS, RARE, levels(
                lines("§7§b15% chance §7retain the lives on", "§7your items on death"),
                lines("§7§b30% chance §7retain the lives on", "§7your items on death"),
                lines("§7§b45% chance §7retain the lives on", "§7your items on death"))));
        registry.register(new DescribedEnchant("escape_pod", "Escape Pod", PANTS, RARE, levels(
                lines("§7When hit below §c2 ❤§7, launch", "§7into the air dealing §c1❤ §7damage", "§7to nearby enemies and gaining", "§7§aRegen II §7(20s). Can launch", "§7once per life."),
                lines("§7When hit below §c2 ❤§7, launch", "§7into the air dealing §c2❤ §7damage", "§7to nearby enemies and gaining", "§7§aRegen III §7(25s). Can launch", "§7once per life."),
                lines("§7When hit below §c2 ❤§7, launch", "§7into the air dealing §c3❤ §7damage", "§7to nearby enemies and gaining", "§7§aRegen IV §7(30s). Can launch", "§7once per life."))));
        registry.register(new DescribedEnchant("regen_when_ooc", "Gomraw's Heart", PANTS, RARE, levels(
                lines("§7Regain all §c❤ §7when out of", "§7combat"),
                lines("§7Regain all §c❤ §7when out of", "§7combat. Gain §cRegen IV §7(1s)", "§7when entering combat"),
                lines("§7Regain all §c❤ §7when out of", "§7combat. Gain §cRegen IV §7(2s)", "§7when entering combat"))));
        registry.register(new DescribedEnchant("instaboom_tnt", "Instaboom", PANTS, RARE, levels(
                lines("§7Spawn with §c1 Instaboom TNT§7. It", "§7explodes instantly and deals", "§7§c0.25❤ §7to enemies in a 4 blocks", "§7radius. Gain §c+1 Instaboom TNT", "§c§7on kill"),
                lines("§7Spawn with §c1 Instaboom TNT§7. It", "§7explodes instantly and deals", "§7§c0.5❤ §7to enemies in a 4 blocks", "§7radius. Gain §c+2 Instaboom TNT", "§c§7on kill", "§7Consecutive uses §churt §7you!"),
                lines("§7Spawn with §c2 Instaboom TNT§7. It", "§7explodes instantly and deals", "§7§c0.75❤ §7to enemies in a 4 blocks", "§7radius. Gain §c+2 Instaboom TNT", "§c§7on kill", "§7Consecutive uses §churt §7you!"))));
        registry.register(new DescribedEnchant("martyrdom", "Martyrdom", PANTS, RARE, levels(
                lines("§7Leave a handful of §acreepers", "§a§7behind on death"),
                lines("§7Leave lots of §acreepers §7behind", "§7on death"),
                lines("§7Leave a ridiculous amount of", "§7§acreepers §7behind on death"))));
        registry.register(new DescribedEnchant("phoenix", "Phoenix", PANTS, RARE, levels(
                lines("§7Lethal attacks against you consume", "§7§c1 life §7of this item. Instead", "§7of dying, regenerate to full", "§7health."),
                lines("§7Lethal attacks against you consume", "§7§c1 life §7of this item. Instead", "§7of dying, heal to §afull HP§7 and", "§7gain a §c+0% §7damage (12s)", "§7buff§7.", "§7§o(Can revive once per life)"),
                lines("§7Lethal attacks against you consume", "§7§c1 life §7of this item. Instead", "§7of dying, regenerate to full", "§7health and gain a stacking §c+15%", "§c§7damage (10s) buff."))));
        registry.register(new DescribedEnchant("the_blob", "Pit Blob", PANTS, RARE, levels(
                lines("§7Kills respawn §aThe Blob§7. This", "§7slimy pet will follow you around", "§7and kill your enemies. §aThe Blob", "§a§7grows and gains health for every", "§7enemy you kill."),
                lines("§7Kills respawn §aThe Blob§7. This", "§7slimy pet will follow you around", "§7and kill your enemies. §aThe Blob", "§a§7grows and gains health for every", "§7enemy you kill."),
                lines("§7Kills respawn §aThe Blob§7. This", "§7slimy pet will follow you around", "§7and kill your enemies. §aThe Blob", "§a§7grows and gains health for every", "§7enemy you kill."))));
        registry.register(new DescribedEnchant("rgm", "Retro-Gravity Microcosm", PANTS, RARE, levels(
                lines("§7When a player hits you from", "§7above ground §e3 times §7in a row:", "§7You heal §c1.25❤"),
                lines("§7When a player hits you from", "§7above ground §e3 times §7in a row:", "§7You heal §c1.25❤", "§7Gain §c+1.5❤ §7damage vs them for 30s"),
                lines("§7When a player hits you from", "§7above ground §e3 times §7in a row:", "§7You heal §c1.25❤", "§7Gain §c+1.5❤ §7damage vs them for 30s", "§7They take §c0.5❤ §7true damage"))));
        registry.register(new DescribedEnchant("singularity", "Singularity", PANTS, RARE, levels(
                lines("§7Hits you receive deal at most §c2❤", "§c§7damage"),
                lines("§7Hits you receive deal at most", "§7§c1.25❤ §7damage"),
                lines("§7Hits you receive deal at most §c1❤", "§c§7damage"))));
        registry.register(new DescribedEnchant("solitude", "Solitude", PANTS, RARE, levels(
                lines("§7Receive §9-40% §7damage when only", "§7one other player is within 7", "§7blocks"),
                lines("§7Receive §9-50% §7damage when two", "§7or less players are within 7", "§7blocks"),
                lines("§7Receive §9-60% §7damage when two", "§7or less players are within 7", "§7blocks"))));
        registry.register(new DescribedEnchant("snowballs", "Snowballs", PANTS, RARE, levels(
                lines("§7Spawn with §b8 snowballs§7. Gain", "§7§b+8 snowballs §7on kill.", "§7§bSnowballs §7apply Slowness I", "§7(3s) and deal §c1 §7true damage", "§7after §b5 §7hits in a row"),
                lines("§7Spawn with §b8 snowballs§7. Refill", "§7on kill. §bSnowballs §7apply", "§7Slowness I (4s) and deal §c1", "§c§7true damage after §b4 §7hits in", "§7a row"),
                lines("§7Spawn with §b16 snowballs§7.", "§7Refill on kill. §bSnowballs", "§b§7apply Slowness I (5s) and deal", "§7§c1 §7true damage after §b3 §7hits", "§7in a row"))));
        registry.register(new DescribedEnchant("snowmen", "Snowmen Army", PANTS, RARE, levels(
                lines("§7Spawn with §b3 scoops§7. Gain §b+1", "§bscoop §7on kill. Placing the scoop", "§7will consume 3 of them to spawn a", "§7Snowman defending you for §a30", "§a§7seconds. Can hold up to §a6", "§a§bscoops§7."),
                lines("§7Spawn with §b3 scoops§7. Gain §b+2", "§bscoops §7on kill. Placing the", "§7scoop will consume 3 of them to", "§7spawn a Snowman defending you for", "§7§a60 §7seconds. Can hold up to §a9", "§a§bscoops§7."),
                lines("§7Spawn with §b3 scoops§7. Gain §b+3", "§bscoops §7on kill. Placing the", "§7scoop will consume 3 of them to", "§7spawn a Snowman defending you for", "§7§a90 §7seconds. Can hold up to", "§7§a12 §bscoops§7."))));
        registry.register(new DescribedEnchant("wolf_pack", "Wolf Pack", PANTS, RARE, levels(
                lines("§7Spawn a §cWolf §7every 4 kills.", "§7Can have up to §c5 §7wolves at", "§7once"),
                lines("§7Spawn a §cWolf §7every 3 kills.", "§7Can have up to §c7 §7wolves at", "§7once"),
                lines("§7Spawn a §cWolf §7every 3 kills.", "§7Can have up to §c9 §7wolves at", "§7once"))));

        // オークションでしか手に入らない (Mystic Well では付かない: {@link #AUCTION_ONLY})。説明文は pitpal.rocks
        registry.register(new DescribedEnchant(PIT_MBA, "Pit MBA", PANTS, COMMON, levels(
                lines("§7Receive §6+10% gold §7and §b+10%", "§bXP §7on kill when there are at", "§7least 5 players within 15 blocks"),
                lines("§7Receive §6+30% gold §7and §b+20%", "§bXP §7on kill when there are at", "§7least 5 players within 15 blocks"),
                lines("§7Receive §6+50% gold §7and §b+30%", "§bXP §7on kill when there are at", "§7least 5 players within 15 blocks")), true));
        registry.register(new DescribedEnchant(FRACTIONAL_RESERVE, "Fractional Reserve", PANTS, COMMON, levels(
                lines("§7Receive §9-1% damage §7per", "§7§610,000g §7you have (§9-15%§7", "§7max)"),
                lines("§7Receive §9-1% damage §7per", "§7§610,000g §7you have (§9-21%§7", "§7max)"),
                lines("§7Receive §9-1% damage §7per", "§7§610,000g §7you have (§9-30%§7", "§7max)"))));
        // 効果は SpireEvent (Major Event の終わりのボーナスを増やし、パンツの Lives を 2 減らす)
        registry.register(new DescribedEnchant(PAPARAZZI, "Paparazzi", PANTS, RARE, levels(
                lines("§7Earn §d+50% major event §7bonus", "§7rewards. Consume §c2 lives §7of", "§7this item per event."),
                lines("§7Earn §d+100% major event §7bonus", "§7rewards. Consume §c2 lives §7of", "§7this item per event."),
                lines("§7Earn §d+150% major event §7bonus", "§7rewards. Consume §c2 lives §7of", "§7this item per event."))));
    }
}
