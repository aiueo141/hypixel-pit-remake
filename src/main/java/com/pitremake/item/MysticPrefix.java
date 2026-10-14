package com.pitremake.item;

import com.pitremake.enchant.EnchantCategory;
import com.pitremake.enchant.EnchantRarity;
import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.PitEnchant;

import java.util.Map;

/**
 * 強化済みアイテムの名前の先頭に付く称号。条件を満たすもののうち、宣言順で最初の 1 つだけが付く。
 *
 * <pre>
 * One in a Million : Artifact かつ Unthinkable かつ Legendary
 * Miraculous       : Artifact かつ Unthinkable
 * Overpowered      : Artifact かつ エンチャント 3 種類 かつ レベル合計 6 以上
 * Manic            : Rage Pants でエンチャントレベル合計 8 (Rage Pants では Legendary の代わり)
 * Legendary        : エンチャントレベル合計 8
 * Malding          : Rage Pants で Rage エンチャントが Lv III
 * Unthinkable      : RARE 3 種類 (Rage Pants ではならない)
 * Artifact         : Lives 100/100
 * Extraordinary    : RARE 2 種類
 * Bountiful        : リソース系 3 種類
 * </pre>
 */
public enum MysticPrefix {
    ONE_IN_A_MILLION("One in a Million"),
    MIRACULOUS("Miraculous"),
    OVERPOWERED("Overpowered"),
    MANIC("Manic"),
    LEGENDARY("Legendary"),
    MALDING("Malding"),
    UNTHINKABLE("Unthinkable"),
    ARTIFACT("Artifact"),
    EXTRAORDINARY("Extraordinary"),
    BOUNTIFUL("Bountiful");

    /** Artifact になる MaxLives。 */
    public static final int ARTIFACT_LIVES = 100;
    /** Legendary になるエンチャントレベル合計。 */
    public static final int LEGENDARY_LEVELS = 8;
    /** Overpowered になるエンチャントの種類数とレベル合計 (Artifact のとき)。 */
    public static final int OVERPOWERED_ENCHANTS = 3;
    public static final int OVERPOWERED_LEVELS = 6;
    /** Malding になる Rage エンチャントのレベル。 */
    public static final int MALDING_LEVEL = 3;

    private final String displayName;

    MysticPrefix(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 条件を満たす称号のうち最も優先度が高いもの。無ければ null。 */
    public static MysticPrefix of(MysticData data, EnchantRegistry registry) {
        if (data.isDarkPants()) {
            // Dark Pants は称号の代わりに Evil Pants の名前になる
            return null;
        }
        int totalLevels = 0;
        int rares = 0;
        int resources = 0;
        boolean maxedRage = false;
        for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
            totalLevels += entry.getValue();
            PitEnchant enchant = registry.get(entry.getKey());
            if (enchant == null) {
                continue;
            }
            if (enchant.getRarity() == EnchantRarity.RARE) {
                rares++;
            }
            if (enchant.isResource()) {
                resources++;
            }
            if (enchant.getCategory() == EnchantCategory.RAGE && entry.getValue() >= MALDING_LEVEL) {
                maxedRage = true;
            }
        }
        boolean artifact = data.getMaxLives() >= ARTIFACT_LIVES;
        boolean legendary = totalLevels >= LEGENDARY_LEVELS;
        // Rage Pants は Unthinkable (と、それを含む Miraculous / One in a Million) にならない
        boolean unthinkable = rares >= 3 && !data.isRagePants();

        if (artifact && unthinkable && legendary) {
            return ONE_IN_A_MILLION;
        }
        if (artifact && unthinkable) {
            return MIRACULOUS;
        }
        if (artifact && data.getEnchants().size() >= OVERPOWERED_ENCHANTS && totalLevels >= OVERPOWERED_LEVELS) {
            return OVERPOWERED;
        }
        if (legendary) {
            return data.isRagePants() ? MANIC : LEGENDARY;
        }
        if (data.isRagePants() && maxedRage) {
            return MALDING;
        }
        if (unthinkable) {
            return UNTHINKABLE;
        }
        if (artifact) {
            return ARTIFACT;
        }
        if (rares == 2) {
            return EXTRAORDINARY;
        }
        if (resources >= 3) {
            return BOUNTIFUL;
        }
        return null;
    }
}
