package com.pitremake.enchant;

import com.pitremake.item.MysticType;

/**
 * エンチャントの系統。RESOURCE は剣・弓・パンツすべてに付けられる。
 * RAGE は Rage Pants を Tier I に強化したときだけ付く (通常の抽選には出ない)。
 * DARK / SEWER / AQUA はそれぞれの特殊なパンツにだけ付く (通常の抽選には出ない)。
 */
public enum EnchantCategory {
    RESOURCE(null, false),
    SWORD(MysticType.SWORD, false),
    BOW(MysticType.BOW, false),
    PANTS(MysticType.PANTS, false),
    RAGE(MysticType.PANTS, false),
    DARK(MysticType.PANTS, true),
    SEWER(MysticType.PANTS, true),
    AQUA(MysticType.PANTS, true);

    private final MysticType target;
    private final boolean specialPantsOnly;

    EnchantCategory(MysticType target, boolean specialPantsOnly) {
        this.target = target;
        this.specialPantsOnly = specialPantsOnly;
    }

    public boolean appliesTo(MysticType type) {
        return target == null || target == type;
    }

    /** 特殊なパンツ専用の処理でだけ付く系統か (通常の Mystic Well の抽選には出ない)。 */
    public boolean isSpecialPantsOnly() {
        return specialPantsOnly;
    }
}
