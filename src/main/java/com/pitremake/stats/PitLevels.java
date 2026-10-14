package com.pitremake.stats;

/**
 * Pit のレベル (1〜120) と XP の計算。プレステージは無し (実験的)。
 * <pre>
 * 1〜9    : 15 XP / レベル        60〜69  : 600
 * 10〜19  : 30                    70〜79  : 800
 * 20〜29  : 50                    80〜89  : 900
 * 30〜39  : 75                    90〜99  : 1000
 * 40〜49  : 125                   100〜109: 1200
 * 50〜59  : 300                   110〜119: 1500
 * </pre>
 * 録画 (2026-10-13_23-49-38.mcrc): Lv1 で残り 5 XP の状態から +18XP → Lv2 の残り 2 XP (バー 0.67 → 0.87)。
 */
public final class PitLevels {

    public static final int MAX_LEVEL = 120;
    /** レベル帯 (10 レベルごと) で次のレベルまでに必要な XP。 */
    private static final int[] XP_PER_LEVEL = {15, 30, 50, 75, 125, 300, 600, 800, 900, 1000, 1200, 1500};
    /** レベル帯ごとの色。60 以上は太字。 */
    private static final String[] LEVEL_COLORS = {
            "§7", "§9", "§3", "§2", "§a", "§e", "§6§l", "§c§l", "§4§l", "§5§l", "§d§l", "§f§l", "§b§l"};

    private PitLevels() {
    }

    /** level から level + 1 に上がるのに必要な XP (最大レベルでは 0)。 */
    public static int xpToNext(int level) {
        return (int) xpToNext(level, 0);
    }

    /** プレステージの倍率を掛けて切り上げた XP (pit.wiki の Level)。 */
    public static long xpToNext(int level, int prestige) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return (long) Math.ceil(XP_PER_LEVEL[Math.max(1, level) / 10] * PrestigeRules.xpMultiplier(prestige) - 1e-9);
    }

    /** 累計 XP から求めたレベル。 */
    public static int levelOf(long totalXp) {
        return levelOf(totalXp, 0);
    }

    public static int levelOf(long totalXp, int prestige) {
        int level = 1;
        long remaining = totalXp;
        while (level < MAX_LEVEL && remaining >= xpToNext(level, prestige)) {
            remaining -= xpToNext(level, prestige);
            level++;
        }
        return level;
    }

    /** レベル 1 からそのレベルになるまでの累計 XP。 */
    public static long totalXpFor(int level) {
        return totalXpFor(level, 0);
    }

    public static long totalXpFor(int level, int prestige) {
        long total = 0;
        for (int i = 1; i < Math.min(level, MAX_LEVEL); i++) {
            total += xpToNext(i, prestige);
        }
        return total;
    }

    /** 次のレベルまでに足りない XP (最大レベルでは 0)。 */
    public static long neededXp(long totalXp) {
        return neededXp(totalXp, 0);
    }

    public static long neededXp(long totalXp, int prestige) {
        int level = levelOf(totalXp, prestige);
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return totalXpFor(level + 1, prestige) - totalXp;
    }

    /** 経験値バー用の進み具合 (0〜1、最大レベルでは 1)。 */
    public static float progress(long totalXp) {
        return progress(totalXp, 0);
    }

    public static float progress(long totalXp, int prestige) {
        int level = levelOf(totalXp, prestige);
        if (level >= MAX_LEVEL) {
            return 1f;
        }
        return (float) (totalXp - totalXpFor(level, prestige)) / xpToNext(level, prestige);
    }

    public static String color(int level) {
        return LEVEL_COLORS[Math.min(Math.max(1, level), MAX_LEVEL) / 10];
    }

    /** 例: §7[§71§7]、§7[§b§l120§7] */
    public static String format(int level) {
        return "§7[" + color(level) + level + "§7]";
    }
}
