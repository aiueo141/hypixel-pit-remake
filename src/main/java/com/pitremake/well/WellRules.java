package com.pitremake.well;

import com.pitremake.enchant.EnchantRarity;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticPrefix;

import java.util.Random;

/**
 * Mystic Well の数値。動画で確認できたのは Tier I = 1,000g, Tier II = 4,000g のみで、
 * それ以外は仮の値。
 */
final class WellRules {

    static final int MAX_TIER = 3;
    /** エンチャントレベルの合計の上限 (8 に達すると Legendary)。 */
    static final int MAX_TOKENS = 8;
    /** RARE のレベル合計の上限。Tier I の時点で RARE III があった特殊なアイテムは MAX_TOKENS まで。 */
    static final int RARE_LEVEL_CAP = 4;

    /** Artifact (Lives 100/100) が出る強化後の Tier と確率。 */
    static final int ARTIFACT_TIER = 3;
    static final double ARTIFACT_CHANCE = 0.01;
    static final int ARTIFACT_LIVES = MysticPrefix.ARTIFACT_LIVES;

    /** Rage Pants を Tier I に強化したときの Lives (どれかが均等に選ばれる)。 */
    static final int[] RAGE_TIER_ONE_LIVES = {4, 9};

    /** Dark Pants は Tier II まで。 */
    static final int DARK_MAX_TIER = 2;
    /** index = 強化後の Tier。Dark Pants の強化費用。 */
    private static final int[] DARK_COST = {0, 10_000, 60_000};
    /** Dark Pants の Tier I の Lives (最小〜最大の 5 の倍数, 均等)。Dark Pants の Lives は常に 5 の倍数。 */
    static final int DARK_TIER_ONE_LIVES_MIN = 5;
    static final int DARK_TIER_ONE_LIVES_MAX = 20;
    /**
     * Dark Pants の Tier II の Lives (5 の倍数)。通常は 10〜25 で強化前より多い値。
     * 低確率 (DARK_EVIL_CHANCE) で 100〜120 になり、Evil Pants になる。Heartripper が付いたときは増えない。
     */
    static final int DARK_TIER_TWO_LIVES_MIN = 10;
    static final int DARK_TIER_TWO_LIVES_MAX = 25;
    static final int DARK_EVIL_LIVES_MIN = 100;
    static final int DARK_EVIL_LIVES_MAX = 120;
    static final double DARK_EVIL_CHANCE = 0.01;
    static final int DARK_LIVES_STEP = 5;

    /** Sewer Pants / Aqua Pants は Tier I まで。 */
    static final int SEWER_AQUA_MAX_TIER = 1;
    static final int SEWER_COST = 250;
    static final int AQUA_COST = 5_000;
    /** Sewer Pants の Lives (どれかが均等に選ばれる)。Trash Panda が付いたときは別。 */
    static final int[] SEWER_LIVES = {10, 20};
    static final int[] SEWER_TRASH_PANDA_LIVES = {30, 40};
    static final int AQUA_LIVES = 5;
    /** 1 回の強化で新しく付けられる RARE の種類数。 */
    static final int MAX_NEW_RARES_PER_UPGRADE = 1;

    /*
     * 追加 or 強化の重み。レベルを 1 つ得るたびに
     *   新規追加の重み = NEW_ENCHANT_WEIGHT × NEW_ENCHANT_DECAY ^ (今のエンチャント数)
     *   既存強化の重み = UPGRADE_WEIGHT (強化できるエンチャントがあるときのみ)
     * の比で抽選する。エンチャントが増えるほど追加が出にくくなる (種類数の上限は TierEnchantRules)。
     *   1 個: 追加 48% / 2 個: 25% (強化候補がある場合)
     * NEW_ENCHANT_DECAY を上げると最終的なエンチャント数が増え、下げると減る。
     * UPGRADE_WEIGHT は 1.0 → 1.5 (レベルが上がらなすぎたので、今あるエンチャントを上げやすくした)。
     */
    static final double UPGRADE_WEIGHT = 1.5;
    static final double NEW_ENCHANT_WEIGHT = 4.0;
    static final double NEW_ENCHANT_DECAY = 0.35;

    /** index = 強化後の Tier。その Tier の MaxLives (最小〜最大)。 */
    private static final int[] LIVES_MIN = {0, 3, 6, 9};
    private static final int[] LIVES_MAX = {0, 5, 20, 50};
    /** index = 強化後の Tier。最も出やすい Lives の位置 (範囲内の割合, 0.5 = 中央)。 */
    private static final double[] LIVES_PEAK = {0, 0.5, 0.2, 0.2};
    /** index = 強化後の Tier。ピーク〜最大値の距離を標準偏差何個分にするか (大きいほど高い値が出にくい)。 */
    private static final double[] LIVES_HIGH_RARITY = {0, 2, 3.5, 3.5};

    /**
     * index = 強化後の Tier。強化の最初に決めるレベル数の重み。要素 i がレベル数 (i + 1) の重み。
     * このレベル数を「新規追加」と「既存強化」に 1 ずつ使っていく。
     *   Tier I  : 1 = 80%, 2 = 20%
     *   Tier II : 1 = 67%, 2 = 33%
     *   Tier III: 1 = 31%, 2 = 31%, 3 = 23%, 4 = 15%
     * (前は I 75/25・II 50/50・III 各 25% で高い値が出すぎ、その後の II 75/25・III 8/4/2/1 では上がらなすぎた。
     *  今の値で Fresh → T3 の平均レベル合計 4.8、Lv III がある 66%、Legendary (合計 8) 約 1/100 (シミュレーション 100 万本))
     */
    private static final int[][] BUDGET_WEIGHTS = {{}, {4, 1}, {2, 1}, {4, 4, 3, 2}};

    /** index = 強化後の Tier。 */
    private static final int[] COST = {0, 1_000, 4_000, 8_000};

    private WellRules() {
    }

    static int cost(int targetTier) {
        return COST[targetTier];
    }

    /** そのアイテムの強化費用 (特殊なパンツは別料金)。 */
    static int cost(int targetTier, MysticData data) {
        if (data.isDarkPants()) {
            return DARK_COST[targetTier];
        }
        if (data.isSewerPants()) {
            return SEWER_COST;
        }
        if (data.isAquaPants()) {
            return AQUA_COST;
        }
        return COST[targetTier];
    }

    /**
     * Renown の Mysticism で決まる強化できる最大の Tier (pit.wiki の Renown)。
     * <pre>
     * Mysticism なし : 使えない (0)
     * I〜III        : Tier II まで
     * IV〜           : Tier III まで
     * 普通の Fresh Pants (Rage / Dark / Sewer / Aqua 以外) は IX から
     * </pre>
     */
    static int mysticismMaxTier(int mysticism, MysticData data, boolean pants) {
        if (mysticism <= 0) {
            return 0;
        }
        if (data.isDarkPants()) {
            // Dark Pants: Tier I は Mysticism I から、Tier II は Mysticism IX (と Heresy III) から (pit.wiki の Dark Pants)
            return mysticism >= MYSTICISM_PANTS_ENCHANT ? DARK_MAX_TIER : 1;
        }
        boolean freshPants = pants && !data.isRagePants() && !data.isDarkPants() && !data.isSewerPants() && !data.isAquaPants();
        if (freshPants && mysticism < MYSTICISM_PANTS_ENCHANT) {
            return 0;
        }
        return mysticism < MYSTICISM_TIER_THREE ? 2 : MAX_TIER;
    }

    static final int MYSTICISM_TIER_THREE = 4;
    static final int MYSTICISM_PANTS_ENCHANT = 9;

    /** そのアイテムが強化できる最大の Tier。 */
    static int maxTier(MysticData data) {
        if (data.isDarkPants()) {
            return DARK_MAX_TIER;
        }
        if (data.isSewerPants() || data.isAquaPants()) {
            return SEWER_AQUA_MAX_TIER;
        }
        return MAX_TIER;
    }

    /** Dark Pants の Tier II の Lives (5 の倍数)。強化前の Lives より必ず多い。 */
    static int rollDarkTierTwoLives(Random random, int previous) {
        if (random.nextDouble() < DARK_EVIL_CHANCE) {
            return multipleOfStep(random, DARK_EVIL_LIVES_MIN, DARK_EVIL_LIVES_MAX);
        }
        int min = Math.max(DARK_TIER_TWO_LIVES_MIN, previous + DARK_LIVES_STEP);
        return multipleOfStep(random, Math.min(min, DARK_TIER_TWO_LIVES_MAX), DARK_TIER_TWO_LIVES_MAX);
    }

    /** min〜max の 5 の倍数から均等に選ぶ。 */
    static int multipleOfStep(Random random, int min, int max) {
        int steps = (max - min) / DARK_LIVES_STEP;
        return min + random.nextInt(steps + 1) * DARK_LIVES_STEP;
    }

    static int minLives(int targetTier) {
        return LIVES_MIN[targetTier];
    }

    static int maxLives(int targetTier) {
        return LIVES_MAX[targetTier];
    }

    static double livesPeak(int targetTier) {
        return LIVES_PEAK[targetTier];
    }

    static double livesHighRarity(int targetTier) {
        return LIVES_HIGH_RARITY[targetTier];
    }

    /** その強化で使えるレベル数の最大値。 */
    static int maxBudget(int targetTier) {
        return BUDGET_WEIGHTS[targetTier].length;
    }

    /** その強化で使えるレベル数を重みに従って決める。 */
    static int rollBudget(Random random, int targetTier) {
        int[] weights = BUDGET_WEIGHTS[targetTier];
        int total = 0;
        for (int weight : weights) {
            total += weight;
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return i + 1;
            }
        }
        return weights.length;
    }

    static double newEnchantWeight(int enchantCount) {
        return NEW_ENCHANT_WEIGHT * Math.pow(NEW_ENCHANT_DECAY, enchantCount);
    }

    /** 新しいエンチャントを付けるときのレアリティの重み。 */
    static int rarityWeight(EnchantRarity rarity) {
        switch (rarity) {
            case COMMON:
                return 40;
            case UNCOMMON:
                // レアが出すぎたので UNCOMMON を上げて RARE を下げた (40 / 40 / 20 → 40 / 50 / 10)
                return 50;
            default:
                return 10;
        }
    }
}
