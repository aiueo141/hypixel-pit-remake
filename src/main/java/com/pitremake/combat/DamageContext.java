package com.pitremake.combat;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * 1 回のダメージの増減を集める。{@link DamageCalculator} が最後にまとめて掛ける。
 * 重ね方は 2 通りから選べる (config の damage.stacking、/pit damage)。
 * <pre>
 * additive (加算)       : 倍率 = max(0, 1 + 増加の合計 - 減少の合計)
 *                         例: Solitude III -60% と Critically Funky III -40% で -100% → Billionaire III +100% で相殺
 * multiplicative (乗算) : 倍率 = Π(1 + 増加) × Π(1 - 減少)
 *                         逆算レポート「Hypixel Pit ダメージ計算ロジック」: 録画 2,383 件の近接攻撃で
 *                         Melee Damage V と プレステージなしの補正が 1.10 × 1.05 = 1.155 (足し算なら 1.15)
 * </pre>
 * 同じ効果の中で足すもの (Strength-Chaining のスタックなど) は呼ぶ側で 1 回にまとめる。
 * そのあとバニラのガード → 防具 → 耐性が掛かる。確定ダメージ (true damage) は防具も倍率も無視して別に与える。
 */
public final class DamageContext {

    /** 増減の重ね方。 */
    public enum Stacking {
        ADDITIVE, MULTIPLICATIVE;

        public static Stacking fromName(String name) {
            for (Stacking stacking : values()) {
                if (stacking.name().equalsIgnoreCase(name)) {
                    return stacking;
                }
            }
            return null;
        }
    }

    private static volatile Stacking stacking = Stacking.ADDITIVE;

    public static Stacking getStacking() {
        return stacking;
    }

    public static void setStacking(Stacking value) {
        stacking = value;
    }

    public final EntityDamageEvent event;
    public final Player victim;
    /** 攻撃したプレイヤー (矢なら撃った人)。プレイヤー以外 (落下など) は null。 */
    public final Player attacker;
    public final boolean melee;
    /** 矢の攻撃なら矢 (それ以外は null)。 */
    public final Arrow arrow;
    public final boolean critical;
    /** 増減を掛ける前の攻撃力。 */
    public final double baseDamage;

    /** 増減を全部掛けた倍率 (乗算のとき)。 */
    double factor = 1;
    /** 増加・減少の合計 (加算のとき)。 */
    double increaseSum;
    double reductionSum;
    /** 倍率を掛ける前に足す攻撃力 (防具で減る)。 */
    double flatDamage;
    double trueDamage;
    /** 防げない確定ダメージ (メガストリークの代償など。Mirror / Hermit でも受ける)。 */
    double penaltyTrueDamage;
    /** Mirror / Hermit で防いだ確定ダメージ (Mirror II / III の跳ね返しに使う)。 */
    double blockedTrueDamage;
    /** 実際に相手に入った確定ダメージ (Mirror / Hermit で無効なら 0)。{@link #afterHit} の中で読める。 */
    double dealtTrueDamage;
    double maxFinalDamage = Double.MAX_VALUE;
    /** 最後のダメージから引く量 (Bruiser)。 */
    double flatReduction;
    boolean trueDamageImmune;
    boolean kill;
    /**
     * Regularity の追撃の倍率と足す攻撃力 (最初の一撃の倍率 × 1.75 と、最初の一撃の足した攻撃力)。
     * null でなければ、この攻撃で集めた増減の代わりにこれを使う (Gamble などの効果は追撃でも起きる)。
     */
    Double forcedMultiplier;
    double forcedFlat;
    /** これが Regularity の追撃なら true (追撃はコンボに数えない・もう一度追撃しない)。 */
    final boolean extraStrike;
    final List<DoubleConsumer> afterHit = new ArrayList<>();

    DamageContext(EntityDamageEvent event, Player victim, Player attacker, boolean melee, Arrow arrow, boolean critical,
                  boolean extraStrike) {
        this.event = event;
        this.victim = victim;
        this.attacker = attacker;
        this.melee = melee;
        this.arrow = arrow;
        this.critical = critical;
        this.baseDamage = event.getDamage();
        this.extraStrike = extraStrike;
    }

    /** プレイヤーからの攻撃 (近接か矢) か。 */
    public boolean byPlayer() {
        return attacker != null && !attacker.equals(victim);
    }

    public boolean isExtraStrike() {
        return extraStrike;
    }

    /** 与ダメージ増加 (0.25 = +25%)。 */
    public void increase(double fraction) {
        factor *= Math.max(0, 1 + fraction);
        increaseSum += fraction;
    }

    /** 被ダメージ減少 (0.25 = -25%)。 */
    public void reduce(double fraction) {
        factor *= Math.max(0, 1 - fraction);
        reductionSum += fraction;
    }

    /** 倍率を掛ける前の攻撃力に足す (体力で。防具で減る)。 */
    public void addDamage(double health) {
        flatDamage += health;
    }

    /** 確定ダメージ (体力で、0.5❤ = 1.0)。 */
    public void addTrueDamage(double health) {
        trueDamage += health;
    }

    /** Mirror / Hermit でも防げない確定ダメージ (体力で)。 */
    public void addPenaltyTrueDamage(double health) {
        penaltyTrueDamage += health;
    }

    /** この攻撃で Mirror / Hermit が防いだ確定ダメージ (afterHit の中で使う)。 */
    public double getBlockedTrueDamage() {
        return blockedTrueDamage;
    }

    /** 最後のダメージ (防具の後) から引く (体力で。Bruiser)。 */
    public void reduceFlat(double health) {
        flatReduction += health;
    }

    /** 1 回で受けるダメージの上限 (Singularity)。 */
    public void capFinalDamage(double health) {
        maxFinalDamage = Math.min(maxFinalDamage, health);
    }

    public void immuneToTrueDamage() {
        trueDamageImmune = true;
    }

    /** この攻撃で倒す (Executioner)。 */
    public void kill() {
        kill = true;
    }

    /** この攻撃で相手に入った確定ダメージ (afterHit の中で使う)。 */
    public double getDealtTrueDamage() {
        return dealtTrueDamage;
    }

    /** 攻撃が当たった後 (最終ダメージが分かってから) にすること。 */
    public void afterHit(DoubleConsumer action) {
        afterHit.add(action);
    }

    /** 今の重ね方での倍率 (Regularity の追撃は最初の一撃の倍率 × 1.75)。 */
    public double multiplier() {
        if (forcedMultiplier != null) {
            return forcedMultiplier;
        }
        return stacking == Stacking.ADDITIVE ? additive(increaseSum, reductionSum) : factor;
    }

    /** 倍率を掛ける前に足す攻撃力 (Regularity の追撃は最初の一撃と同じ)。 */
    double flat() {
        return forcedMultiplier != null ? forcedFlat : flatDamage;
    }

    /** 加算: max(0, 1 + 増加の合計 - 減少の合計)。 */
    public static double additive(double increases, double reductions) {
        return Math.max(0, 1 + increases - reductions);
    }

    /** 倍率 = Π(1 + 増加) × Π(1 - 減少) (どれも 0 未満にはしない)。 */
    public static double multiplier(double[] increases, double[] reductions) {
        double factor = 1;
        for (double increase : increases) {
            factor *= Math.max(0, 1 + increase);
        }
        for (double reduction : reductions) {
            factor *= Math.max(0, 1 - reduction);
        }
        return factor;
    }
}
