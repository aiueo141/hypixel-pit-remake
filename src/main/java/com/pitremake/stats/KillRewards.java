package com.pitremake.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * キルしたときにもらえる XP とゴールド (pit.wiki の XP / Gold のページより)。
 * <pre>
 * XP   = (5 + 最初の 3 キル +4 + レベル差 (相手が 5 レベル上ごとに +1) + ストリーク + シャットダウン + XP Bump)
 *        を上限 400 (Sweaty で +50 / +100) で切り、倍率を掛ける
 * Gold = (10 + 最初の 3 キル +4 + 防具差 (最大 +16) + シャットダウン (最大 +80) + Gold Bump + Moctezuma + Sierra)
 *        に倍率を掛け、上限 2,500g
 * 倍率 = 出どころごとの (1 + %) の掛け算。同じ出どころの % はその中で足し算
 *        (例: 剣とパンツの Gold Boost II + II = ×1.6、Highlander ×2.1、Passive V ×1.5、Renown X ×1.25、Celebrity ×2)。
 *        相手が Lv20 以下 (noob) なら ×0.9。
 *        ユーザーの実例 (この装備で Celebrity 込み 900g 近く) は掛け算でないと届かない
 * </pre>
 * メガストリーク・Passive・Renown・パークの分は {@link Boosts}。イベントは未実装。
 */
public final class KillRewards {

    static final int BASE_XP = 5;
    static final double BASE_GOLD = 10;
    /** 1 回の人生で最初の 3 キルにつくボーナス。 */
    static final int FIRST_KILLS = 3;
    static final int FIRST_KILLS_XP = 4;
    static final double FIRST_KILLS_GOLD = 4;
    /** 相手が自分よりこのレベル数上ごとに XP +1。 */
    static final int LEVEL_GAP_PER_XP = 5;
    static final int MAX_XP = 400;
    static final double MAX_GOLD = 2500;
    /** このレベル以下の相手は noob 扱い (報酬 -10%)。 */
    static final int NOOB_LEVEL = 20;
    /** noob は XP もゴールドも ×0.9。 */
    static final double NOOB_MULTIPLIER = 0.9;
    static final int MAX_STREAK_XP = 30;
    static final int MAX_ARMOR_BONUS = 16;
    static final int MAX_SHUTDOWN_XP = 300;
    static final double SHUTDOWN_GOLD_PER_KILL = 2;
    static final double MAX_SHUTDOWN_GOLD = 80;

    // リソース系エンチャント (添字 = レベル)
    private static final double[] XP_BUMP = {0, 2, 4, 6};
    private static final double[] XP_BOOST = {0, 0.10, 0.20, 0.30};
    private static final double[] SWEATY_STREAK = {0, 0.20, 0.40, 0.60};
    private static final double[] SWEATY_MAX_XP = {0, 0, 50, 100};
    private static final double[] GOLD_BUMP = {0, 4, 8, 12};
    private static final double[] GOLD_BOOST = {0, 0.15, 0.30, 0.45};
    private static final double[] MOCTEZUMA = {0, 7, 12, 18};
    private static final double[] SIERRA_PER_DIAMOND = {0, 30, 60, 90};

    private KillRewards() {
    }

    /** キルの状況。items はキルした人の持っている武器と履いているパンツのエンチャント (アイテムごとの効果を合計する)。 */
    public static final class Kill {
        final int killerLevel;
        final int victimLevel;
        /** このキルを含めたキルした人のストリーク。 */
        final int killerStreak;
        /** 死ぬ前の相手のストリーク。 */
        final int victimStreak;
        final int killerArmor;
        final int victimArmor;
        final int victimDiamondPieces;
        final List<Map<String, Integer>> items;
        /** メガストリーク・Passive の分。 */
        Boosts boosts = new Boosts();

        public Kill(int killerLevel, int victimLevel, int killerStreak, int victimStreak,
                    int killerArmor, int victimArmor, int victimDiamondPieces, List<Map<String, Integer>> items) {
            this.killerLevel = killerLevel;
            this.victimLevel = victimLevel;
            this.killerStreak = killerStreak;
            this.victimStreak = victimStreak;
            this.killerArmor = killerArmor;
            this.victimArmor = victimArmor;
            this.victimDiamondPieces = victimDiamondPieces;
            this.items = items == null ? Collections.<Map<String, Integer>>emptyList() : items;
        }

        double sum(String enchant, double[] perLevel) {
            double total = 0;
            for (Map<String, Integer> item : items) {
                Integer level = item.get(enchant);
                if (level != null) {
                    total += perLevel[Math.min(Math.max(level, 0), perLevel.length - 1)];
                }
            }
            return total;
        }

        public Kill with(Boosts boosts) {
            this.boosts = boosts;
            return this;
        }

        boolean isFirstKills() {
            return killerStreak <= FIRST_KILLS;
        }

        boolean isNoob() {
            return victimLevel <= NOOB_LEVEL;
        }
    }

    /**
     * メガストリーク・Passive・Renown・パークによる追加。% は出どころごとに 1 つずつ足し ({@link #addXp} / {@link #addGold})、
     * それぞれの (1 + %) を掛け合わせる。flat は El Gato のような基本値への加算。
     */
    public static final class Boosts {
        private final List<Double> xpBonuses = new ArrayList<>();
        private final List<Double> goldBonuses = new ArrayList<>();

        /** XP の倍率を 1 つ足す (0.5 = ×1.5)。0 は無視する。 */
        public void addXp(double bonus) {
            if (bonus != 0) {
                xpBonuses.add(bonus);
            }
        }

        /** ゴールドの倍率を 1 つ足す (1.1 = ×2.1)。0 は無視する。 */
        public void addGold(double bonus) {
            if (bonus != 0) {
                goldBonuses.add(bonus);
            }
        }

        double xpFactor() {
            return factor(xpBonuses);
        }

        double goldFactor() {
            return factor(goldBonuses);
        }

        private static double factor(List<Double> bonuses) {
            double factor = 1;
            for (double bonus : bonuses) {
                factor *= Math.max(0, 1 + bonus);
            }
            return factor;
        }
        public int maxXp;
        public double flatXp;
        public double flatGold;
        /** ストリークの XP ボーナスに掛ける (Streaker で 3)。 */
        public double streakXpMultiplier = 1;
        /** ゴールドの最後に掛ける倍率 (Renown の Celebrity で 2 倍。ほかの % とは足さない)。 */
        public double goldFinalMultiplier = 1;
        /** ストリークの XP ボーナスの上限 (Experience-Industrial Complex で 60)。 */
        public int streakXpCap = MAX_STREAK_XP;
    }

    /** 先に足し算の分 (基本値・ボーナス) を合計して上限で切り、そのあと出どころごとの倍率を掛ける。 */
    public static int xp(Kill kill) {
        double xp = BASE_XP
                + (kill.isFirstKills() ? FIRST_KILLS_XP : 0)
                + Math.max(0, kill.victimLevel - kill.killerLevel) / LEVEL_GAP_PER_XP
                + streakXp(kill.killerStreak, kill.boosts.streakXpCap) * (1 + kill.sum("streak_xp", SWEATY_STREAK))
                * kill.boosts.streakXpMultiplier
                + shutdownXp(kill.victimStreak)
                + kill.sum("xp_per_kill", XP_BUMP)
                + kill.boosts.flatXp;
        xp = Math.min(xp, MAX_XP + kill.sum("streak_xp", SWEATY_MAX_XP) + kill.boosts.maxXp);
        xp *= (1 + kill.sum("xp_boost", XP_BOOST)) * kill.boosts.xpFactor() * (kill.isNoob() ? NOOB_MULTIPLIER : 1);
        return (int) Math.round(xp);
    }

    /** XP と同じく、足し算の分を合計してから出どころごとの倍率を掛ける。上限 2,500g。 */
    public static double gold(Kill kill) {
        double gold = BASE_GOLD
                + (kill.isFirstKills() ? FIRST_KILLS_GOLD : 0)
                + armorBonus(kill.killerArmor, kill.victimArmor)
                + shutdownGold(kill.victimStreak)
                + kill.sum("gold_per_kill", GOLD_BUMP)
                + kill.sum("gold_strictly_kills", MOCTEZUMA)
                + kill.sum("gold_per_diamond_piece", SIERRA_PER_DIAMOND) * kill.victimDiamondPieces
                + kill.boosts.flatGold;
        gold *= (1 + kill.sum("gold_boost", GOLD_BOOST)) * kill.boosts.goldFactor() * (kill.isNoob() ? NOOB_MULTIPLIER : 1);
        gold *= kill.boosts.goldFinalMultiplier;
        return Math.round(Math.min(gold, MAX_GOLD) * 100) / 100.0;
    }

    static int streakXp(int streak) {
        return streakXp(streak, MAX_STREAK_XP);
    }

    /** ストリーク 3〜4: +3、5〜19: +5、20 以上: 10 キルごとに +3 (最大 +30、Experience-Industrial Complex で +60)。 */
    static int streakXp(int streak, int cap) {
        if (streak < 3) {
            return 0;
        }
        if (streak < 5) {
            return 3;
        }
        if (streak < 20) {
            return 5;
        }
        return Math.min(cap, 3 * (streak / 10));
    }

    /** 相手のストリーク 3〜4: +15、5〜19: +25、20 以上: 10 キルごとに +15 (最大 +300)。 */
    static int shutdownXp(int victimStreak) {
        if (victimStreak < 3) {
            return 0;
        }
        if (victimStreak < 5) {
            return 15;
        }
        if (victimStreak < 20) {
            return 25;
        }
        return Math.min(MAX_SHUTDOWN_XP, 15 * (victimStreak / 10));
    }

    /** 相手のストリーク 3 以上: 1 キルにつき +2g (最大 +80g)。 */
    static double shutdownGold(int victimStreak) {
        return victimStreak < 3 ? 0 : Math.min(MAX_SHUTDOWN_GOLD, SHUTDOWN_GOLD_PER_KILL * victimStreak);
    }

    /** 防具の値 (ダイヤ 1 つ 4、鉄 1 つ 2) が相手の方が高ければ、その差 (最大 +16g)。 */
    static int armorBonus(int killerArmor, int victimArmor) {
        return Math.min(MAX_ARMOR_BONUS, Math.max(0, victimArmor - killerArmor));
    }
}
