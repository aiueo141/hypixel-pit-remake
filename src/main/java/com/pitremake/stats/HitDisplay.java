package com.pitremake.stats;

import java.util.Locale;

/**
 * プレイヤーを殴ったときのアクションバー (録画 Assist Kill Bounty Sound logs.mcrc / Death.mcrc と同じ形式)。
 * <pre>
 * §6[§4§l86§6] §7PeeNut_ §4❤§4❤…§c❤§c❤…§0❤§0❤…
 *   §4❤ = 殴った後に残っている体力
 *   §c❤ = 今回削った体力
 *   §0❤ = 殴る前から減っていた体力
 * Renown の Raw Numbers があると最後に与えたダメージ (§c 12.000HP、録画 2026-10-13_23-49-38.mcrc) も出る。
 * </pre>
 * ハートの数は最大体力の半分 (端数切り上げ)。残りのハートは切り捨てなので、半端に減ったハートは赤になる。
 */
public final class HitDisplay {

    private static final String HEART = "❤";

    private HitDisplay() {
    }

    /** label は色付きの "レベル 名前" (例: §6[§4§l86§6] §7PeeNut_)。 */
    public static String format(String label, double healthBefore, double damage, double maxHealth, boolean rawNumbers) {
        double before = Math.max(0, Math.min(healthBefore, maxHealth));
        double after = Math.max(0, before - damage);
        int total = hearts(maxHealth);
        // 残りは切り捨てにして、半ハートだけ削ったときも赤いハートが 1 つは出るようにする
        int remaining = (int) Math.floor(after / 2 + 1e-9);
        int lost = hearts(before) - remaining;
        int missing = total - remaining - lost;

        StringBuilder text = new StringBuilder(label).append(' ');
        repeat(text, "§4", remaining);
        repeat(text, "§c", lost);
        repeat(text, "§0", missing);
        if (rawNumbers) {
            text.append("§c ").append(String.format(Locale.US, "%.3f", damage)).append("HP");
        }
        return text.toString();
    }

    private static int hearts(double health) {
        return (int) Math.ceil(health / 2 - 1e-9);
    }

    private static void repeat(StringBuilder text, String color, int count) {
        for (int i = 0; i < count; i++) {
            text.append(color).append(HEART);
        }
    }
}
