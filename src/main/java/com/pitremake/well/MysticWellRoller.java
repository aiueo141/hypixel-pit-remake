package com.pitremake.well;

import com.pitremake.enchant.EnchantCategory;
import com.pitremake.enchant.EnchantRarity;
import com.pitremake.enchant.list.AquaEnchants;
import com.pitremake.enchant.list.DarkEnchants;
import com.pitremake.enchant.list.SewerEnchants;
import com.pitremake.item.SpecialPants;
import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Mystic Well で 1 Tier 強化したときのエンチャント抽選。 */
public final class MysticWellRoller {

    public static final String HIDDEN_JEWEL = "hidden_jewel";

    private final EnchantRegistry registry;

    public MysticWellRoller(EnchantRegistry registry) {
        this.registry = registry;
    }

    /** data を 1 Tier 強化する。呼び出し側で Tier が上限未満であることを確認すること。 */
    public void upgrade(MysticData data, MysticType type) {
        upgrade(data, type, ThreadLocalRandom.current());
    }

    /** 乱数を指定して強化する (テスト用)。 */
    void upgrade(MysticData data, MysticType type, Random random) {
        if (data.getSpecialPants() != null && data.getSpecialPants() != SpecialPants.RAGE) {
            upgradeSpecialPants(data, random);
            return;
        }
        int tier = data.getTier() + 1;
        applyTierLives(data, tier, random);

        TierEnchantRules rules = TierEnchantRules.of(tier, data.isRagePants());
        // 最初にこの強化で使えるレベル数を決め、それを使い切るまで追加・強化を繰り返す
        Roll roll = new Roll(data.getEnchants().keySet(), rareLevelCap(data), WellRules.rollBudget(random, tier));
        while (roll.budget > 0 && totalTokens(data) < WellRules.MAX_TOKENS) {
            if (!applyLevel(data, type, rules, roll, random)) {
                // 追加も強化もできなければ、残りのレベルは捨てて終了する
                break;
            }
            roll.budget--;
            if (roll.newRare != null && data.getEnchantLevel(roll.newRare) >= registry.get(roll.newRare).getMaxLevel()) {
                // 新しい RARE がいきなり最大レベルになったら、残りのレベルは捨てて終了する (他のエンチャントは変わらない)
                break;
            }
        }
        if (data.getEnchants().containsKey(HIDDEN_JEWEL)) {
            // Hidden Jewel の付いたアイテムも普通に強化され (エンチャントが増える)、Hidden Jewel のレベルは Tier と同じになる
            data.setEnchant(HIDDEN_JEWEL, Math.min(tier, 3));
        }
        data.setTier(tier);
    }

    /**
     * Hidden Jewel の 117 キル (pit.wiki: "recycle into Tier I pants with a Tier III enchant"):
     * 空の data (Fresh) に、そのアイテムの Tier III で付けられるエンチャント (Resource 以外の RARE / UNCOMMON / COMMON をレアリティの重みで)
     * から 1 つを Lv III で付けて Tier I にする。付けたエンチャントの key を返す。
     */
    public String jewelRecycle(MysticData data, MysticType type) {
        Random random = ThreadLocalRandom.current();
        List<PitEnchant> candidates = new ArrayList<>();
        for (PitEnchant enchant : newEnchantCandidates(data, type, TierEnchantRules.of(3, false), true)) {
            // Resource (Gold Boost など) からは選ばない (ユーザーの指定)
            if (enchant.getMaxLevel() >= 3 && !enchant.getKey().equals(HIDDEN_JEWEL) && enchant.getCategory() != EnchantCategory.RESOURCE) {
                candidates.add(enchant);
            }
        }
        PitEnchant added = addNewEnchant(data, candidates, random);
        data.setEnchant(added.getKey(), 3);
        raiseTier(data, random);
        return added.getKey();
    }

    /**
     * エンチャントの抽選をせずに Tier だけを 1 上げる (Lives は通常の強化と同じルールで決める)。
     * 管理者コマンドでエンチャントを直接付けたときに使う。呼び出し側で Tier が上限未満であることを確認すること。
     */
    public void raiseTier(MysticData data) {
        raiseTier(data, ThreadLocalRandom.current());
    }

    void raiseTier(MysticData data, Random random) {
        int tier = data.getTier() + 1;
        applyTierLives(data, tier, random);
        data.setTier(tier);
    }

    /**
     * 専用ルールの特殊なパンツの強化 (pit.wiki の各ページ)。
     * <pre>
     * Dark  : Tier I は必ず Somber。Tier II は Somber 以外の Dark エンチャントを 1 つ
     * Sewer : Tier I で Sewer エンチャントを 1 つ
     * Aqua  : Tier I で Club Rod と、それ以外の Aqua エンチャントを 1 つ
     * </pre>
     * ランダムに選ぶときは RARE が出にくい (通常の抽選と同じレアリティの重み)。
     */
    private void upgradeSpecialPants(MysticData data, Random random) {
        int tier = data.getTier() + 1;
        switch (data.getSpecialPants()) {
            case DARK:
                if (tier == 1) {
                    data.setEnchant(DarkEnchants.SOMBER, 1);
                } else {
                    addRandomSpecialEnchant(data, EnchantCategory.DARK, DarkEnchants.SOMBER, random);
                }
                break;
            case SEWER:
                addRandomSpecialEnchant(data, EnchantCategory.SEWER, null, random);
                break;
            case AQUA:
                data.setEnchant(AquaEnchants.CLUB_ROD, 1);
                addRandomSpecialEnchant(data, EnchantCategory.AQUA, AquaEnchants.CLUB_ROD, random);
                break;
            default:
                break;
        }
        applyTierLives(data, tier, random);
        data.setTier(tier);
    }

    /** その系統のエンチャント (excludedKey と付いているものを除く) からランダムに 1 つ Lv I で付ける。 */
    private void addRandomSpecialEnchant(MysticData data, EnchantCategory category, String excludedKey, Random random) {
        List<PitEnchant> candidates = new ArrayList<>();
        for (PitEnchant enchant : registry.getAll()) {
            if (enchant.getCategory() == category && !enchant.getKey().equals(excludedKey)
                    && !data.getEnchants().containsKey(enchant.getKey())) {
                candidates.add(enchant);
            }
        }
        if (!candidates.isEmpty()) {
            addNewEnchant(data, candidates, random);
        }
    }

    /** 強化後の Tier に合わせて Lives を決める。エンチャントを付けた後に呼ぶこと (Heartripper の判定のため)。 */
    private static void applyTierLives(MysticData data, int tier, Random random) {
        int lives;
        if (data.isDarkPants()) {
            if (tier == 1) {
                // Dark Pants の Tier I は Lives 5 / 10 / 15 / 20 (5 の倍数)
                lives = WellRules.multipleOfStep(random, WellRules.DARK_TIER_ONE_LIVES_MIN, WellRules.DARK_TIER_ONE_LIVES_MAX);
            } else if (data.getEnchants().containsKey(DarkEnchants.HEARTRIPPER)) {
                // Heartripper が付いた強化では Lives が増えない
                lives = data.getMaxLives();
            } else {
                // Tier II は 10〜25、低確率で 100〜120 (Evil Pants)
                lives = WellRules.rollDarkTierTwoLives(random, data.getMaxLives());
            }
        } else if (data.isSewerPants()) {
            // Sewer Pants は Lives 10 か 20 (Trash Panda が付いたら 30 か 40)
            int[] choices = data.getEnchants().containsKey(SewerEnchants.TRASH_PANDA)
                    ? WellRules.SEWER_TRASH_PANDA_LIVES : WellRules.SEWER_LIVES;
            lives = choices[random.nextInt(choices.length)];
        } else if (data.isAquaPants()) {
            // Aqua Pants は Lives 5
            lives = WellRules.AQUA_LIVES;
        } else if (data.isRagePants() && tier == 1) {
            // Rage Pants の Tier I は Lives 4 か 9
            int[] choices = WellRules.RAGE_TIER_ONE_LIVES;
            lives = choices[random.nextInt(choices.length)];
        } else if (tier == WellRules.ARTIFACT_TIER && random.nextDouble() < WellRules.ARTIFACT_CHANCE) {
            // Tier III への強化では低確率で Artifact (100/100)
            lives = WellRules.ARTIFACT_LIVES;
        } else {
            lives = rollLives(random, tier, data.getMaxLives());
        }
        // 減っていた Lives は割合のまま新しい MaxLives に合わせる (ユーザーの指定: 2/4 → 次が 14 なら 7/14)。Fresh は満タン
        int previousMax = data.getMaxLives();
        int current = data.getLives();
        int newLives = previousMax > 0 && current < previousMax
                ? Math.max(1, (int) Math.round((double) current / previousMax * lives)) : lives;
        data.setMaxLives(lives);
        data.setLives(Math.min(lives, newLives));
    }

    /** 強化できる最大の Tier (通常のアイテム)。 */
    public static int maxTier() {
        return WellRules.MAX_TIER;
    }

    /** そのアイテムが強化できる最大の Tier (Dark Pants は II)。 */
    public static int maxTier(MysticData data) {
        return WellRules.maxTier(data);
    }

    /**
     * RARE のレベル合計の上限。通常は {@link WellRules#RARE_LEVEL_CAP}。
     * 強化前から最大レベルの RARE が付いている (= Tier I の時点で RARE III があった特殊なアイテム) 場合は
     * 上限を撤廃し、全体の上限 {@link WellRules#MAX_TOKENS} までとする。
     * 通常の強化では RARE は Tier II で Lv I 固定、Tier III で初めて上がるため、強化前に RARE III は存在しない。
     */
    private int rareLevelCap(MysticData data) {
        for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
            PitEnchant enchant = registry.get(entry.getKey());
            if (enchant != null && enchant.getRarity() == EnchantRarity.RARE && entry.getValue() >= enchant.getMaxLevel()) {
                return WellRules.MAX_TOKENS;
            }
        }
        return WellRules.RARE_LEVEL_CAP;
    }

    /** 1 回の強化の途中状態。 */
    private static final class Roll {
        final Set<String> enchantsBefore;
        final int rareLevelCap;
        int budget;
        /** この強化で新しく付いた RARE の key (無ければ null)。 */
        String newRare;
        /** この強化で newRare 以外のエンチャントが追加・強化されたか。 */
        boolean othersChanged;

        Roll(Set<String> enchantsBefore, int rareLevelCap, int budget) {
            this.enchantsBefore = new HashSet<>(enchantsBefore);
            this.rareLevelCap = rareLevelCap;
            this.budget = budget;
        }
    }

    /**
     * その Tier の範囲から、強化前の MaxLives より大きい値を選ぶ。
     * ピークが最も出やすく、両端に近いほど出にくい (ピークの左右で幅の違う正規分布)。
     * 最小値はピークから 2σ、最大値はピークから {@link WellRules#livesHighRarity} σ の位置。
     */
    static int rollLives(Random random, int tier, int previous) {
        int min = WellRules.minLives(tier);
        int max = WellRules.maxLives(tier);
        int from = Math.max(min, previous + 1);
        if (from > max) {
            // これ以上上げられない場合は据え置き
            return Math.max(previous, max);
        }

        double peak = min + (max - min) * WellRules.livesPeak(tier);
        double lowSigma = Math.max((peak - min) / 2.0, 0.5);
        double highSigma = Math.max((max - peak) / WellRules.livesHighRarity(tier), 0.5);
        double[] weights = new double[max - from + 1];
        double total = 0;
        for (int i = 0; i < weights.length; i++) {
            int lives = from + i;
            double z = (lives - peak) / (lives < peak ? lowSigma : highSigma);
            weights[i] = Math.exp(-0.5 * z * z);
            total += weights[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return from + i;
            }
        }
        return max;
    }

    /**
     * 残りレベル数から 1 レベルを使い、「新規追加」か「既存強化」を 1 回行う。
     * 片方に候補が無ければもう片方を選び、どちらも無ければ何もせず false を返す。
     */
    private boolean applyLevel(MysticData data, MysticType type, TierEnchantRules rules, Roll roll, Random random) {
        // RARE は 1 レベルでも上がる (付く) と合計が増えるので、上限未満のときだけ候補にする
        boolean rareRoom = rareLevelSum(data) < roll.rareLevelCap;
        boolean allowNewRare = rareRoom
                && countNewRares(data, roll.enchantsBefore) < WellRules.MAX_NEW_RARES_PER_UPGRADE;
        List<PitEnchant> newCandidates = data.getEnchants().size() < rules.getMaxEnchants()
                ? newEnchantCandidates(data, type, rules, allowNewRare) : Collections.<PitEnchant>emptyList();
        List<PitEnchant> upgradeCandidates = upgradeCandidates(data, rules, rareRoom);
        if (roll.newRare != null && roll.othersChanged) {
            // 新しい RARE がいきなり最大レベルになるのは、他のエンチャントが何も変わらなかった強化だけ (real Pit:
            // Sniper I, MLB I → Sniper I, MLB I, Telebow III)。他が変わっていたら最大レベルの 1 つ手前で止める
            PitEnchant newRare = registry.get(roll.newRare);
            if (data.getEnchantLevel(roll.newRare) >= newRare.getMaxLevel() - 1) {
                upgradeCandidates.remove(newRare);
            }
        }
        if (newCandidates.isEmpty() && upgradeCandidates.isEmpty()) {
            return false;
        }

        boolean addNew;
        if (upgradeCandidates.isEmpty()) {
            addNew = true;
        } else if (newCandidates.isEmpty()) {
            addNew = false;
        } else {
            double addWeight = rules.newEnchantWeight(data.getEnchants().size());
            addNew = random.nextDouble() * (addWeight + WellRules.UPGRADE_WEIGHT) < addWeight;
        }

        PitEnchant changed = addNew
                ? addNewEnchant(data, newCandidates, random)
                : upgradeExistingEnchant(data, upgradeCandidates, random);
        if (addNew && changed.getRarity() == EnchantRarity.RARE) {
            roll.newRare = changed.getKey();
        } else if (!changed.getKey().equals(roll.newRare)) {
            roll.othersChanged = true;
        }
        return true;
    }

    /**
     * 新しく付けられるエンチャント: 系統・レアリティが対象で、まだ付いていないもの。
     * allowRare が false (この強化で既に RARE を付けた) なら RARE は除く。
     */
    List<PitEnchant> newEnchantCandidates(MysticData data, MysticType type, TierEnchantRules rules, boolean allowRare) {
        List<PitEnchant> result = new ArrayList<>();
        for (PitEnchant enchant : registry.getAll()) {
            if (!rules.isEligible(enchant, type) || data.getEnchants().containsKey(enchant.getKey())) {
                continue;
            }
            if (!allowRare && enchant.getRarity() == EnchantRarity.RARE) {
                continue;
            }
            result.add(enchant);
        }
        return result;
    }

    /** この強化で新しく付いた RARE の数。 */
    private int countNewRares(MysticData data, Set<String> enchantsBefore) {
        int count = 0;
        for (String key : data.getEnchants().keySet()) {
            PitEnchant enchant = registry.get(key);
            if (!enchantsBefore.contains(key) && enchant != null && enchant.getRarity() == EnchantRarity.RARE) {
                count++;
            }
        }
        return count;
    }

    /**
     * レベルを上げられるエンチャント: 登録済みで最大レベル未満のもの。
     * Tier II (RARE は Lv I のまま) と、RARE のレベル合計が上限に達しているときは RARE を除く。
     */
    List<PitEnchant> upgradeCandidates(MysticData data, TierEnchantRules rules, boolean allowRare) {
        List<PitEnchant> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
            PitEnchant enchant = registry.get(entry.getKey());
            if (enchant == null || entry.getValue() >= enchant.getMaxLevel() || entry.getKey().equals(HIDDEN_JEWEL)) {
                // Hidden Jewel のレベルは抽選では上げない (Tier に合わせて上がる)
                continue;
            }
            if (enchant.getRarity() == EnchantRarity.RARE && (rules.locksRares() || !allowRare)) {
                continue;
            }
            result.add(enchant);
        }
        return result;
    }

    /** 付いている RARE のレベル合計。 */
    private int rareLevelSum(MysticData data) {
        int sum = 0;
        for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
            PitEnchant enchant = registry.get(entry.getKey());
            if (enchant != null && enchant.getRarity() == EnchantRarity.RARE) {
                sum += entry.getValue();
            }
        }
        return sum;
    }

    /** レアリティの重みでレアリティを選び、その中から均等に 1 つ選んで Lv I で付ける。付けたものを返す。 */
    private static PitEnchant addNewEnchant(MysticData data, List<PitEnchant> candidates, Random random) {
        Map<EnchantRarity, List<PitEnchant>> byRarity = new EnumMap<>(EnchantRarity.class);
        for (PitEnchant enchant : candidates) {
            List<PitEnchant> list = byRarity.get(enchant.getRarity());
            if (list == null) {
                list = new ArrayList<>();
                byRarity.put(enchant.getRarity(), list);
            }
            list.add(enchant);
        }

        int total = 0;
        for (EnchantRarity rarity : byRarity.keySet()) {
            total += WellRules.rarityWeight(rarity);
        }
        int roll = random.nextInt(total);
        List<PitEnchant> pool = null;
        for (Map.Entry<EnchantRarity, List<PitEnchant>> entry : byRarity.entrySet()) {
            pool = entry.getValue();
            roll -= WellRules.rarityWeight(entry.getKey());
            if (roll < 0) {
                break;
            }
        }

        PitEnchant enchant = pool.get(random.nextInt(pool.size()));
        data.setEnchant(enchant.getKey(), 1);
        return enchant;
    }

    /** 候補から均等に 1 つ選んでレベルを 1 上げる。上げたものを返す。 */
    private static PitEnchant upgradeExistingEnchant(MysticData data, List<PitEnchant> candidates, Random random) {
        PitEnchant enchant = candidates.get(random.nextInt(candidates.size()));
        data.setEnchant(enchant.getKey(), data.getEnchantLevel(enchant.getKey()) + 1);
        return enchant;
    }

    private static int totalTokens(MysticData data) {
        int total = 0;
        for (int level : data.getEnchants().values()) {
            total += level;
        }
        return total;
    }
}
