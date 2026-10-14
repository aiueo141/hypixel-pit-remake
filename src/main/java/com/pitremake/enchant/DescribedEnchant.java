package com.pitremake.enchant;

import java.util.Arrays;
import java.util.List;

/**
 * レベルごとの説明文を持つエンチャント。最大レベルは説明文の数。
 * 効果は今後このクラスを継承して実装する。
 */
public class DescribedEnchant extends PitEnchant {

    private final List<List<String>> descriptions;
    private final boolean resource;

    public DescribedEnchant(String key, String name, EnchantCategory category, EnchantRarity rarity,
                            List<List<String>> descriptions) {
        this(key, name, category, rarity, descriptions, false);
    }

    /**
     * @param resource 系統が RESOURCE 以外 (例: 剣専用) でもリソース系として扱う場合は true
     */
    public DescribedEnchant(String key, String name, EnchantCategory category, EnchantRarity rarity,
                            List<List<String>> descriptions, boolean resource) {
        super(key, name, category, rarity);
        this.descriptions = descriptions;
        this.resource = resource;
    }

    private boolean rareTag;

    /**
     * 見た目だけ RARE にする。Lore には "RARE!" が付くが、抽選・上限・称号などのルール上は
     * 登録したレアリティ (例: COMMON) として扱う。
     */
    public DescribedEnchant withRareTag() {
        this.rareTag = true;
        return this;
    }

    @Override
    protected String lorePrefix() {
        return rareTag ? EnchantRarity.RARE.getLorePrefix() : super.lorePrefix();
    }

    @Override
    public boolean isResource() {
        return resource || super.isResource();
    }

    @Override
    public List<String> getDescription(int level) {
        int index = Math.min(Math.max(level, 1), descriptions.size()) - 1;
        return descriptions.get(index);
    }

    @Override
    public int getMaxLevel() {
        return descriptions.size();
    }

    /** Lv I, Lv II, Lv III ... の説明文をまとめる。 */
    @SafeVarargs
    public static List<List<String>> levels(List<String>... levels) {
        return Arrays.asList(levels);
    }

    /** 1 レベル分の説明文 (色コード込み)。 */
    public static List<String> lines(String... lines) {
        return Arrays.asList(lines);
    }
}
