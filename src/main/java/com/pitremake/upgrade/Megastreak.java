package com.pitremake.upgrade;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * メガストリーク (pit.wiki の各ページ)。説明文は録画 (2026-10-14_01-08-56.mcrc の "Choose a killstreak"、
 * Overdrive は UpgradesExtraUpgradeless.mcrc の新しい説明) のまま。
 * Overdrive 以外は先に Renown Shop の Killstreaks で解放してから、レベルとゴールドで買う (pit.wiki の Renown)。
 */
public enum Megastreak {
    OVERDRIVE("Overdrive", Material.BLAZE_POWDER, 10, 50, 0, 0, "§c§lOVRDRV", Arrays.asList(
            "§7Triggers on: §c50 kills",
            " ",
            "§7On trigger:",
            "§a■ §7Perma §eSpeed I§7.",
            "§a■ §7Earn §b+50% XP §7from kills.",
            "§a■ §7Earn §6+100% gold §7from kills.",
            " ",
            "§7BUT:",
            "§7§c■ §7Receive §c+0.1❤ §7§overy §7true damage per",
            "§75 kills over 50.",
            " ",
            "§7On death:",
            "§e■ §7Gain §b4,000 XP§7.")),
    BEASTMODE("Beastmode", Material.DIAMOND_HELMET, 11, 50, 30, 10_000, "§a§lBEAST", Arrays.asList(
            "§7Triggers on: §c50 kills",
            " ",
            "§7On trigger:",
            "§a■ §7Gain a §bDiamond Helmet§7.",
            "§a■ §7Deal §c+25% §7damage.",
            "§a■ §7Earn §b+50% XP §7from kills.",
            "§a■ §7Earn §6+75% gold §7from kills.",
            " ",
            "§7BUT:",
            "§c■ §7Receive §c+0.1❤ §7damage per 5 kills above 50.",
            " ",
            "§7On death:",
            "§e■ §7Keep the §bDiamond Helmet§7.")),
    HERMIT("Hermit", Material.BED, 12, 50, 50, 20_000, "§9§lHERMIT", Arrays.asList(
            "§7Triggers on: §c50 kills",
            " ",
            "§7From 0 kills:",
            "§a■ §7Placed blocks stay §f2x §7longer.",
            "§c■ §7Permanent §9Slowness I§7.",
            " ",
            "§7On trigger:",
            "§a■ §7Permanent §9Resistance I§7.",
            "§a■ §7True damage immunity.",
            "§a■ §7Gain §f32 Bedrock §7+ §f16 §7every 10 kills.",
            "§a■ §7Earn §6+5% gold §7and §b+5%",
            "§bXP §7from kills for each 10 kills",
            "§7over 50, up to 200.",
            " ",
            "§7BUT",
            "§c■ §7Receive §c+0.3% §7damage per",
            "§7kill over 50.")),
    HIGHLANDER("Highlander", Material.GOLD_BOOTS, 13, 50, 60, 30_000, "§6§lHIGH", Arrays.asList(
            "§7Triggers on: §c50 kills",
            " ",
            "§7On trigger:",
            "§a■ §7Perma §eSpeed I§7.",
            "§a■ §7Earn §6+110% gold §7from kills.",
            "§a■ §7Deal §c+33% damage §7vs bountied players.",
            " ",
            "§7BUT:",
            "§c■ §7§6+5000g §7max bounty.",
            "§c■ §7Receive §c+0.3% §7damage",
            "§7from §6Bounty Hunter §7wearers per",
            "§7kill over 50.",
            " ",
            "§7On death:",
            "§e■ §7Earn your own bounty aswell.")),
    /** 説明文は録画 2026-10-14_20-50-01.mcrc、数値は pit.wiki の Magnum Opus。アイコンはネザースター (ユーザーから)。 */
    MAGNUM_OPUS("Magnum Opus", Material.NETHER_STAR, 14, 50, 70, 40_000, "§e§lOPUS", Arrays.asList(
            "§7Triggers on: §c50 kills",
            " ",
            "§7On trigger:",
            "§a■ §7EXPLODE!!!",
            " ",
            "§7BUT:",
            "§c■ §7Die.",
            " ",
            "§7On death:",
            "§e■ §7Earn §e1 Renown§7.",
            "§7§e■ §7Consume §54 Chunks of Vile §7to fix §c4",
            "§clives §7on the item in the §dMystic Well§7.")),
    TO_THE_MOON("To the Moon", Material.ENDER_STONE, 15, 100, 80, 50_000, "§b§lMOON", Arrays.asList(
            "§7Triggers on: §c100 kills",
            " ",
            "§7On trigger:",
            "§a■ §7Earn §b+20% XP §7from kills.",
            "§a■ §7Get §b+100 max XP §7from kills.",
            " ",
            "§7BUT:",
            "§c■ §7Receive §c+10% §7damage per 20 kills.",
            "   §7(starting from 100)",
            "§c■ §7Receive §c+0.1❤ §7true damage per 20 kills.",
            "   §7(starting from 200)",
            " ",
            "§7During the streak:",
            "§b■ §7Copy and store your kills §bXP§7.",
            " ",
            "§7On death:",
            "§b■ §7Earn the stored §bXP§7. but",
            "§7multiply it by §b0.005x §7per kill",
            "§7above 100, up to §b1x§7.")),
    UBERSTREAK("Uberstreak", Material.GOLD_SWORD, 16, 100, 90, 50_000, "§d§lUBER", Arrays.asList(
            "§7Triggers on: §c100 kills",
            " ",
            "§7On trigger:",
            "§a■ §7Gain §d+50% §7chance to find mystic items.",
            " ",
            "§7BUT:",
            "§c■ §7Receive §c+10% §7damage per 100 kills.",
            " ",
            "§7During the streak:",
            "§d■ §7100 kills: Deal §c-40% damage §7vs non-prestiged",
            "§d■ §7200 kills: §c-2 max ❤",
            "§d■ §7300 kills: §7Potion Effects last §c-50% §7as long",
            "§d■ §7400 kills: §cNo longer gain health",
            " ",
            "§7On death:",
            "§7Earn a random §dUberdrop §7if you have at least 400 streak."));

    private final String displayName;
    private final Material icon;
    /** "Choose a killstreak" 画面での位置 (録画と同じ。14 は Magnum Opus)。 */
    private final int slot;
    private final int trigger;
    private final int requiredLevel;
    private final int cost;
    private final String tag;
    private final List<String> lore;

    Megastreak(String displayName, Material icon, int slot, int trigger, int requiredLevel, int cost, String tag, List<String> lore) {
        this.displayName = displayName;
        this.icon = icon;
        this.slot = slot;
        this.trigger = trigger;
        this.requiredLevel = requiredLevel;
        this.cost = cost;
        this.tag = tag;
        this.lore = Collections.unmodifiableList(lore);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getSlot() {
        return slot;
    }

    /** 発動するキル数。 */
    public int getTrigger() {
        return trigger;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    /** 解放に必要なゴールド (Overdrive は最初から使える)。 */
    public int getCost() {
        return cost;
    }

    public boolean isDefault() {
        return this == OVERDRIVE;
    }

    /** Renown Shop の Killstreaks での解放に要る Renown (Overdrive は 0)。 */
    public int getRenownCost() {
        switch (this) {
            case BEASTMODE:
                return 10;
            case HERMIT:
                return 20;
            case HIGHLANDER:
                return 40;
            case MAGNUM_OPUS:
                return 60;
            case TO_THE_MOON:
                return 150;
            case UBERSTREAK:
                return 50;
            default:
                return 0;
        }
    }

    /** Renown Shop の Killstreaks での解放に要るプレステージ。 */
    public int getRenownPrestige() {
        switch (this) {
            case BEASTMODE:
                return 3;
            case HERMIT:
                return 4;
            case HIGHLANDER:
                return 7;
            case MAGNUM_OPUS:
                return 10;
            case TO_THE_MOON:
                return 14;
            case UBERSTREAK:
                return 20;
            default:
                return 0;
        }
    }

    /** UpgradeManager の解放の key。 */
    public String unlockKey() {
        return "streak:" + name();
    }

    public List<String> getLore() {
        return lore;
    }

    /**
     * 発動中に tab と頭の上でレベルの代わりに出す文字。
     * 録画で確認できたのは Uberstreak の "§d§lUBER100" だけ (100 キルごとに数字が上がる)。他は推測。
     */
    public String tag(int streak) {
        return this == UBERSTREAK ? tag + Math.max(100, streak / 100 * 100) : tag;
    }

    public static Megastreak fromName(String name) {
        for (Megastreak megastreak : values()) {
            if (megastreak.name().equalsIgnoreCase(name)) {
                return megastreak;
            }
        }
        return null;
    }
}
