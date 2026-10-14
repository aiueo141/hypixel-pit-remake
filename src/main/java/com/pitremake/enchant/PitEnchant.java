package com.pitremake.enchant;

import com.pitremake.item.MysticType;
import com.pitremake.util.RomanNumerals;
import org.bukkit.ChatColor;

import java.util.List;

/**
 * ミスティックエンチャントの定義。効果は今後サブクラスで実装する。
 */
public abstract class PitEnchant {

    private final String key;
    private final String name;
    private final EnchantCategory category;
    private final EnchantRarity rarity;

    /**
     * @param key NBT (ExtraAttributes.CustomEnchants[].Key) に保存される ID。変更しないこと。
     */
    protected PitEnchant(String key, String name, EnchantCategory category, EnchantRarity rarity) {
        this.key = key;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
    }

    /** レベルごとの説明文 (色コード込み)。 */
    public abstract List<String> getDescription(int level);

    public int getMaxLevel() {
        return 3;
    }

    /** 例: "Name", "Name II", "RARE! Name III" */
    public String formatName(int level) {
        String numeral = level > 1 ? " " + RomanNumerals.of(level) : "";
        return lorePrefix() + ChatColor.BLUE + name + numeral;
    }

    /** Lore に "RARE!" が付くか (本物の RARE と、見た目だけ RARE のもの)。 */
    public boolean showsRareTag() {
        return !lorePrefix().isEmpty();
    }

    /** Lore のエンチャント名の前に付く文字列。通常はレアリティで決まる。 */
    protected String lorePrefix() {
        return rarity.getLorePrefix();
    }

    /** ゴールド・XP などが増えるリソース系か (Bountiful の判定に使う)。系統が RESOURCE なら常に true。 */
    public boolean isResource() {
        return category == EnchantCategory.RESOURCE;
    }

    public boolean canApplyTo(MysticType type) {
        return category.appliesTo(type);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public EnchantCategory getCategory() {
        return category;
    }

    public EnchantRarity getRarity() {
        return rarity;
    }
}
