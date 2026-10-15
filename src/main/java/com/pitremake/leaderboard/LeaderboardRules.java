package com.pitremake.leaderboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** リーダーボードの数字の書き方と順位 (録画 2026-10-15_10-32-13.mcrc)。 */
public final class LeaderboardRules {

    /** 表示する人数。 */
    public static final int TOP = 10;
    /** "Players who logged in this week" (ミリ秒)。 */
    public static final long WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private LeaderboardRules() {
    }

    /** XP の短い書き方 (録画: 11B XP / 5.1M XP)。10 未満は小数 1 桁、10 以上は整数 (どちらも切り捨て)。 */
    public static String abbreviate(long value) {
        String[] units = {"B", "M", "K"};
        long[] sizes = {1_000_000_000L, 1_000_000L, 1_000L};
        for (int i = 0; i < units.length; i++) {
            if (value >= sizes[i]) {
                double scaled = (double) value / sizes[i];
                if (scaled < 10) {
                    double tenths = Math.floor(scaled * 10) / 10;
                    String text = tenths == Math.floor(tenths) ? String.valueOf((long) tenths) : String.valueOf(tenths);
                    return text + units[i];
                }
                return (long) Math.floor(scaled) + units[i];
            }
        }
        return String.format(Locale.US, "%,d", value);
    }

    /** XP の多い順 (同じなら UUID 順)。 */
    public static List<UUID> rank(Map<UUID, Long> xp) {
        List<UUID> ranked = new ArrayList<>(xp.keySet());
        ranked.sort((a, b) -> {
            int byXp = Long.compare(xp.get(b), xp.get(a));
            return byXp != 0 ? byXp : a.compareTo(b);
        });
        return ranked;
    }

    /**
     * Event Leaderboards の Season (録画: 10 月に Fall 2026)。
     * 3〜5 月 Spring / 6〜8 月 Summer / 9〜11 月 Fall / それ以外 Winter (録画にあるのは Fall だけなので残りは推測)。
     */
    public static String season(LocalDate date) {
        int month = date.getMonthValue();
        String name = month >= 3 && month <= 5 ? "Spring" : month >= 6 && month <= 8 ? "Summer"
                : month >= 9 && month <= 11 ? "Fall" : "Winter";
        return name + " " + date.getYear();
    }
}
