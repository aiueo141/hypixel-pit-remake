package com.pitremake.scoreboard;

import org.bukkit.ChatColor;

/**
 * 1.8 のスコアボードはチームの prefix / suffix が 16 文字までなので、1 行を 2 つに分ける。
 * suffix の頭には prefix の最後の色を付け直す (例: "Gold: §6912,364," + "§6354g")。
 */
final class SidebarLine {

    static final int MAX_PART = 16;

    final String prefix;
    final String suffix;

    private SidebarLine(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    static SidebarLine split(String text) {
        if (text.length() <= MAX_PART) {
            return new SidebarLine(text, "");
        }
        int cut = MAX_PART;
        // 色コード (§x) の途中で切らない
        if (text.charAt(cut - 1) == ChatColor.COLOR_CHAR) {
            cut--;
        }
        String prefix = text.substring(0, cut);
        String suffix = ChatColor.getLastColors(prefix) + text.substring(cut);
        if (suffix.length() > MAX_PART) {
            suffix = suffix.substring(0, MAX_PART);
        }
        if (suffix.endsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
            suffix = suffix.substring(0, suffix.length() - 1);
        }
        return new SidebarLine(prefix, suffix);
    }
}
