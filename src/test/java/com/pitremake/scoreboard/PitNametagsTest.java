package com.pitremake.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 録画 (2026-10-14_00-39-15.mcrc) の Hypixel のチーム名・prefix・suffix と同じになるか。 */
class PitNametagsTest {

    @Test
    void sortKeyMatchesHypixelTeamNames() {
        // 録画のチーム名の先頭 2 文字とレベル
        assertEquals("aa", PitNametags.sortKey(120));
        assertEquals("ai", PitNametags.sortKey(112));
        assertEquals("al", PitNametags.sortKey(109));
        assertEquals("ba", PitNametags.sortKey(108));
        assertEquals("bb", PitNametags.sortKey(107));
        assertEquals("cl", PitNametags.sortKey(85));
        assertEquals("el", PitNametags.sortKey(61));
        assertEquals("fa", PitNametags.sortKey(60));
        assertEquals("ib", PitNametags.sortKey(23));
        assertEquals("jj", PitNametags.sortKey(3));
        assertEquals("jl", PitNametags.sortKey(1));
    }

    @Test
    void higherLevelsSortFirst() {
        for (int level = 2; level <= 120; level++) {
            assertTrue(PitNametags.sortKey(level).compareTo(PitNametags.sortKey(level - 1)) < 0, "level " + level);
        }
    }

    @Test
    void prefixAndSuffixFitIn16Characters() {
        assertEquals("§7[§b§l120§7]§7 ", PitNametags.prefix(120));
        assertEquals("§7[§73§7]§7 ", PitNametags.prefix(3));
        assertTrue(PitNametags.prefix(120).length() <= 16);
        assertEquals(" ", PitNametags.suffix(0));
        assertEquals(" §6§l1800g", PitNametags.suffix(1800));
        assertTrue(PitNametags.suffix(5000).length() <= 16);
    }
}
