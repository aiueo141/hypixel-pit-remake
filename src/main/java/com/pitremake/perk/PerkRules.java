package com.pitremake.perk;

/**
 * パークの数値 (説明文と pit.wiki の Perks)。回復アイテムの持てる数は wiki に無いので推測。
 */
public final class PerkRules {

    /** Perk Slot #1〜#4 が使えるレベル (#4 は本来 Renown の Extra Perk Slot も必要)。 */
    static final int[] SLOT_LEVELS = {10, 35, 70, 100};
    public static final int SLOTS = SLOT_LEVELS.length;

    /** Strength-Chaining: キルごとにレベル +1 (1 レベル +8%、V (+40%) まで) で 7 秒に戻る。7 秒以内に次のキルが無いと 0 に戻る。 */
    static final double STRENGTH_PER_STACK = 0.08;
    static final int STRENGTH_MAX_STACKS = 5;
    static final long STRENGTH_MILLIS = 7_000;

    static final double GLADIATOR_PER_PLAYER = 0.03;
    static final int GLADIATOR_MIN_PLAYERS = 3;
    static final int GLADIATOR_MAX_PLAYERS = 10;
    static final double GLADIATOR_RANGE = 12;

    static final double BOUNTY_HUNTER_GOLD = 4;
    static final double BOUNTY_HUNTER_PER_100G = 0.01;
    /** Spammer: 矢を当てた相手のゴールドは +200% (pit.wiki の Gold、他の % と足し算)。 */
    static final double SPAMMER_GOLD_BONUS = 2.0;
    static final double SPAMMER_ASSIST_GOLD = 2;
    static final int SPAMMER_ARROWS = 3;
    static final double COOP_CAT_BONUS = 0.5;
    /** Promotion!! (Renown) を持った Assistant (to the) Streaker: +15% XP、+2g、4 キルごとにストリーク +1。 */
    static final double PROMOTION_XP = 0.15;
    static final double PROMOTION_GOLD = 2;
    public static final int PROMOTION_EVERY = 4;
    static final double STREAKER_MULTIPLIER = 3;
    static final double CONGLOMERATE_RATIO = 0.2;

    static final double VAMPIRE_HEAL = 1.0;
    static final double VAMPIRE_ARROW_CRIT_MULTIPLIER = 3;
    static final int VAMPIRE_REGEN_TICKS = 8 * 20;

    static final double FIRST_STRIKE_BONUS = 0.35;
    static final int FIRST_STRIKE_SPEED_TICKS = 5 * 20;
    static final double MARATHON_BONUS = 0.18;
    static final double SOUP_MELEE_BONUS = 0.15;
    static final int RECON_EVERY = 4;
    static final double RECON_BONUS = 0.5;
    static final int RECON_XP = 40;

    /** Kung Fu Knowledge の素手 (録画 8 本の 3,255 回の攻撃から 3.7❤、クリティカルは wiki の 3.95❤)。 */
    static final double KUNG_FU_FIST = 7.4;
    static final double KUNG_FU_CRIT = 7.9;
    static final int KUNG_FU_EVERY = 4;
    static final int KUNG_FU_SPEED_TICKS = 5 * 20;

    /** Barbarian: 鉄の斧 (6) が鉄の剣 (7) より 0.5❤ 強くなる分。 */
    static final double BARBARIAN_EXTRA = 2;

    static final int BONK_RESISTANCE_TICKS = 20;
    static final int DIRTY_RESISTANCE_TICKS = 4 * 20;

    static final double BASE_MAX_HEALTH = 20;
    static final double RAMBO_MAX_HEALTH = 16;
    static final double THICK_HEALTH = 4;

    static final int MINEMAN_COBBLE = 24;
    static final int MINEMAN_COBBLE_PER_KILL = 3;
    static final int MINEMAN_MAX_COBBLE = 64;
    static final long MINEMAN_COBBLE_TICKS = 15 * 20;
    /** 置いた溶岩が消えるまで (推測)。 */
    static final long LAVA_TICKS = 10 * 20;

    static final double LUCKY_DIAMOND_CHANCE = 0.3;

    static final int GOLDEN_APPLE_LIMIT = 2;
    static final int GOLDEN_HEAD_LIMIT = 2;
    /** pit.wiki: スープは合わせて 4 つまで。 */
    static final int SOUP_LIMIT = 4;
    static final int OLYMPUS_LIMIT = 1;

    private PerkRules() {
    }

    /** その Perk Slot (0 から) が使えるレベル。 */
    public static int slotLevel(int slot) {
        return SLOT_LEVELS[slot];
    }

    /** Gladiator: 近くのプレイヤー 3 人から 1 人 -3% (10 人まで)。 */
    static double gladiatorReduction(int nearbyPlayers) {
        if (nearbyPlayers < GLADIATOR_MIN_PLAYERS) {
            return 0;
        }
        return GLADIATOR_PER_PLAYER * Math.min(nearbyPlayers, GLADIATOR_MAX_PLAYERS);
    }

    static double strengthBonus(int stacks) {
        return STRENGTH_PER_STACK * Math.min(Math.max(stacks, 0), STRENGTH_MAX_STACKS);
    }

    /** Bounty Hunter: 相手のバウンティ 100g ごとに +1%。 */
    static double bountyHunterBonus(int bounty) {
        return BOUNTY_HUNTER_PER_100G * (Math.max(bounty, 0) / 100);
    }

    /** Conglomerate: もらうはずだった XP の 20% をゴールドに。 */
    static double conglomerateGold(int xp) {
        return Math.round(xp * CONGLOMERATE_RATIO * 100) / 100.0;
    }

    /** 回復アイテムを持てる数 (Overheal で 2 倍)。 */
    static int healingLimit(int base, boolean overheal) {
        return overheal ? base * 2 : base;
    }
}
