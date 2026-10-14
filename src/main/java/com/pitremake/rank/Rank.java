package com.pitremake.rank;

import org.bukkit.ChatColor;

/**
 * 見た目だけの Hypixel のランク。Non〜MVP++ は誰でも選べて、それより上 (staff など) は管理者だけ。
 * <pre>
 * VIP    §a[VIP]            VIP+  §a[VIP§6+§a]
 * MVP    §b[MVP]            MVP+  §b[MVP§c+§b]   (+ の色は選べる。録画では白: §b[MVP§f+§b])
 * MVP++  §6[MVP§c++§6]      (+ の色と、全体を金 / 水色から選べる)
 * </pre>
 */
public enum Rank {
    NONE("Non", "", ChatColor.GRAY, false),
    VIP("VIP", "§a[VIP]", ChatColor.GREEN, false),
    VIP_PLUS("VIP+", "§a[VIP§6+§a]", ChatColor.GREEN, false),
    MVP("MVP", "§b[MVP]", ChatColor.AQUA, false),
    MVP_PLUS("MVP+", null, ChatColor.AQUA, false),
    MVP_PLUS_PLUS("MVP++", null, ChatColor.GOLD, false),
    YOUTUBE("YOUTUBE", "§c[§fYOUTUBE§c]", ChatColor.RED, true),
    PIG("PIG+++", "§d[PIG§b+++§d]", ChatColor.LIGHT_PURPLE, true),
    INNIT("INNIT", "§d[INNIT]", ChatColor.LIGHT_PURPLE, true),
    MOJANG("MOJANG", "§6[MOJANG]", ChatColor.GOLD, true),
    EVENTS("EVENTS", "§6[EVENTS]", ChatColor.GOLD, true),
    MCP("MCP", "§c[MCP]", ChatColor.RED, true),
    GM("GM", "§2[GM]", ChatColor.DARK_GREEN, true),
    ADMIN("ADMIN", "§c[ADMIN]", ChatColor.RED, true),
    OWNER("OWNER", "§c[OWNER]", ChatColor.RED, true);

    private final String displayName;
    /** 固定の表示 (MVP+ / MVP++ は + の色で変わるので null)。 */
    private final String fixedPrefix;
    private final ChatColor nameColor;
    private final boolean adminOnly;

    Rank(String displayName, String fixedPrefix, ChatColor nameColor, boolean adminOnly) {
        this.displayName = displayName;
        this.fixedPrefix = fixedPrefix;
        this.nameColor = nameColor;
        this.adminOnly = adminOnly;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    /** + の色を選べるランク。 */
    public boolean hasPlusColor() {
        return this == MVP_PLUS || this == MVP_PLUS_PLUS;
    }

    /** 名前の前の表示 (Non は "")。aqua: MVP++ を水色にするか。 */
    public String prefix(ChatColor plus, boolean aqua) {
        if (this == MVP_PLUS) {
            return "§b[MVP" + plus + "+§b]";
        }
        if (this == MVP_PLUS_PLUS) {
            String base = aqua ? "§b" : "§6";
            return base + "[MVP" + plus + "++" + base + "]";
        }
        return fixedPrefix;
    }

    /** 名前の色 (MVP++ は金か水色)。 */
    public String nameColor(boolean aqua) {
        if (this == MVP_PLUS_PLUS && aqua) {
            return ChatColor.AQUA.toString();
        }
        return nameColor.toString();
    }

    public static Rank fromName(String name) {
        for (Rank rank : values()) {
            if (rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return null;
    }
}
