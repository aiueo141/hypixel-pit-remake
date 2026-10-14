package com.pitremake.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 録画 (2026-10-13_23-49-38.mcrc) のアクションバーと同じになるか (相手の最大体力 28)。 */
class HitDisplayTest {

    private static String hearts(int dark, int red, int black) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < dark; i++) {
            text.append("§4❤");
        }
        for (int i = 0; i < red; i++) {
            text.append("§c❤");
        }
        for (int i = 0; i < black; i++) {
            text.append("§0❤");
        }
        return text.toString();
    }

    @Test
    void firstHitFromFullHealth() {
        assertEquals("§7SatouSui " + hearts(8, 6, 0) + "§c 12.000HP", HitDisplay.format("§7SatouSui", 28, 12, 28, true));
    }

    @Test
    void alreadyDamagedTarget() {
        assertEquals("§7SatouSui " + hearts(4, 4, 6) + "§c 8.000HP", HitDisplay.format("§7SatouSui", 16, 8, 28, true));
    }

    @Test
    void halfHeartLeftCountsAsHit() {
        // 16 → 7 (3.5 ハート): 残り 3、削った 5
        assertEquals("§7SatouSui " + hearts(3, 5, 6) + "§c 9.000HP", HitDisplay.format("§7SatouSui", 16, 9, 28, true));
    }

    @Test
    void killingHitShowsOnlyWhatWasLeft() {
        assertEquals("§7SatouSui " + hearts(0, 1, 13) + "§c 7.500HP", HitDisplay.format("§7SatouSui", 2, 7.5, 28, true));
    }

    @Test
    void withoutRawNumbersOnlyHearts() {
        // 録画 Death.mcrc: §d[§b§l120§d] §7w8lver §4❤§4❤§4❤§4❤§c❤§0❤§0❤§0❤§0❤§0❤
        assertEquals("§d[§b§l120§d] §7w8lver " + hearts(4, 1, 5), HitDisplay.format("§d[§b§l120§d] §7w8lver", 10, 2, 20, false));
    }

    @Test
    void normalPlayerHasTenHearts() {
        assertEquals("§7Bot1 " + hearts(9, 1, 0) + "§c 1.250HP", HitDisplay.format("§7Bot1", 20, 1.25, 20, true));
    }
}
