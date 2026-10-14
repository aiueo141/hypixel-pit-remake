package com.pitremake.perk;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Upgrades の村人で選ぶパーク (録画 2026-10-14_08-53-15.mcrc の "Choose a perk" の位置・アイコン・説明文・値段)。
 * Renown パークのアイコン・必要 Renown・必要レベルは同じ録画の "Renown Shop - Perks" から、
 * 必要レベル・必要プレステージは pit.wiki の Perks / Renown から。
 * <p>
 * available = false はゴールドインゴットの仕組みが無いので効果を作れないもの (Trickle-down)。
 */
public enum Perk {
    GOLDEN_HEADS("Golden Heads", 10, Material.SKULL_ITEM, 3, 10, 500,
            "§7Golden apples you earn turn into", "§7§6Golden Heads§7."),
    FISHING_ROD("Fishing Rod", 11, Material.FISHING_ROD, 0, 10, 1000,
            "§7Spawn with a fishing rod."),
    LAVA_BUCKET("Lava Bucket", 12, Material.LAVA_BUCKET, 0, 10, 1000,
            "§7Spawn with a lava bucket."),
    STRENGTH_CHAINING("Strength-Chaining", 13, Material.REDSTONE, 0, 20, 2000,
            "§7§c+8% damage §7for 7s stacking on kill."),
    SAFETY_FIRST("Safety First", 14, Material.CHAINMAIL_HELMET, 0, 20, 3000,
            "§7Spawn with a helmet."),
    BARBARIAN("Barbarian", 15, Material.IRON_AXE, 0, 30, 3000, 2, 12, 10,
            "§7Replaces your sword with a", "§7stronger axe."),
    MINEMAN("Mineman", 16, Material.COBBLESTONE, 0, 30, 3000,
            "§7Spawn with §f24 cobblestone §7and a", "§7diamond pickaxe.", "", "§7+§f3 blocks §7on kill."),
    BONK("Bonk!", 19, Material.ANVIL, 0, 35, 2000,
            "§7The first hit you receive from a", "§7player is blocked and grants", "§7§9Resistance I §7(1s)."),
    TRICKLE_DOWN("Trickle-down", 20, Material.GOLD_INGOT, 0, 40, 1000, false,
            "§7Gold ingots reward §6+§610g§7 and heal §c2❤§7."),
    LUCKY_DIAMOND("Lucky Diamond", 21, Material.DIAMOND, 0, 40, 4000,
            "§730% chance to upgrade dropped", "§7armor pieces from kills to §bdiamond§7.", "",
            "§7Upgraded pieces warp to your", "§7inventory."),
    SPAMMER("Spammer", 22, Material.BOW, 0, 40, 4000,
            "§7Get §f3 arrows §7on arrow hit.", "", "§7Gain §63x base gold §7reward on targets",
            "§7you've shot an arrow in.", "", "§7Earn §6+2g§7 on assists."),
    BOUNTY_HUNTER("Bounty Hunter", 23, Material.GOLD_LEGGINGS, 0, 50, 2000,
            "§7§6+§64g §7on all kills.", "§7Earn bounty assist shares.", "", "§7§c+1% damage§7/100g bounty on target."),
    STREAKER("Streaker", 24, Material.WHEAT, 0, 50, 8000,
            "§7Triple streak kill §bXP §7bonus."),
    ASSISTANT_STREAKER("Assistant (to the) Streaker", 25, Material.SPRUCE_FENCE, 0, 50, 8000, 5, 16, 15,
            "§7Assists count their §aparticipation", "§a§7towards killstreaks."),
    COOP_CAT("Co-op Cat", 28, Material.MONSTER_EGG, 98, 50, 6000, 6, 11, 10,
            "§7§7Earn §b+50% XP §7and §6+50%g §7on all", "§7assists."),
    CONGLOMERATE("Conglomerate", 29, Material.HAY_BLOCK, 0, 50, 20000, 8, 23, 40,
            "§7Don't earn §bXP §7from kills. The §bXP §7you", "§7would earn is converted to §6gold §7at a",
            "§7§e20% §7ratio."),
    GLADIATOR("Gladiator", 30, Material.BONE, 0, 60, 4000,
            "§7Receive §9-3% §7damage per nearby", "§7player.", "", "§712 blocks range.", "§7Minimum 3, max 10 players."),
    VAMPIRE("Vampire", 31, Material.FERMENTED_SPIDER_EYE, 0, 60, 4000,
            "§7Don't earn golden apples.", "§7Heal §c0.5❤ §7on hit.", "§7Tripled on arrow crit.", "§7§cRegen I §7(8s) on kill."),
    RECON("Recon", 32, Material.EYE_OF_ENDER, 0, 60, 6000, 7, 22, 20,
            "§7Each fourth arrow shot at someone", "§7rewards §b+40 XP§7 and deals §c+50%", "§cdamage§7."),
    OVERHEAL("Overheal", 33, Material.BREAD, 0, 70, 6000, 1, 10, 10,
            "§7Double healing item limits."),
    RAMBO("Rambo", 34, Material.STICK, 0, 70, 6000, 3, 14, 15,
            "§7Don't earn golden apples.", "§7Max health: §c8❤", "§7Refill all health on kill."),
    OLYMPUS("Olympus", 37, Material.POTION, 8203, 70, 6000, 4, 15, 20,
            "§7Golden apples you earn turn into", "§7§bOlympus Potions§7.", "", "§bOlympus Potion", "§9Speed I (0:24)",
            "§9Regeneration III (0:10)", "§9Resistance II (0:04)", "§bGain +27 XP!", "§7Can only hold 1"),
    DIRTY("Dirty", 38, Material.DIRT, 2, 80, 8000, 2, 13, 15,
            "§7Gain §9Resistance II §7(4s) on kill."),
    FIRST_STRIKE("First Strike", 39, Material.COOKED_CHICKEN, 0, 80, 8000, 5, 19, 25,
            "§7First hit on a player deals §c+35%", "§cdamage §7and grants §eSpeed I §7(5s)."),
    SOUP("Soup", 40, Material.MUSHROOM_SOUP, 0, 90, 8000, 7, 21, 30,
            "§7Golden apples you earn turn into", "§7§aTasty Soup§7. You also earn soup on", "§7assists.", "",
            "§aTasty Soup", "§9Speed I (0:07)", "§a1.5❤ Heal §7+ §61❤ Absorption", "§cNext melee hit +15% damage"),
    MARATHON("Marathon", 41, Material.LEATHER_BOOTS, 0, 90, 8000, 6, 20, 20,
            "§7Cannot wear boots.", "§7While you have speed:", "§8◼ §7Deal §c+18% §7damage", "§8◼ §7Receive §9-18% §7damage"),
    THICK("Thick", 42, Material.APPLE, 0, 90, 10000, 11, 25, 45,
            "§7§7You have §c+2 Max ❤§7."),
    KUNG_FU_KNOWLEDGE("Kung Fu Knowledge", 43, Material.RAW_BEEF, 0, 100, 10000, 9, 24, 40,
            "§7No sword damage.", "§7Fists hit like a truck.", "§7Gain speed II (5s) every fourth", "§7strike on a player.");

    private final String displayName;
    private final int slot;
    private final Material icon;
    private final short data;
    private final int requiredLevel;
    private final int cost;
    /** Renown Shop で解放するパークのプレステージ (普通のパークは 0)。 */
    private final int requiredPrestige;
    /** "Renown Shop - Perks" の位置 (普通のパークは -1)。 */
    private final int renownSlot;
    private final int renownCost;
    private final boolean available;
    private final List<String> description;

    Perk(String displayName, int slot, Material icon, int data, int requiredLevel, int cost, String... description) {
        this(displayName, slot, icon, data, requiredLevel, cost, 0, -1, 0, true, description);
    }

    Perk(String displayName, int slot, Material icon, int data, int requiredLevel, int cost, boolean available,
         String... description) {
        this(displayName, slot, icon, data, requiredLevel, cost, 0, -1, 0, available, description);
    }

    Perk(String displayName, int slot, Material icon, int data, int requiredLevel, int cost,
         int requiredPrestige, int renownSlot, int renownCost, String... description) {
        this(displayName, slot, icon, data, requiredLevel, cost, requiredPrestige, renownSlot, renownCost, true, description);
    }

    Perk(String displayName, int slot, Material icon, int data, int requiredLevel, int cost,
         int requiredPrestige, int renownSlot, int renownCost, boolean available, String... description) {
        this.displayName = displayName;
        this.slot = slot;
        this.icon = icon;
        this.data = (short) data;
        this.requiredLevel = requiredLevel;
        this.cost = cost;
        this.requiredPrestige = requiredPrestige;
        this.renownSlot = renownSlot;
        this.renownCost = renownCost;
        this.available = available;
        this.description = Collections.unmodifiableList(Arrays.asList(description));
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSlot() {
        return slot;
    }

    public Material getIcon() {
        return icon;
    }

    public short getData() {
        return data;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getCost() {
        return cost;
    }

    /** Renown Shop で解放が必要なパークか。 */
    public boolean isRenownPerk() {
        return renownSlot >= 0;
    }

    public int getRequiredPrestige() {
        return requiredPrestige;
    }

    public int getRenownSlot() {
        return renownSlot;
    }

    public int getRenownCost() {
        return renownCost;
    }

    /** 効果を作ってあるか (false はアシスト・ゴールドインゴットの仕組みが無くて作れないもの)。 */
    public boolean isAvailable() {
        return available;
    }

    public List<String> getDescription() {
        return description;
    }

    public static Perk fromName(String name) {
        for (Perk perk : values()) {
            if (perk.name().equalsIgnoreCase(name)) {
                return perk;
            }
        }
        return null;
    }
}
