package com.pitremake.upgrade;

/**
 * メガストリークの効果の数値 (pit.wiki の各ページ)。streak はこのキルを含めた今のキルストリーク。
 * 発動前 (streak がトリガー未満) は Hermit の鈍足以外なにも起きない。
 * <pre>
 * Overdrive   : XP +50% / ゴールド +100% (wiki と新しいゲーム内の説明)、Speed I、
 *               50 キルを超えて 5 キルごとに被ダメージ +0.1❤ の確定ダメージ、死亡時 4,000 XP
 * Beastmode   : 与ダメージ +25%、XP +50%、ゴールド +75%、ダイヤのヘルメット、
 *               50 キルを超えて 5 キルごとに被ダメージ +0.1❤、死亡してもヘルメットは残る
 * Hermit      : 選んでいるだけで Slowness I。発動で Resistance I・確定ダメージ無効、
 *               XP とゴールド +5% (50 キルを超えて 10 キルごとに +5%、最大 +75%)、被ダメージ 1 キルごとに +0.3%
 * Highlander  : Speed I、ゴールド +110%、バウンティ持ちへの与ダメージ +33%、バウンティの上限 10,000g、
 *               死亡時に自分のバウンティ分のゴールドももらう
 * To the Moon : XP +20%、1 キルの XP 上限 +100、100 キルを超えて 20 キルごとに被ダメージ +10%、
 *               200 キルを超えて 20 キルごとに +0.1❤ の確定ダメージ、
 *               ストリーク中の XP を貯めて、死亡時に 0.005 倍 × (キル数 - 100) (最大 1 倍) でもらう
 * Uberstreak  : 100 キルごとに被ダメージ +10%、100 キル: 与ダメージ -40% (プレステージの無い相手 = 全員)、
 *               200 キル: 最大体力 -2❤、400 キル: 回復しない、400 キル以上で死ぬと Uberdrop
 * </pre>
 */
public final class MegastreakRules {

    /** 0.1❤ (体力 0.2)。 */
    static final double TENTH_HEART = 0.2;
    static final int OVERDRIVE_DEATH_XP = 4000;
    static final int MOON_MAX_XP_BONUS = 100;
    static final int UBER_LOST_MAX_HEALTH = 4;
    static final int UBER_DROP_STREAK = 400;

    private MegastreakRules() {
    }

    public static boolean isActive(Megastreak megastreak, int streak) {
        return megastreak != null && streak >= megastreak.getTrigger();
    }

    /** キルの XP の追加倍率 (+0.5 = +50%)。 */
    public static double xpBonus(Megastreak megastreak, int streak) {
        if (!isActive(megastreak, streak)) {
            return 0;
        }
        switch (megastreak) {
            case OVERDRIVE:
            case BEASTMODE:
                return 0.5;
            case HERMIT:
                return hermitBonus(streak);
            case TO_THE_MOON:
                return 0.2;
            default:
                return 0;
        }
    }

    /** キルのゴールドの追加倍率。 */
    public static double goldBonus(Megastreak megastreak, int streak) {
        if (!isActive(megastreak, streak)) {
            return 0;
        }
        switch (megastreak) {
            case OVERDRIVE:
                return 1.0;
            case BEASTMODE:
                return 0.75;
            case HERMIT:
                return hermitBonus(streak);
            case HIGHLANDER:
                return 1.1;
            default:
                return 0;
        }
    }

    /** Hermit の XP・ゴールド: 発動時 +5%、50 キルを超えて 10 キルごとに +5% (最大 +75%)。 */
    static double hermitBonus(int streak) {
        return Math.min(0.75, 0.05 * (1 + (streak - 50) / 10));
    }

    /** 1 キルの XP 上限の追加。 */
    public static int maxXpBonus(Megastreak megastreak, int streak) {
        return megastreak == Megastreak.TO_THE_MOON && isActive(megastreak, streak) ? MOON_MAX_XP_BONUS : 0;
    }

    /** 与えるダメージの倍率。victimIsPlayer は Uberstreak の判定では "相手がプレステージ無し" の意味。 */
    public static double damageDealtMultiplier(Megastreak megastreak, int streak, boolean victimIsPlayer, boolean victimHasBounty) {
        if (!isActive(megastreak, streak)) {
            return 1;
        }
        switch (megastreak) {
            case BEASTMODE:
                return 1.25;
            case HIGHLANDER:
                return victimIsPlayer && victimHasBounty ? 1.33 : 1;
            case UBERSTREAK:
                return victimIsPlayer ? 0.6 : 1;
            default:
                return 1;
        }
    }

    /** 受けるダメージの倍率。 */
    public static double damageTakenMultiplier(Megastreak megastreak, int streak) {
        if (!isActive(megastreak, streak)) {
            return 1;
        }
        switch (megastreak) {
            case HERMIT:
                return 1 + 0.003 * (streak - 50);
            case TO_THE_MOON:
                return 1 + 0.10 * ((streak - 100) / 20);
            case UBERSTREAK:
                return 1 + 0.10 * (streak / 100);
            default:
                return 1;
        }
    }

    /** Highlander: Bounty Hunter を付けた相手からのダメージ +0.3% × (50 を超えたキル数) (pit.wiki の Highlander)。 */
    public static double bountyHunterDamageTaken(Megastreak megastreak, int streak) {
        return megastreak == Megastreak.HIGHLANDER && isActive(megastreak, streak) ? 0.003 * (streak - 50) : 0;
    }

    /** 受けるダメージに足す量 (防具で減る普通のダメージ)。 */
    public static double extraDamageTaken(Megastreak megastreak, int streak) {
        return megastreak == Megastreak.BEASTMODE && isActive(megastreak, streak) ? TENTH_HEART * ((streak - 50) / 5) : 0;
    }

    /** 攻撃を受けるたびに追加で受ける確定ダメージ (防具を無視)。 */
    public static double trueDamageTaken(Megastreak megastreak, int streak) {
        if (!isActive(megastreak, streak)) {
            return 0;
        }
        if (megastreak == Megastreak.OVERDRIVE) {
            return TENTH_HEART * ((streak - 50) / 5);
        }
        if (megastreak == Megastreak.TO_THE_MOON && streak >= 200) {
            return TENTH_HEART * ((streak - 200) / 20);
        }
        return 0;
    }

    public static boolean isTrueDamageImmune(Megastreak megastreak, int streak) {
        return megastreak == Megastreak.HERMIT && isActive(megastreak, streak);
    }

    public static int maxBounty(Megastreak megastreak, int streak) {
        return megastreak == Megastreak.HIGHLANDER && isActive(megastreak, streak) ? 10_000 : 5_000;
    }

    /** Uberstreak の最大体力の減少 (200 キルから -2❤)。 */
    public static int lostMaxHealth(Megastreak megastreak, int streak) {
        return megastreak == Megastreak.UBERSTREAK && streak >= 200 ? UBER_LOST_MAX_HEALTH : 0;
    }

    /** Uberstreak 300 キルから、ポーション効果の時間が半分。 */
    public static boolean halvesPotions(Megastreak megastreak, int streak) {
        return megastreak == Megastreak.UBERSTREAK && streak >= 300;
    }

    public static boolean canHeal(Megastreak megastreak, int streak) {
        return !(megastreak == Megastreak.UBERSTREAK && streak >= 400);
    }

    /** To the Moon の死亡時: 貯めた XP に掛ける倍率 (0.005 × (キル数 - 100)、最大 1)。 */
    public static double moonPayoutMultiplier(int streak) {
        return streak <= 100 ? 0 : Math.min(1, 0.005 * (streak - 100));
    }
}
