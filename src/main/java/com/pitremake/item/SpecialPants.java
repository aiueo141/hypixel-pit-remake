package com.pitremake.item;

/**
 * 通常の 5 色とは強化ルールが違う特殊なパンツ。ExtraAttributes の {@link #getNbtKey()} = 1 で判別する。
 * <pre>
 * RAGE  : Tier I で Rage エンチャント 1 つ。以降は通常のパンツと同じ
 * DARK  : Tier I で Somber、Tier II で Dark エンチャント 1 つ (Tier II まで)
 * SEWER : Tier I で Sewer エンチャント 1 つ (Tier I まで)
 * AQUA  : Tier I で Club Rod と Aqua エンチャント 1 つ (Tier I まで)
 * </pre>
 */
public enum SpecialPants {
    RAGE("RagePants", PantsColor.RAGE),
    DARK("DarkPants", PantsColor.DARK),
    SEWER("SewerPants", PantsColor.SEWER),
    AQUA("AquaPants", PantsColor.AQUA);

    private final String nbtKey;
    private final PantsColor color;

    SpecialPants(String nbtKey, PantsColor color) {
        this.nbtKey = nbtKey;
        this.color = color;
    }

    public String getNbtKey() {
        return nbtKey;
    }

    public PantsColor getColor() {
        return color;
    }
}
