package com.pitremake.stats;

import com.pitremake.util.RomanNumerals;

/**
 * プレステージ (pit.wiki の Prestige)。0〜L (50)。
 * <pre>
 * 条件   : レベル 120 と、そのプレステージ中に稼いだゴールド (下の表)
 * 倍率   : 次のレベルまでの XP が何倍になるか (端数は切り上げ)
 * 括弧色 : 5 プレステージごとに変わる (灰 / 青 / 黄 / 橙 / 赤 / 紫 / 桃 / 白 / 水 / 紺 / 黒 / 暗赤 / 暗灰)
 * 表示   : チャットは §9[§e§eI§9-§6§l67§9] (録画のまま §e が 2 つ)、tab は §9[§6§l67§9] (数字なし)
 * </pre>
 */
public final class PrestigeRules {

    public static final int MAX_PRESTIGE = 50;
    /** プレステージ 1〜4 の報酬 (録画 01-09-29 の初回プレステージ "Reward: 10 Renown")。 */
    public static final int RENOWN_REWARD = 10;

    private static final double[] XP_MULTIPLIERS = {
            1, 1.1, 1.2, 1.3, 1.4, 1.5, 1.75, 2, 2.5, 3,
            4, 5, 6, 7, 8, 9, 10, 12, 14, 16,
            18, 20, 24, 28, 32, 36, 40, 45, 50, 75,
            100, 101, 101, 101, 101, 101, 200, 300, 400, 500,
            750, 1_000, 1_250, 1_500, 1_750, 2_000, 3_000, 5_000, 10_000, 50_000,
            100_000};
    /** このプレステージから次へ進むのに稼ぐ必要のあるゴールド (L は無し)。 */
    private static final long[] GOLD_REQUIRED = {
            10_000, 20_000, 20_000, 20_000, 30_000, 35_000, 40_000, 45_000, 50_000, 60_000,
            70_000, 80_000, 90_000, 100_000, 125_000, 150_000, 175_000, 200_000, 250_000, 300_000,
            350_000, 400_000, 500_000, 600_000, 700_000, 800_000, 900_000, 1_000_000, 1_000_000, 1_000_000,
            1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 2_000_000, 2_000_000, 2_000_000, 2_000_000, 2_000_000,
            2_000_000, 2_000_000, 2_000_000, 2_000_000, 2_000_000, 2_000_000, 2_000_000, 2_000_000, 2_000_000, 2_000_000};

    private PrestigeRules() {
    }

    static int clamp(int prestige) {
        return Math.max(0, Math.min(prestige, MAX_PRESTIGE));
    }

    /**
     * そのプレステージになったときにもらえる Renown (pit.wiki の Renown)。
     * I〜IV 10 / V〜X 20 / XI〜XV 30 / XVI〜XX 40 / XXI〜XXV 50 / XXVI〜XXVIII 75 / XXIX 100 / XXX 250 / XXXI〜L 100
     */
    public static int renownReward(int newPrestige) {
        int p = clamp(newPrestige);
        if (p <= 4) {
            return RENOWN_REWARD;
        }
        if (p <= 10) {
            return 20;
        }
        if (p <= 15) {
            return 30;
        }
        if (p <= 20) {
            return 40;
        }
        if (p <= 25) {
            return 50;
        }
        if (p <= 28) {
            return 75;
        }
        if (p == 30) {
            return 250;
        }
        return 100;
    }

    public static double xpMultiplier(int prestige) {
        return XP_MULTIPLIERS[clamp(prestige)];
    }

    /** 次のプレステージに必要な稼いだゴールド (最大なら -1)。 */
    public static long goldRequired(int prestige) {
        int p = clamp(prestige);
        return p >= MAX_PRESTIGE ? -1 : GOLD_REQUIRED[p];
    }

    /** 括弧の色。 */
    public static String bracketColor(int prestige) {
        int p = clamp(prestige);
        if (p == 0) {
            return "§7";
        }
        if (p >= 50) {
            return "§8";
        }
        if (p >= 48) {
            return "§4";
        }
        if (p >= 45) {
            return "§0";
        }
        String[] colors = {"§9", "§e", "§6", "§c", "§5", "§d", "§f", "§b", "§1"};
        return colors[p / 5];
    }

    /** 例: "+1300%" (通常の何 % 多く XP が必要か)。 */
    public static String extraXpPercent(int prestige) {
        long percent = Math.round((xpMultiplier(prestige) - 1) * 100);
        return "+" + percent + "%";
    }

    /** チャットの名前の前。例: §9[§e§eI§9-§6§l67§9]、プレステージ 0 は §7[§71§7]。 */
    public static String chatTag(int prestige, int level) {
        if (clamp(prestige) == 0) {
            return PitLevels.format(level);
        }
        String color = bracketColor(prestige);
        return color + "[§e§e" + RomanNumerals.of(prestige) + color + "-" + PitLevels.color(level) + level + color + "]";
    }

    /** tab・頭の上・サイドバー (数字なし)。例: §9[§6§l67§9] */
    public static String levelTag(int prestige, int level) {
        String color = bracketColor(prestige);
        return color + "[" + PitLevels.color(level) + level + color + "]";
    }
}
