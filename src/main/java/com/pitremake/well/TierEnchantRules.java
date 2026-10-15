package com.pitremake.well;

import com.pitremake.enchant.EnchantCategory;
import com.pitremake.enchant.EnchantRarity;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.enchant.list.PantsEnchants;
import com.pitremake.item.MysticType;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1 回の強化で付けられるエンチャントと、その選び方のルール。剣・弓・パンツ共通。
 *
 * <pre>
 * Tier I   : RESOURCE / COMMON。エンチャントは最大 2 種類。
 * Tier II  : RESOURCE / COMMON / UNCOMMON / RARE。最大 3 種類。RARE は必ず Lv I (この強化では上げない)。
 * Tier III : RESOURCE / COMMON / UNCOMMON / RARE。最大 3 種類。
 *
 * Rage Pants の Tier I : RAGE 系統のエンチャントだけから 1 種類。
 * Rage Pants の Tier II 以降は通常のパンツと同じ (RAGE 系統は新しく付かない)。
 * </pre>
 *
 * RESOURCE はレアリティではなく系統 ({@link com.pitremake.enchant.EnchantCategory}) なので、
 * 系統 (どのアイテムに付くか) の判定は {@link PitEnchant#canApplyTo} に任せ、ここではレアリティだけを絞り込む。
 */
final class TierEnchantRules {

    private static final TierEnchantRules TIER_1 = new TierEnchantRules(EnumSet.of(EnchantRarity.COMMON), 2, false, false);
    private static final TierEnchantRules TIER_2 = new TierEnchantRules(EnumSet.allOf(EnchantRarity.class), 3, true, false);
    private static final TierEnchantRules TIER_3 = new TierEnchantRules(EnumSet.allOf(EnchantRarity.class), 3, false, false);
    private static final TierEnchantRules RAGE_TIER_1 = new TierEnchantRules(EnumSet.allOf(EnchantRarity.class), 1, false, true);

    private final Set<EnchantRarity> rarities;
    private final int maxEnchants;
    private final boolean lockRares;
    private final boolean ragePool;

    private TierEnchantRules(Set<EnchantRarity> rarities, int maxEnchants, boolean lockRares, boolean ragePool) {
        this.rarities = rarities;
        this.maxEnchants = maxEnchants;
        this.lockRares = lockRares;
        this.ragePool = ragePool;
    }

    static TierEnchantRules of(int targetTier, boolean ragePants) {
        if (ragePants && targetTier == 1) {
            return RAGE_TIER_1;
        }
        switch (targetTier) {
            case 1:
                return TIER_1;
            case 2:
                return TIER_2;
            default:
                return TIER_3;
        }
    }

    /**
     * この強化で新しく付けられるエンチャントか (系統とレアリティ)。
     * RAGE 系統は Rage Pants の Tier I だけ。DARK / SEWER / AQUA 系統はそれぞれ専用の処理で付けるので常に対象外。
     */
    boolean isEligible(PitEnchant enchant, MysticType type) {
        EnchantCategory category = enchant.getCategory();
        boolean poolMatches = category == EnchantCategory.RAGE ? ragePool : !ragePool && !category.isSpecialPantsOnly();
        // Pit MBA などオークション限定のエンチャントは抽選に出ない
        return enchant.canApplyTo(type) && rarities.contains(enchant.getRarity()) && poolMatches
                && !PantsEnchants.AUCTION_ONLY.contains(enchant.getKey());
    }

    /** 強化後のアイテムが持てるエンチャントの種類数の上限。 */
    int getMaxEnchants() {
        return maxEnchants;
    }

    /** この強化で RARE のレベルを上げられないか (RARE は Lv I のまま)。 */
    boolean locksRares() {
        return lockRares;
    }

    /** エンチャントが enchantCount 個のとき、新規追加を選ぶ重み。 */
    double newEnchantWeight(int enchantCount) {
        return WellRules.newEnchantWeight(enchantCount);
    }
}
