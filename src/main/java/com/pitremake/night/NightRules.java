package com.pitremake.night;

/**
 * Pit の昼と夜の時刻 (録画 2026-10-15_10-34-42.mcrc)。
 * <pre>
 * 昼   : 時刻は固定 (doDaylightCycle false)。録画では 4900〜4987
 * 日暮れ: 1 tick に 117 ずつ 94 tick (4.7 秒) 進めて 4987 → 15985 (+10998)
 * 夜   : 固定。日暮れの開始から 20 tick 後に NIGHT QUEST! と portal.travel
 * 夜明け: 録画に無い。日暮れと同じ速さで先へ進めて次の日の昼の時刻に戻す (推測)
 * </pre>
 */
public final class NightRules {

    /** 昼の時刻。 */
    public static final long DAY_TIME = 4900;
    /** 1 tick に進める時刻。 */
    public static final long STEP = 117;
    /** 日暮れの tick 数。 */
    public static final int NIGHTFALL_TICKS = 94;
    /** 夜の時刻。 */
    public static final long NIGHT_TIME = DAY_TIME + STEP * NIGHTFALL_TICKS;
    /** 夜明けで行き着く時刻 (次の日の昼)。 */
    public static final long NEXT_DAY_TIME = DAY_TIME + 24000;
    /** 夜明けの tick 数。 */
    public static final int MORNING_TICKS = (int) ((NEXT_DAY_TIME - NIGHT_TIME + STEP - 1) / STEP);
    /** 日暮れの開始から NIGHT QUEST! までの tick 数 (録画 64.49 秒 → 65.52 秒)。 */
    public static final int QUEST_DELAY_TICKS = 20;

    /** Night Quest: 夜明けまでにキルする人数と報酬 (録画のチャットのホバー: +650 XP & +1 Chunk of Vile)。 */
    public static final int QUEST_KILLS = 30;
    public static final int QUEST_XP = 650;
    public static final int QUEST_VILE = 1;

    private NightRules() {
    }

    /** 日暮れの tick 目の時刻。 */
    public static long nightfall(int tick) {
        return Math.min(DAY_TIME + STEP * tick, NIGHT_TIME);
    }

    /** 夜明けの tick 目の時刻 (0〜23999)。 */
    public static long morning(int tick) {
        return Math.min(NIGHT_TIME + STEP * tick, NEXT_DAY_TIME) % 24000;
    }
}
