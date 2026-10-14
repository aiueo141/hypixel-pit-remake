package com.pitremake.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PitLevelsTest {

    @Test
    void levelOneNeedsFifteenXp() {
        assertEquals(1, PitLevels.levelOf(0));
        assertEquals(1, PitLevels.levelOf(14));
        assertEquals(2, PitLevels.levelOf(15));
        assertEquals(15, PitLevels.neededXp(0));
    }

    /** 録画: Lv1 でバー 0.67 (残り 5) → +18XP → Lv2 でバー 0.87 (残り 2)。 */
    @Test
    void matchesRecordedLevelUp() {
        long before = 10;
        assertEquals(0.67f, PitLevels.progress(before), 0.01f);
        assertEquals(5, PitLevels.neededXp(before));
        long after = before + 18;
        assertEquals(2, PitLevels.levelOf(after));
        assertEquals(2, PitLevels.neededXp(after));
        assertEquals(0.87f, PitLevels.progress(after), 0.01f);
    }

    @Test
    void bracketsChangeEveryTenLevels() {
        assertEquals(15, PitLevels.xpToNext(9));
        assertEquals(30, PitLevels.xpToNext(10));
        assertEquals(1500, PitLevels.xpToNext(119));
        assertEquals(0, PitLevels.xpToNext(120));
        assertEquals(9 * 15, PitLevels.totalXpFor(10));
    }

    @Test
    void maxLevelIsCapped() {
        long max = PitLevels.totalXpFor(PitLevels.MAX_LEVEL);
        assertEquals(120, PitLevels.levelOf(max));
        assertEquals(120, PitLevels.levelOf(max * 10));
        assertEquals(0, PitLevels.neededXp(max));
        assertEquals(1f, PitLevels.progress(max));
    }

    @Test
    void formatMatchesRecording() {
        assertEquals("§7[§71§7]", PitLevels.format(1));
        assertEquals("§7[§b§l120§7]", PitLevels.format(120));
        assertEquals("§7[§e57§7]", PitLevels.format(57));
    }
}
