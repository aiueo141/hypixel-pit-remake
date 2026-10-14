package com.pitremake.scoreboard;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PitSidebarTest {

    @Test
    void linesFollowHypixelLayout() {
        List<String> lines = PitSidebar.lines("10/13/26", "M6C", 0, 16, 23.7, false, 0, 0, "§ewww.hypixel.net");
        assertEquals(Arrays.asList(
                "§710/13/26  §8M6C",
                "",
                "Level: §7[§72§7]",
                "Needed XP: §b14",
                "",
                "Gold: §623.70g",
                "",
                "Status: §aIdling",
                "",
                "§ewww.hypixel.net"), lines);
    }

    @Test
    void streakAppearsWhileFighting() {
        List<String> lines = PitSidebar.lines("10/13/26", "M6C", 0, 0, 912364354, true, 0, 3, "x");
        assertTrue(lines.contains("Status: §cFighting"));
        assertEquals("Streak: §a3", lines.get(lines.indexOf("Status: §cFighting") + 1));
        // 10,000g 以上は小数点以下なし
        assertTrue(lines.contains("Gold: §6912,364,354g"));
    }

    @Test
    void bountyGoesBetweenStatusAndStreak() {
        List<String> lines = PitSidebar.lines("10/13/26", "M6C", 0, 0, 0, true, 1800, 40, "x");
        int status = lines.indexOf("Status: §cFighting");
        // 太字ではなく 1,000g のような形
        assertEquals("Bounty: §61,800g", lines.get(status + 1));
        assertEquals("Streak: §a40", lines.get(status + 2));
        assertTrue(lines.get(status + 1).length() <= 2 * SidebarLine.MAX_PART);
    }

    @Test
    void megastreakStatusFractionalStreakAndStrength() {
        List<String> lines = PitSidebar.lines("10/13/26", "M6C", 0, 0, 0, "§6Highlander", 0, 52.25, 5, 7, "x");
        int status = lines.indexOf("Status: §6Highlander");
        assertEquals("Streak: §a52.25", lines.get(status + 1));
        // Strength はストリークの下、ローマ数字は赤・秒数のかっこは灰色
        assertEquals("Strength: §cV §7(7)", lines.get(status + 2));
    }

    @Test
    void longLinesAreSplitKeepingColor() {
        SidebarLine line = SidebarLine.split("Gold: §6912,364,354.00g");
        assertEquals("Gold: §6912,364,", line.prefix);
        assertEquals("§6354.00g", line.suffix);
    }

    @Test
    void splitNeverCutsColorCode() {
        // 16 文字目が § になる行
        SidebarLine line = SidebarLine.split("Needed XP: 1234§b67");
        assertEquals("Needed XP: 1234", line.prefix);
        assertEquals("§b67", line.suffix);
    }

    @Test
    void shortLinesStayInPrefix() {
        SidebarLine line = SidebarLine.split("Status: §aIdling");
        assertEquals("Status: §aIdling", line.prefix);
        assertEquals("", line.suffix);
    }
}
