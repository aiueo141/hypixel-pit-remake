package com.pitremake.combat;

import com.pitremake.enchant.PitEnchant;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * エンチャントの数値を説明文から読む (説明文は pitpal から取り込んだもの。手で写すと間違えるので文から読む)。
 * 例: "Deal +12% melee damage" → percent = 12、"deals less than 1.5❤" → hearts = 1.5 (体力 3.0)、"deal 1.33x" → 1.33。
 */
public final class EnchantNumbers {

    private static final Pattern PERCENT = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern HEARTS = Pattern.compile("(\\d+(?:\\.\\d+)?) ?❤");
    private static final Pattern TIMES = Pattern.compile("(\\d+(?:\\.\\d+)?)x");

    private EnchantNumbers() {
    }

    /** 説明文の index 番目 (0 から) の "%" の数値を割合で (12% → 0.12)。無ければ 0。 */
    public static double percent(PitEnchant enchant, int level, int index) {
        return find(PERCENT, enchant, level, index) / 100.0;
    }

    /** 説明文の index 番目の "❤" を体力で (1.5❤ → 3.0)。無ければ 0。 */
    public static double hearts(PitEnchant enchant, int level, int index) {
        return find(HEARTS, enchant, level, index) * 2;
    }

    /** 説明文の "1.33x" のような倍率。無ければ 1。 */
    public static double times(PitEnchant enchant, int level) {
        double value = find(TIMES, enchant, level, 0);
        return value == 0 ? 1 : value;
    }

    static double find(Pattern pattern, PitEnchant enchant, int level, int index) {
        if (enchant == null || level <= 0) {
            return 0;
        }
        String text = ChatColor.stripColor(String.join(" ", enchant.getDescription(level)));
        Matcher matcher = pattern.matcher(text);
        int found = 0;
        while (matcher.find()) {
            if (found++ == index) {
                return Double.parseDouble(matcher.group(1));
            }
        }
        return 0;
    }
}
