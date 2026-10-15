package com.pitremake.spire;

/**
 * Spire (Major Event) の数字。録画 2026-10-15_13-57-51.mcrc と pit.wiki/Spire。
 * <pre>
 * Floor  床 (立つ高さ)  出る半径  キル Souls  死亡 Souls  報酬
 *   1        85         18.5        +1          0       -50%
 *   2        96         17.5        +2          0       -25%
 *   3       105         16.5        +3         -1        ±0%
 *   4       113         14.5        +4         -2       +25%
 *   5       121         12.5        +5         -3       +50%
 *   6       129         10.5        +6         -4      +100%
 *   7       137          7.5        +8         -5      +150%
 *   8       145          4.5       +10         -6      +200%
 *   9       152          3.5       +25        -20      +250%
 * </pre>
 * 1〜5 階の高さ・半径は録画のテレポート先、6〜9 階は塔のブロック (床と壁の位置) から。
 * 名前の色は録画で見えた ONE〜FIVE と SIX (§9) 以外は推測。
 */
public final class SpireRules {

    public static final int FLOORS = 9;
    /** 塔の中心 (録画: 各階のテレポート先の円の中心)。 */
    public static final double CENTER_X = 35.0;
    public static final double CENTER_Z = 45.0;
    /** 長さと予告 (録画: 3 分前から 1 分ごと、5 分間)。 */
    public static final int WARNING_SECONDS = 180;
    public static final int DURATION_SECONDS = 300;

    private static final int[] FLOOR_Y = {85, 96, 105, 113, 121, 129, 137, 145, 152};
    private static final double[] RADIUS = {18.5, 17.5, 16.5, 14.5, 12.5, 10.5, 7.5, 4.5, 3.5};
    private static final int[] KILL_SOULS = {1, 2, 3, 4, 5, 6, 8, 10, 25};
    private static final int[] DEATH_SOULS = {0, 0, 1, 2, 3, 4, 5, 6, 20};
    private static final double[] REWARD = {-0.50, -0.25, 0, 0.25, 0.50, 1.00, 1.50, 2.00, 2.50};
    private static final String[] NAMES = {"ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE"};
    private static final String[] COLORS = {"§4", "§c", "§e", "§a", "§b", "§9", "§d", "§5", "§6"};

    /** Floor Bonus: この階に一度でも着けば +1 Renown +1,500 XP +1,500g。 */
    public static final int BONUS_FLOOR = 6;
    public static final int BONUS_RENOWN = 1;
    public static final int BONUS_XP = 1_500;
    public static final int BONUS_GOLD = 1_500;

    private SpireRules() {
    }

    private static int index(int floor) {
        return Math.max(1, Math.min(FLOORS, floor)) - 1;
    }

    public static int floorY(int floor) {
        return FLOOR_Y[index(floor)];
    }

    public static double radius(int floor) {
        return RADIUS[index(floor)];
    }

    public static int killSouls(int floor) {
        return KILL_SOULS[index(floor)];
    }

    public static int deathSouls(int floor) {
        return DEATH_SOULS[index(floor)];
    }

    /** キルの XP・ゴールドに足す割合 (-0.5 = -50%)。 */
    public static double rewardBonus(int floor) {
        return REWARD[index(floor)];
    }

    public static String color(int floor) {
        return COLORS[index(floor)];
    }

    /** 例: §4§lONE */
    public static String name(int floor) {
        return COLORS[index(floor)] + "§l" + NAMES[index(floor)];
    }

    /** 例: §c-50% / §aNORMAL / §e+25% */
    public static String rewardText(int floor) {
        double bonus = rewardBonus(floor);
        if (bonus == 0) {
            return "§aNORMAL";
        }
        return (bonus < 0 ? "§c" : "§e+") + Math.round(bonus * 100) + "%";
    }

    /** 終わったときの順位の報酬 {ゴールド, Renown} (Top 3 / Top 20 / それ以外)。 */
    public static int[] placementReward(int rank) {
        if (rank >= 1 && rank <= 3) {
            return new int[]{2_000, 2};
        }
        if (rank >= 1 && rank <= 20) {
            return new int[]{500, 1};
        }
        return new int[]{100, 0};
    }

    /** 残り時間 (例: 04:56)。 */
    public static String time(long seconds) {
        long s = Math.max(0, seconds);
        return String.format(java.util.Locale.US, "%02d:%02d", s / 60, s % 60);
    }
}
