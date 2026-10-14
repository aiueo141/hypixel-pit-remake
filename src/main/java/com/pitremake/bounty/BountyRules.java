package com.pitremake.bounty;

import java.util.Random;

/**
 * バウンティのルール。
 * <pre>
 * pit.wiki (Bounty) より:
 *   ストリーク 6 以上のキルで、ランダムにバウンティが付く / 上がる (bump)
 *   ストリークが高いほど額が大きく、続けてキルするほど上がりやすい
 *   50g 単位、1 回の上昇は最大 300g、上限 5,000g
 *   バウンティ 1,000g ごとに戦闘中の時間 +5 秒
 * wiki に数値が無いので推測 (実験的):
 *   確率   5% + ストリーク 1 につき 0.5% (最大 25%)、前のキルから 10 秒以内なら 1.5 倍 (最大 35%)
 *          (前は 15% + 1% で上がりやすすぎたので下げた。ストリーク 10 で 7% / 素早いキルなら 10.5%)
 *   上昇額 ストリーク 6〜9: 50〜100g / 10〜19: 100〜150g / 20〜49: 150〜200g / 50〜99: 200〜250g / 100〜: 250〜300g
 * </pre>
 * 録画 (2026-10-14_00-39-15.mcrc) では 180 キルのストリークで 300g ずつ上がっていた。
 */
public final class BountyRules {

    public static final int MIN_STREAK = 6;
    public static final int STEP = 50;
    public static final int MAX_BUMP = 300;
    public static final int MAX_BOUNTY = 5000;
    static final double BASE_CHANCE = 0.05;
    static final double CHANCE_PER_STREAK = 0.005;
    static final double MAX_CHANCE = 0.25;
    static final long QUICK_KILL_MILLIS = 10_000;
    static final double QUICK_KILL_MULTIPLIER = 1.5;
    static final double MAX_QUICK_CHANCE = 0.35;
    /** {ストリーク, 最小額, 最大額} (ストリークの小さい順)。 */
    private static final int[][] BUMP_RANGES = {
            {6, 50, 100}, {10, 100, 150}, {20, 150, 200}, {50, 200, 250}, {100, 250, 300}};
    /** バウンティ 1,000g ごとに延びる戦闘中の時間。 */
    static final long COMBAT_MILLIS_PER_1000 = 5_000;

    private BountyRules() {
    }

    /** このキルでバウンティが付く / 上がる確率。 */
    public static double chance(int streak, boolean quickKill) {
        if (streak < MIN_STREAK) {
            return 0;
        }
        double chance = Math.min(MAX_CHANCE, BASE_CHANCE + CHANCE_PER_STREAK * (streak - MIN_STREAK));
        return quickKill ? Math.min(MAX_QUICK_CHANCE, chance * QUICK_KILL_MULTIPLIER) : chance;
    }

    public static boolean isQuickKill(long sinceLastKillMillis) {
        return sinceLastKillMillis >= 0 && sinceLastKillMillis <= QUICK_KILL_MILLIS;
    }

    /** 上昇額 (50g 単位、最大 300g、上限 5,000g を超えない)。上がらなければ 0。 */
    public static int rollBump(int streak, int current, Random random) {
        return rollBump(streak, current, MAX_BOUNTY, random);
    }

    /** maxBounty: バウンティの上限 (Highlander 発動中は 10,000g)。 */
    public static int rollBump(int streak, int current, int maxBounty, Random random) {
        if (streak < MIN_STREAK) {
            return 0;
        }
        int[] range = BUMP_RANGES[0];
        for (int[] candidate : BUMP_RANGES) {
            if (streak >= candidate[0]) {
                range = candidate;
            }
        }
        int steps = (range[2] - range[1]) / STEP;
        int bump = Math.min(MAX_BUMP, range[1] + STEP * random.nextInt(steps + 1));
        return Math.max(0, Math.min(bump, maxBounty - current));
    }

    public static long combatExtensionMillis(int bounty) {
        return bounty / 1000 * COMBAT_MILLIS_PER_1000;
    }
}
