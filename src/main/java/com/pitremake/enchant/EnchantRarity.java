package com.pitremake.enchant;

import org.bukkit.ChatColor;

public enum EnchantRarity {
    COMMON(""),
    UNCOMMON(""),
    RARE(ChatColor.LIGHT_PURPLE + "RARE! ");

    private final String lorePrefix;

    EnchantRarity(String lorePrefix) {
        this.lorePrefix = lorePrefix;
    }

    /** Lore のエンチャント名の前に付く文字列 (レアのみ "RARE! ")。 */
    public String getLorePrefix() {
        return lorePrefix;
    }
}
