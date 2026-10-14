package com.pitremake.gem;

import com.pitremake.enchant.EnchantRarity;
import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticPrefix;
import com.pitremake.well.MysticWellRoller;

import java.util.Map;

/**
 * Totally Legit Gem を使えるアイテム・エンチャントの条件 (pit.wiki/Totally_Legit_Gem)。
 * <pre>
 * アイテム: Tier III / エンチャントレベル合計が 8 未満 / まだ Gem を使っていない /
 *          RARE 以外で Lv III 未満のエンチャントが 1 つ以上ある
 * エンチャント: RARE 以外で Lv III 未満 (見た目だけ RARE のものはルール上 RARE ではないので可)
 * </pre>
 */
public final class GemRules {

    private GemRules() {
    }

    public static boolean isEligible(MysticData data, EnchantRegistry registry) {
        if (data.getTier() < MysticWellRoller.maxTier() || data.isGemmed()) {
            return false;
        }
        int total = 0;
        boolean upgradable = false;
        for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
            total += entry.getValue();
            upgradable |= canUpgrade(registry.get(entry.getKey()), entry.getValue());
        }
        return upgradable && total < MysticPrefix.LEGENDARY_LEVELS;
    }

    public static boolean canUpgrade(PitEnchant enchant, int level) {
        return enchant != null && enchant.getRarity() != EnchantRarity.RARE && level < enchant.getMaxLevel();
    }
}
